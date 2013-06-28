/*
============================================================================ 
Licensed Materials - Property of IBM

5747-SM3
 
(C) Copyright IBM Corp. 1999, 2012 All Rights Reserved.
 
US Government Users Restricted Rights - Use, duplication or
disclosure restricted by GSA ADP Schedule Contract with
IBM Corp.
============================================================================
 */
package com.ibm.msg.android.service;

import java.io.File;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClientPersistence;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttSecurityException;
import org.eclipse.paho.client.mqttv3.persist.MqttDefaultFilePersistence;
import org.eclipse.paho.client.mqttv3.util.Debug;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.util.Log;

import com.ibm.msg.android.service.MessageStore.StoredMessage;

/**
 * <p>
 * This performs MQTT operations for a specific client {host,port,clientId}
 * </p>
 * <p>
 * Most of the major API here is intended to implement the most general forms of
 * the methods in IMqttAsyncClient, with slight adjustments for the Android
 * environment<br>
 * These adjustments usually consist of adding two parameters to each method :-
 * <ul>
 * <li>invocationContext - a string passed from the application to identify the
 * context of the operation (mainly included for support of the javascript API
 * implementation)</li>
 * <li>activityToken - a string passed from the Activity to relate back to a
 * callback method or other context-specific data</li>
 * </ul>
 * </p>
 * <p>
 * Operations are very much asynchronous, so success and failure are notified by
 * packing the relevant data into Intent objects which are broadcast back to the
 * Activity via the MqttService.callbackToActivity() method.
 * </p>
 */
class MqttServiceClient implements MqttCallback {

  // Strings for Intents etc..
  private static final String TAG = "MqttServiceClient";
  // Error status messages
  private static final String NOT_CONNECTED = "not connected";

  // fields for the connection definition
  private String serverURI;
  private String clientId;
  private MqttClientPersistence persistence = null;
  private MqttConnectOptions connectOptions;

  // Alarm details for the keepalive/ping mechanism
  private String alarmAction;
  private String pingTopic;
  private PingSender pingSender;

  // Client handle, used for callbacks...
  private String clientHandle;

  // our client object - instantiated on connect
  private MqttAsyncClient myClient = null;

  // our (parent) service object
  private MqttService service = null;

  // Saved sent messages and their corresponding Topics, activityTokens and
  // invocationContexts, so we can handle "deliveryComplete" callbacks
  // from the mqttClient
  private Map<IMqttDeliveryToken, String /* Topic */> savedTopics = new HashMap<IMqttDeliveryToken, String>();
  private Map<IMqttDeliveryToken, MqttMessage> savedSentMessages = new HashMap<IMqttDeliveryToken, MqttMessage>();
  private Map<IMqttDeliveryToken, String> savedActivityTokens = new HashMap<IMqttDeliveryToken, String>();
  private Map<IMqttDeliveryToken, String> savedInvocationContexts = new HashMap<IMqttDeliveryToken, String>();

  /**
   * Constructor
   * 
   * @param service
   *            our "parent" service - we make callbacks to it
   * @param serverURI
   *            the URI of the MQTT server to which we will connect
   * @param clientId
   *            the name by which we will identify ourselves to the MQTT
   *            server
   * @param persistence
   * @param clientHandle
   *            the "handle" by which the activity will identify us
   */
  MqttServiceClient(MqttService service, String serverURI, String clientId,
      MqttClientPersistence persistence, String clientHandle) {
    this.serverURI = serverURI.toString();
    this.service = service;
    this.clientId = clientId;
    this.persistence = persistence;
    this.clientHandle = clientHandle;
    alarmAction = MqttServiceConstants.ALARM_INTENT_PREFIX + clientHandle;
    pingTopic = MqttServiceConstants.PING_TOPIC_PREFIX + clientHandle;
    pingSender = new PingSender();
  }

  // The major API implementation follows :-

