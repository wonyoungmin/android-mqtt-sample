/*
 * Licensed Materials - Property of IBM
 *
 * 5747-SM3
 *
 * (C) Copyright IBM Corp. 1999, 2012 All Rights Reserved.
 *
 * US Government Users Restricted Rights - Use, duplication or
 * disclosure restricted by GSA ADP Schedule Contract with
 * IBM Corp.
 *
 */
package com.ibm.msg.android;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Map;

import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;

import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.ibm.msg.android.Connection.ConnectionStatus;
import com.ibm.msg.android.service.MqttClientAndroidService;

/**
 * ClientConnections is the main activity for the sample application, it
 * displays all the active connections.
 * 
 */
public class ClientConnections extends ListActivity {

  /**
   * Token to pass to the MQTT Service
   */
  final static String TOKEN = "com.ibm.msg.android.ClientConnections";

  /**
   * ArrayAdapter to populate the list view
   */
  private ArrayAdapter<Connection> arrayAdapter = null;

  /**
   * {@link ChangeListener} for use with all {@link Connection} objects created by this instance of <code>ClientConnections</code>
   */
  private ChangeListener changeListener = new ChangeListener();

  /**
   * This instance of <code>ClientConnections</code> used to update the UI in {@link ChangeListener}
   */
  private ClientConnections clientConnections = this;

  /**
   * @see android.app.ListActivity#onCreate(Bundle)
   */
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    ListView connectionList = getListView();

    connectionList.setTextFilterEnabled(true);

    arrayAdapter = new ArrayAdapter<Connection>(this,
        R.layout.connection_text_view);
    setListAdapter(arrayAdapter);

    // get all the available connections
    Map<String, Connection> connections = Connections.getInstance()
        .getConnections();

