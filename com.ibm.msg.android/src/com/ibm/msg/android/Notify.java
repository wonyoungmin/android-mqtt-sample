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

import java.util.Calendar;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

/**
 * Provides static methods for creating and showing notifications to the user.
 *
 */
public class Notify {

  /** Message ID Counter **/
  private static int MessageID = 0;

  /**
   * Displays a notification in the notification area of the UI
   * @param context Context from which to create the notification
   * @param messageString The string to display to the user as a message
   * @param intent The intent which will start the activity when the user clicks the notification
   * @param notifcationTitle The resource reference to the notification title
   */
  @SuppressWarnings("deprecation")
  static void notifcation(Context context, String messageString, Intent intent, int notficationTitle) {

    //Get the notification manage which we will use to display the notification
    String ns = Context.NOTIFICATION_SERVICE;
    NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(ns);

    Calendar.getInstance().getTime().toString();

    long when = System.currentTimeMillis();

    //get the notification title from the application's strings.xml file
    CharSequence contentTitle = context.getString(notficationTitle);

    //the message that will be displayed as thr ticker
    String ticker = contentTitle + " " + messageString;

    //build the pending intent that will start the apparoiate activity
    PendingIntent pendingIntent = PendingIntent.getActivity(context,
        ActivityConstants.showHistory, intent, 0);

    //build the notification
    Notification notification = new Notification(R.drawable.ic_launcher, ticker, when);
    notification.setLatestEventInfo(context, contentTitle, messageString, pendingIntent);

    //display the notification
    mNotificationManager.notify(MessageID, notification);
    MessageID++;
  }

  /**
   * Display a toast notification to the user
   * @param context Context from which to create a notification
   * @param text The text the toast should display
   * @param duration The amount of time for the toast to appear to the user
   */
  static void toast(Context context, CharSequence text, int duration) {
    Toast toast = Toast.makeText(context, text, duration);
    toast.show();
  }

}