  /**
   * Connect to the server specified when we were instantiated
   * 
   * @param options
   *            timeout, etc
   * @param invocationContext
   *            arbitrary data to be passed back to the application
   * @param activityToken
   *            arbitrary identifier to be passed back to the Activity
   * @throws MqttSecurityException
   * @throws MqttException
   */
  public void connect(MqttConnectOptions options, String invocationContext,
      String activityToken) {

    connectOptions = options;

    if (connectOptions.isCleanSession()) { // if it's a clean session,
      // discard old data
      service.messageStore.clearArrivedMessages(clientHandle);
    }

    service.traceDebug(TAG, "Connecting {" + serverURI + "} as {"
                            + clientId + "}");
    final Bundle resultBundle = new Bundle();
    resultBundle.putString(MqttServiceConstants.CALLBACK_ACTIVITY_TOKEN,
        activityToken);
    resultBundle.putString(
        MqttServiceConstants.CALLBACK_INVOCATION_CONTEXT,
        invocationContext);
    resultBundle.putString(MqttServiceConstants.CALLBACK_ACTION,
        MqttServiceConstants.CONNECT_ACTION);
    try {
      if (persistence == null) {
        // ask Android where we can put files
        File myDir = service.getExternalFilesDir(TAG);

        if (myDir == null) {
          resultBundle.putString(
              MqttServiceConstants.CALLBACK_ERROR_MESSAGE,
              "No external storage available");
          resultBundle.putInt(
              MqttServiceConstants.CALLBACK_ERROR_NUMBER, -1);
          service.callbackToActivity(clientHandle, Status.ERROR,
              resultBundle);
          return;
        }

        // use that to setup MQTT client persistence storage
        persistence = new MqttDefaultFilePersistence(
            myDir.getAbsolutePath());
      }

      myClient = new MqttAsyncClient(serverURI, clientId, persistence);
      myClient.setCallback(this);

      IMqttActionListener listener = new MqttServiceClientListener(
          resultBundle) {

        @Override
        public void onSuccess(IMqttToken asyncActionToken) {
          // we need to wake up the phone's CPU frequently enough
          // so that the keep alive messages can be sent
          // we schedule the first one of these now
          service.registerReceiver(pingSender, new IntentFilter(
              alarmAction));
          scheduleNextPing();

          service.callbackToActivity(clientHandle, Status.OK,
              resultBundle);
          deliverBacklog();
        }
      };
      myClient.connect(connectOptions, invocationContext, listener);
    }
    catch (Exception e) {
      handleException(resultBundle, e);
    }
  }

  private void handleException(final Bundle resultBundle, Exception e) {
    resultBundle.putString(MqttServiceConstants.CALLBACK_ERROR_MESSAGE,
        e.getLocalizedMessage());
    resultBundle.putInt(
        MqttServiceConstants.CALLBACK_ERROR_NUMBER,
        (e instanceof MqttException) ? ((MqttException) e)
            .getReasonCode() : -1);
    service.callbackToActivity(clientHandle, Status.ERROR, resultBundle);
  }

  /**
   * Attempt to deliver any outstanding messages we've received but which the
   * application hasn't acknowledged. If "cleanSession" was specified, we'll
   * have already purged any such messages from our messageStore.
   */
  private void deliverBacklog() {
    Iterator<StoredMessage> backlog = service.messageStore
        .getAllArrivedMessages(clientHandle);
    while (backlog.hasNext()) {
      StoredMessage msgArrived = backlog.next();
      Bundle resultBundle = messageToBundle(msgArrived.getMessageId(),
          msgArrived.getTopic(), msgArrived.getMessage());
      resultBundle.putString(MqttServiceConstants.CALLBACK_ACTION,
          MqttServiceConstants.MESSAGE_ARRIVED_ACTION);
      service.callbackToActivity(clientHandle, Status.OK, resultBundle);
    }
  }