    if (connections != null) {
      for (String s : connections.keySet())
      {
        arrayAdapter.add(connections.get(s));
      }
    }

  }

  /**
   * Creates the action bar for the activity
   * 
   * @see ListActivity#onCreateOptionsMenu(Menu)
   */
  @Override
  public boolean onCreateOptionsMenu(Menu menu) {

    OnMenuItemClickListener menuItemClickListener = new Listener(this);

    //load the correct menu depending on the status of logging
    if (Listener.logging)
    {
      getMenuInflater().inflate(R.menu.activity_connections_logging, menu);
      menu.findItem(R.id.endLogging).setOnMenuItemClickListener(menuItemClickListener);
      menu.findItem(R.id.dumpLog).setOnMenuItemClickListener(menuItemClickListener);
    }
    else {
      getMenuInflater().inflate(R.menu.activity_connections, menu);
      menu.findItem(R.id.startLogging).setOnMenuItemClickListener(menuItemClickListener);
    }

    menu.findItem(R.id.newConnection).setOnMenuItemClickListener(
        menuItemClickListener);

    return true;
  }

  /**
   * Listens for item clicks on the view
   * 
   * @param listView
   *            The list view where the click originated from
   * @param view
   *            The view which was clicked
   * @param position
   *            The position in the list that was clicked
   */
  @Override
  protected void onListItemClick(ListView listView, View view, int position,
      long id) {
    super.onListItemClick(listView, view, position, id);

    Connection c = arrayAdapter.getItem(position);

    // start the connectionDetails activity to display the details about the
    // selected connection
    Intent intent = new Intent();
    intent.setClassName(getApplicationContext().getPackageName(),
        "com.ibm.msg.android.ConnectionDetails");
    intent.putExtra("handle", c.handle());
    startActivity(intent);

  }

  /**
   * @see ListActivity#onActivityResult(int,int,Intent)
   */
  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {

    if (resultCode == RESULT_CANCELED) {
      return;
    }

    Bundle dataBundle = data.getExtras();

    // perform connection create and connect
    connectAction(dataBundle);

  }

  /**
   * @see ListActivity#onResume()
   */
  @Override
  protected void onResume() {
    super.onResume();
    arrayAdapter.notifyDataSetChanged();
  }

  /**
   * @see ListActivity#onDestroy()
   */
  @Override
  protected void onDestroy() {

    Map<String, Connection> connections = Connections.getInstance().getConnections();

    for (String s : connections.keySet())
    {
      connections.get(s).removeChangeListener(changeListener);
    }
    super.onDestroy();
  }

  /**
   * Process data from the connect action
   * 
   * @param data the {@link Bundle} returned by the {@link NewConnection} Acitivty
   */
  private void connectAction(Bundle data) {

    // The basic client information
    String server = (String) data.get(ActivityConstants.server);
    String clientId = (String) data.get(ActivityConstants.clientId);
    int port = Integer.parseInt((String) data.get(ActivityConstants.port));
    boolean cleanSession = (Boolean) data.get(ActivityConstants.cleanSession);

    boolean ssl = (Boolean) data.get(ActivityConstants.ssl);
    String uri = null;
    if (ssl) {
      uri = "ssl://";
    }
    else {
      uri = "tcp://";
    }

    uri = uri + server + ":" + port;

    MqttClientAndroidService client;
    client = Connections.getInstance().createClient(this, uri, clientId);
    // create a client handle
    String clientHandle = uri + clientId;

    // last will message
    String message = (String) data.get(ActivityConstants.message);
    String topic = (String) data.get(ActivityConstants.topic);
    Integer qos = (Integer) data.get(ActivityConstants.qos);
    Boolean retained = (Boolean) data.get(ActivityConstants.retained);

    // connection options

    String username = (String) data.get(ActivityConstants.username);

    String password = (String) data.get(ActivityConstants.password);

    int timeout = (Integer) data.get(ActivityConstants.timeout);
    int keepalive = (Integer) data.get(ActivityConstants.keepalive);

    Connection connection = new Connection(clientHandle, clientId, server, port,
        this, client);
    arrayAdapter.add(connection);

    connection.registerChangeListener(changeListener);
    Connections.getInstance().addConnection(connection);
    // connect client

    String[] actionArgs = new String[1];
    actionArgs[0] = clientId;
    connection.changeConnectionStatus(ConnectionStatus.CONNECTING);

    MqttConnectOptions conOpt = new MqttConnectOptions();
    conOpt.setCleanSession(cleanSession);
    conOpt.setConnectionTimeout(timeout);
    conOpt.setKeepAliveInterval(keepalive);
    if (!username.equals(ActivityConstants.empty)) {
      conOpt.setUserName(username);
    }
    if (!password.equals(ActivityConstants.empty)) {
      conOpt.setPassword(password.toCharArray());
    }

    final ActionListener callback = new ActionListener(this,
        ActionListener.Action.CONNECT, clientHandle, actionArgs);

    boolean doConnect = true;

    if ((!message.equals(ActivityConstants.empty))
        || (!topic.equals(ActivityConstants.empty))) {
      // need to make a message since last will is set
      try {
        conOpt.setWill(topic, message.getBytes(), qos.intValue(),
            retained.booleanValue());
      }
      catch (Exception e) {
        doConnect = false;
        callback.onFailure(null, e);
      }
    }
    client.setCallback(new MqttCallbackHandler(this, clientHandle));
    connection.addConnectionOptions(conOpt);

    if (doConnect) {
      try {
        client.connect(conOpt, null, callback);
      }
      catch (MqttException e) {
        Log.e(this.getClass().getCanonicalName(),
            "MqttException Occured", e);
      }
    }

  }

  /**
   * This class ensures that the user interface is updated as the Connection objects change their states
   * 
   *
   */
  private class ChangeListener implements PropertyChangeListener {

    /**
     * @see java.beans.PropertyChangeListener#propertyChange(java.beans.PropertyChangeEvent)
     */
    @Override
    public void propertyChange(PropertyChangeEvent event) {

      if (!event.getPropertyName().equals(ActivityConstants.ConnectionStatusProperty)) {
        return;
      }
      clientConnections.runOnUiThread(new Runnable() {

        @Override
        public void run() {
          clientConnections.arrayAdapter.notifyDataSetChanged();
        }

      });

    }

  }
}