  /**
   * Create a bundle containing all relevant data pertaining to a message
   * 
   * @param messageId
   *            the message's identifier in the messageStore, so that a
   *            callback can be made to remove it once delivered
   * @param topic
   *            the topic on which the message was delivered
   * @param message
   *            the message itself
   * @return the bundle
   */
  private Bundle messageToBundle(String messageId, String topic,
      MqttMessage message) {
    Bundle result = new Bundle();
    result.putString(MqttServiceConstants.CALLBACK_MESSAGE_ID, messageId);
    result.putString(MqttServiceConstants.CALLBACK_DESTINATION_NAME, topic);
    result.putParcelable(MqttServiceConstants.CALLBACK_MESSAGE_PARCEL,
        new ParcelableMqttMessage(message));
    return result;
  }

  /**
   * Disconnect from the server
   * 
   * @param quiesceTimeout
   *            in milliseconds
   * @param invocationContext
   *            arbitrary data to be passed back to the application
   * @param activityToken
   *            arbitrary string to be passed back to the activity
   */
  void disconnect(long quiesceTimeout, String invocationContext,
      String activityToken) {
    service.traceDebug(TAG, "disconnect()");
    final Bundle resultBundle = new Bundle();
    resultBundle.putString(MqttServiceConstants.CALLBACK_ACTIVITY_TOKEN,
        activityToken);
    resultBundle.putString(
        MqttServiceConstants.CALLBACK_INVOCATION_CONTEXT,
        invocationContext);
    resultBundle.putString(MqttServiceConstants.CALLBACK_ACTION,
        MqttServiceConstants.DISCONNECT_ACTION);
    if ((myClient != null) && (myClient.isConnected())) {
      IMqttActionListener listener = new MqttServiceClientListener(
          resultBundle);
      try {
        myClient.disconnect(quiesceTimeout, invocationContext, listener);
      }
      catch (Exception e) {
        handleException(resultBundle, e);
      }
    }
    else {
      resultBundle.putString(MqttServiceConstants.CALLBACK_ERROR_MESSAGE,
          NOT_CONNECTED);
      service.traceError(MqttServiceConstants.DISCONNECT_ACTION,
          NOT_CONNECTED);
      service.callbackToActivity(clientHandle, Status.ERROR, resultBundle);
    }

    if (connectOptions.isCleanSession()) {
      // assume we'll clear the stored messages at this point
      service.messageStore.clearArrivedMessages(clientHandle);
    }

    // We don't need the ping mechanism now
    service.unregisterReceiver(pingSender);
  }

  /**
   * Disconnect from the server
   * 
   * @param invocationContext
   *            arbitrary data to be passed back to the application
   * @param activityToken
   *            arbitrary string to be passed back to the activity
   */
  void disconnect(String invocationContext, String activityToken) {
    service.traceDebug(TAG, "disconnect()");
    final Bundle resultBundle = new Bundle();
    resultBundle.putString(MqttServiceConstants.CALLBACK_ACTIVITY_TOKEN,
        activityToken);
    resultBundle.putString(
        MqttServiceConstants.CALLBACK_INVOCATION_CONTEXT,
        invocationContext);
    resultBundle.putString(MqttServiceConstants.CALLBACK_ACTION,
        MqttServiceConstants.DISCONNECT_ACTION);
    if ((myClient != null) && (myClient.isConnected())) {
      IMqttActionListener listener = new MqttServiceClientListener(
          resultBundle);
      try {
        myClient.disconnect(invocationContext, listener);
      }
      catch (Exception e) {
        handleException(resultBundle, e);
      }
    }
    else {
      resultBundle.putString(MqttServiceConstants.CALLBACK_ERROR_MESSAGE,
          NOT_CONNECTED);
      service.traceError(MqttServiceConstants.DISCONNECT_ACTION,
          NOT_CONNECTED);
      service.callbackToActivity(clientHandle, Status.ERROR, resultBundle);
    }

    if (connectOptions.isCleanSession()) {
      // assume we'll clear the stored messages at this point
      service.messageStore.clearArrivedMessages(clientHandle);
    }
    // We don't need the ping mechanism now
    service.unregisterReceiver(pingSender);
  }

  /**
   * @return true if we are connected to an MQTT server
   */
  public boolean isConnected() {
    return myClient.isConnected();
  }

  /**
   * Publish a message on a topic
   * 
   * @param topic
   *            the topic on which to publish - represented as a string, not
   *            an MqttTopic object
   * @param payload
   *            the content of the message to publish
   * @param qos
   *            the quality of service requested
   * @param retained
   *            whether the MQTT server should retain this message
   * @param invocationContext
   *            arbitrary data to be passed back to the application
   * @param activityToken
   *            arbitrary string to be passed back to the activity
   * @return token for tracking the operation
   */
  public IMqttDeliveryToken publish(String topic, byte[] payload, int qos,
      boolean retained, String invocationContext, String activityToken) {
    final Bundle resultBundle = new Bundle();
    resultBundle.putString(MqttServiceConstants.CALLBACK_ACTION,
        MqttServiceConstants.SEND_ACTION);
    resultBundle.putString(MqttServiceConstants.CALLBACK_ACTIVITY_TOKEN,
        activityToken);
    resultBundle.putString(
        MqttServiceConstants.CALLBACK_INVOCATION_CONTEXT,
        invocationContext);

    IMqttDeliveryToken sendToken = null;

    if ((myClient != null) && (myClient.isConnected())) {
      IMqttActionListener listener = new MqttServiceClientListener(
          resultBundle);
      try {
        MqttMessage message = new MqttMessage(payload);
        message.setQos(qos);
        message.setRetained(retained);
        sendToken = myClient.publish(topic, payload, qos, retained,
            invocationContext, listener);
        storeSendDetails(topic, message, sendToken, invocationContext,
            activityToken);
      }
      catch (Exception e) {
        handleException(resultBundle, e);
      }
    }
    else {
      resultBundle.putString(MqttServiceConstants.CALLBACK_ERROR_MESSAGE,
          NOT_CONNECTED);
      service.traceError(MqttServiceConstants.SEND_ACTION, NOT_CONNECTED);
      service.callbackToActivity(clientHandle, Status.ERROR, resultBundle);
    }

    return sendToken;
  }

  /**
   * Publish a message on a topic
   * 
   * @param topic
   *            the topic on which to publish - represented as a string, not
   *            an MqttTopic object
   * @param message
   *            the message to publish
   * @param invocationContext
   *            arbitrary data to be passed back to the application
   * @param activityToken
   *            arbitrary string to be passed back to the activity
   * @return token for tracking the operation
   */
  public IMqttDeliveryToken publish(String topic, MqttMessage message,
      String invocationContext, String activityToken) {
    final Bundle resultBundle = new Bundle();
    resultBundle.putString(MqttServiceConstants.CALLBACK_ACTION,
        MqttServiceConstants.SEND_ACTION);
    resultBundle.putString(MqttServiceConstants.CALLBACK_ACTIVITY_TOKEN,
        activityToken);
    resultBundle.putString(
        MqttServiceConstants.CALLBACK_INVOCATION_CONTEXT,
        invocationContext);

    IMqttDeliveryToken sendToken = null;

    if ((myClient != null) && (myClient.isConnected())) {
      IMqttActionListener listener = new MqttServiceClientListener(
          resultBundle);
      try {
        sendToken = myClient.publish(topic, message, invocationContext,
            listener);
        storeSendDetails(topic, message, sendToken, invocationContext,
            activityToken);
      }
      catch (Exception e) {
        handleException(resultBundle, e);
      }
    }
    else {
      resultBundle.putString(MqttServiceConstants.CALLBACK_ERROR_MESSAGE,
          NOT_CONNECTED);
      service.traceError(MqttServiceConstants.SEND_ACTION, NOT_CONNECTED);
      service.callbackToActivity(clientHandle, Status.ERROR, resultBundle);
    }
    return sendToken;
  }

  /**
   * subscribe to a topic
   * 
   * @param topic
   *            a possibly wildcarded topic name
   * @param qos
   *            requested quality of service for the topic
   * @param invocationContext
   *            arbitrary data to be passed back to the application
   * @param activityToken
   *            arbitrary identifier to be passed back to the Activity
   */
  public void subscribe(final String topic, final int qos,
      String invocationContext, String activityToken) {
    service.traceDebug(TAG, "subscribe({" + topic + "}," + qos + ",{"
                            + invocationContext + "}, {" + activityToken + "}");
    final Bundle resultBundle = new Bundle();
    resultBundle.putString(MqttServiceConstants.CALLBACK_ACTION,
        MqttServiceConstants.SUBSCRIBE_ACTION);
    resultBundle.putString(MqttServiceConstants.CALLBACK_ACTIVITY_TOKEN,
        activityToken);
    resultBundle.putString(
        MqttServiceConstants.CALLBACK_INVOCATION_CONTEXT,
        invocationContext);

    if ((myClient != null) && (myClient.isConnected())) {
      IMqttActionListener listener = new MqttServiceClientListener(
          resultBundle);
      try {
        myClient.subscribe(topic, qos, invocationContext, listener);
      }
      catch (Exception e) {
        handleException(resultBundle, e);
      }
    }
    else {
      resultBundle.putString(MqttServiceConstants.CALLBACK_ERROR_MESSAGE,
          NOT_CONNECTED);
      service.traceError("subscribe", NOT_CONNECTED);
      service.callbackToActivity(clientHandle, Status.ERROR, resultBundle);
    }
  }

  /**
   * subscribe to one or more topics
   * 
   * @param topic
   *            a list of possibly wildcarded topic names
   * @param qos
   *            requested quality of service for each topic
   * @param invocationContext
   *            arbitrary data to be passed back to the application
   * @param activityToken
   *            arbitrary identifier to be passed back to the Activity
   */
  public void subscribe(final String[] topic, final int[] qos,
      String invocationContext, String activityToken) {
    service.traceDebug(TAG, "subscribe({" + topic + "}," + qos + ",{"
                            + invocationContext + "}, {" + activityToken + "}");
    final Bundle resultBundle = new Bundle();
    resultBundle.putString(MqttServiceConstants.CALLBACK_ACTION,
        MqttServiceConstants.SUBSCRIBE_ACTION);
    resultBundle.putString(MqttServiceConstants.CALLBACK_ACTIVITY_TOKEN,
        activityToken);
    resultBundle.putString(
        MqttServiceConstants.CALLBACK_INVOCATION_CONTEXT,
        invocationContext);

    if ((myClient != null) && (myClient.isConnected())) {
      IMqttActionListener listener = new MqttServiceClientListener(
          resultBundle);
      try {
        myClient.subscribe(topic, qos, invocationContext, listener);
      }
      catch (Exception e) {
        handleException(resultBundle, e);
      }
    }
    else {
      resultBundle.putString(MqttServiceConstants.CALLBACK_ERROR_MESSAGE,
          NOT_CONNECTED);
      service.traceError("subscribe", NOT_CONNECTED);
      service.callbackToActivity(clientHandle, Status.ERROR, resultBundle);
    }
  }

  /**
   * unsubscribe from a topic
   * 
   * @param topic
   *            a possibly wildcarded topic name
   * @param invocationContext
   *            arbitrary data to be passed back to the application
   * @param activityToken
   *            arbitrary identifier to be passed back to the Activity
   */
  void unsubscribe(final String topic, String invocationContext,
      String activityToken) {
    service.traceDebug(TAG, "unsubscribe({" + topic + "},{"
                            + invocationContext + "}, {" + activityToken + "})");
    final Bundle resultBundle = new Bundle();
    resultBundle.putString(MqttServiceConstants.CALLBACK_ACTION,
        MqttServiceConstants.UNSUBSCRIBE_ACTION);
    resultBundle.putString(MqttServiceConstants.CALLBACK_ACTIVITY_TOKEN,
        activityToken);
    resultBundle.putString(
        MqttServiceConstants.CALLBACK_INVOCATION_CONTEXT,
        invocationContext);
    if ((myClient != null) && (myClient.isConnected())) {
      IMqttActionListener listener = new MqttServiceClientListener(
          resultBundle);
      try {
        myClient.unsubscribe(topic, invocationContext, listener);
      }
      catch (Exception e) {
        handleException(resultBundle, e);
      }
    }
    else {
      resultBundle.putString(MqttServiceConstants.CALLBACK_ERROR_MESSAGE,
          NOT_CONNECTED);

      service.traceError("subscribe", NOT_CONNECTED);
      service.callbackToActivity(clientHandle, Status.ERROR, resultBundle);
    }
  }

  /**
   * unsubscribe from one or more topics
   * 
   * @param topic
   *            a list of possibly wildcarded topic names
   * @param invocationContext
   *            arbitrary data to be passed back to the application
   * @param activityToken
   *            arbitrary identifier to be passed back to the Activity
   */
  void unsubscribe(final String[] topic, String invocationContext,
      String activityToken) {
    service.traceDebug(TAG, "unsubscribe({" + topic + "},{"
                            + invocationContext + "}, {" + activityToken + "})");
    final Bundle resultBundle = new Bundle();
    resultBundle.putString(MqttServiceConstants.CALLBACK_ACTION,
        MqttServiceConstants.UNSUBSCRIBE_ACTION);
    resultBundle.putString(MqttServiceConstants.CALLBACK_ACTIVITY_TOKEN,
        activityToken);
    resultBundle.putString(
        MqttServiceConstants.CALLBACK_INVOCATION_CONTEXT,
        invocationContext);
    if ((myClient != null) && (myClient.isConnected())) {
      IMqttActionListener listener = new MqttServiceClientListener(
          resultBundle);
      try {
        myClient.unsubscribe(topic, invocationContext, listener);
      }
      catch (Exception e) {
        handleException(resultBundle, e);
      }
    }
    else {
      resultBundle.putString(MqttServiceConstants.CALLBACK_ERROR_MESSAGE,
          NOT_CONNECTED);

      service.traceError("subscribe", NOT_CONNECTED);
      service.callbackToActivity(clientHandle, Status.ERROR, resultBundle);
    }
  }

  /**
   * get tokens for all outstanding deliveries for a client
   * 
   * @return an array (possibly empty) of tokens
   */
  public IMqttDeliveryToken[] getPendingDeliveryTokens() {
    return myClient.getPendingDeliveryTokens();
  }

  // Implement MqttCallback

  /**
   * Callback for connectionLost
   * 
   * @param why
   *            the exeception causing the break in communications
   */
  @Override
  public void connectionLost(Throwable why) {
    service.traceDebug(TAG, "connectionLost(" + why.getMessage() + ")");

    // we protect against the phone switching off
    // by requesting a wake lock - we request the minimum possible wake
    // lock - just enough to keep the CPU running until we've finished
    PowerManager pm = (PowerManager) service
        .getSystemService(Service.POWER_SERVICE);
    WakeLock wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
        "MQTT - connectionLost");
    wl.acquire();

    try {
      myClient.disconnect(null, new IMqttActionListener() {

        @Override
        public void onSuccess(IMqttToken asyncActionToken) {
          // No action
        }

        @Override
        public void onFailure(IMqttToken asyncActionToken,
            Throwable exception) {
          // No action
        }
      });
    }
    catch (Exception e) {
      // ignore it - we've done our best
    }

    Bundle resultBundle = new Bundle();
    resultBundle.putString(MqttServiceConstants.CALLBACK_ACTION,
        MqttServiceConstants.ON_CONNECTION_LOST_ACTION);
    if (why != null) {
      resultBundle.putString(MqttServiceConstants.CALLBACK_ERROR_MESSAGE,
          why.getMessage());
      if (why instanceof MqttException) {
        resultBundle.putInt(MqttServiceConstants.CALLBACK_ERROR_NUMBER,
            ((MqttException) why).getReasonCode());
      }
      resultBundle.putString(
          MqttServiceConstants.CALLBACK_EXCEPTION_STACK,
          Log.getStackTraceString(why));
    }
    service.callbackToActivity(clientHandle, Status.OK, resultBundle);

    // we're finished - if the phone is switched off, it's okay for the CPU
    // to sleep now
    wl.release();
  }

  /**
   * Callback to indicate a message has been delivered (the exact meaning of
   * "has been delivered" is dependent on the QOS value)
   * 
   * @param messageToken
   *            the messge token provided when the message was originally sent
   */
  @Override
  public void deliveryComplete(IMqttDeliveryToken messageToken) {
    // we protect against the phone switching off
    // by requesting a wake lock - we request the minimum possible wake
    // lock - just enough to keep the CPU running until we've finished
    PowerManager pm = (PowerManager) service
        .getSystemService(Service.POWER_SERVICE);
    WakeLock wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
        "MQTT - deliveryComplete");
    wl.acquire();

    service.traceDebug(TAG, "deliveryComplete(" + messageToken + ")");

    MqttMessage message = savedSentMessages.remove(messageToken);
    if (message != null) { // If I don't know about the message, it's
      // irrelevant
      String topic = savedTopics.remove(messageToken);
      String activityToken = savedActivityTokens.remove(messageToken);
      String invocationContext = savedInvocationContexts
          .remove(messageToken);

      if (message.getQos() != 0) {
        Bundle resultBundle = messageToBundle(null, topic, message);
        if (activityToken != null) {
          resultBundle.putString(
              MqttServiceConstants.CALLBACK_ACTION,
              MqttServiceConstants.SEND_ACTION);
          resultBundle.putString(
              MqttServiceConstants.CALLBACK_ACTIVITY_TOKEN,
              activityToken);
          resultBundle.putString(
              MqttServiceConstants.CALLBACK_INVOCATION_CONTEXT,
              invocationContext);

          service.callbackToActivity(clientHandle, Status.OK,
              resultBundle);
        }
        resultBundle.putString(MqttServiceConstants.CALLBACK_ACTION,
            MqttServiceConstants.MESSAGE_DELIVERED_ACTION);
        service.callbackToActivity(clientHandle, Status.OK,
            resultBundle);
      }
    }

    // this notification will have kept the connection alive
    // so we can reset the next ping
    scheduleNextPing();

    // we're finished - if the phone is switched off, it's okay for the CPU
    // to sleep now
    wl.release();
  }

  /**
   * Callback when a message is received
   * 
   * @param topic
   *            the topic on which the message was received
   * @param message
   *            the message itself
   */
  @Override
  public void messageArrived(String topic, MqttMessage message)
      throws Exception {
    // we protect against the phone switching off
    // by requesting a wake lock - we request the minimum possible wake
    // lock - just enough to keep the CPU running until we've finished
    PowerManager pm = (PowerManager) service
        .getSystemService(Service.POWER_SERVICE);
    WakeLock wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
        "MQTT - messageArrived");
    wl.acquire();
    if (!topic.startsWith(MqttServiceConstants.PING_TOPIC_PREFIX)) { // ignore pings...
      service.traceDebug(TAG,
          "messageArrived(" + topic + ",{" + message.toString() + "}");

      String messageId = service.messageStore.storeArrived(clientHandle,
          topic, message);
      Bundle resultBundle = messageToBundle(messageId, topic, message);
      resultBundle.putString(MqttServiceConstants.CALLBACK_ACTION,
          MqttServiceConstants.MESSAGE_ARRIVED_ACTION);
      resultBundle.putString(MqttServiceConstants.CALLBACK_MESSAGE_ID,
          messageId);
      service.callbackToActivity(clientHandle, Status.OK, resultBundle);
    }

    // receiving this message will have kept the connection alive for
    // us, so we take advantage of this to postpone the next scheduled ping
    scheduleNextPing();

    // we're finished - if the phone is switched off, it's okay for the CPU
    // to sleep now
    wl.release();
  }

  /**
   * Store details of sent messages so we can handle "deliveryComplete"
   * callbacks from the mqttClient
   * 
   * @param topic
   * @param msg
   * @param messageToken
   * @param invocationContext
   * @param activityToken
   */
  private void storeSendDetails(final String topic, final MqttMessage msg,
      final IMqttDeliveryToken messageToken,
      final String invocationContext, final String activityToken) {
    savedTopics.put(messageToken, topic);
    savedSentMessages.put(messageToken, msg);
    savedActivityTokens.put(messageToken, activityToken);
    savedInvocationContexts.put(messageToken, invocationContext);
  }

  /*
   * Schedule the next time that you want the phone to wake up and ping the
   * message broker server
   */
  private void scheduleNextPing() {
    // When the phone is off, the CPU may be stopped. This means that our
    // code may stop running.
    // When connecting to the message broker, we specify a 'keep alive'
    // period - a period after which, if the client has not contacted
    // the server, even if just with a ping, the connection is considered
    // broken.
    // To make sure the CPU is woken at least once during each keep alive
    // period, we schedule a wake up to manually ping the server
    // thereby keeping the long-running connection open
    // Normally when using this Java MQTT client library, this ping would be
    // handled for us.
    // Note that this may be called multiple times before the next scheduled
    // ping has fired. This is good - the previously scheduled one will be
    // cancelled in favour of this one.
    // This means if something else happens during the keep alive period,
    // (e.g. we receive an MQTT message), then we start a new keep alive
    // period, postponing the next ping.

    PendingIntent pendingIntent = PendingIntent.getBroadcast(service, 0,
        new Intent(alarmAction), PendingIntent.FLAG_UPDATE_CURRENT);

    Calendar wakeUpTime = Calendar.getInstance();
    wakeUpTime.add(Calendar.SECOND, connectOptions.getKeepAliveInterval());

    AlarmManager aMgr = (AlarmManager) service
        .getSystemService(Context.ALARM_SERVICE);
    aMgr.set(AlarmManager.RTC_WAKEUP, wakeUpTime.getTimeInMillis(),
        pendingIntent);
  }

  /**
   * general-purpose IMqttActionListener for the Client context
   * <p>
   * Simply handles the basic success/failure cases for operations which don't
   * return results
   * 
   */
  private class MqttServiceClientListener implements IMqttActionListener {

    private final Bundle resultBundle;

    private MqttServiceClientListener(Bundle resultBundle) {
      this.resultBundle = resultBundle;
    }

    @Override
    public void onSuccess(IMqttToken asyncActionToken) {
      service.callbackToActivity(clientHandle, Status.OK, resultBundle);
    }

    @Override
    public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
      resultBundle.putString(MqttServiceConstants.CALLBACK_ERROR_MESSAGE,
          exception.getLocalizedMessage());
      if (exception instanceof MqttException) {
        resultBundle.putInt(MqttServiceConstants.CALLBACK_ERROR_NUMBER,
            ((MqttException) exception).getReasonCode());
      }
      service.callbackToActivity(clientHandle, Status.ERROR, resultBundle);
    }
  }

  /*
   * Used to implement a keep-alive protocol at this Service level - it sends
   * a PING message to the server, then schedules another ping after an
   * interval defined by keepAliveSeconds
   */
  public class PingSender extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
      // Note that we don't need a wake lock for this method (even though
      // it's important that the phone doesn't switch off while we're
      // doing this).
      // According to the docs, "Alarm Manager holds a CPU wake lock as
      // long as the alarm receiver's onReceive() method is executing.
      // This guarantees that the phone will not sleep until you have
      // finished handling the broadcast."
      // This is good enough for our needs.

      ping();
      // start the next keep alive period
      scheduleNextPing();
    }
  }

  public void ping() {
    // Temporary ping mechanism...
    try {
      myClient.publish(pingTopic, new byte[]{0}, 1, false, null, null);
    }
    catch (MqttException e) {
      // ignore it?
    }

  }

  public Debug getDebug()
  {
    return myClient.getDebug();
  }

}
