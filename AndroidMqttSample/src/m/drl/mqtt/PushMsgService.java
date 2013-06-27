/**
 * 
 */
package m.drl.mqtt;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

/**
 * @author hljdrl@gmail.com
 * 
 */
public class PushMsgService extends Service implements MqttCallback {
	private String TAG = "PushMsgService";

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	private MqttClient mMqttClient;
	private static String Client_id;
	private String MQTT_HOST = "192.168.0.83";
	private String MQTT_HOST_PORT = "1883";
	private String SUBSCRIBE_MESAGE = "mqtt_android_message";

	private static final String ACTION_START = Client_id + ".START";
	private static final String ACTION_STOP = Client_id + ".STOP";

	// private static final String ACTION_KEEPALIVE = Client_id + ".KEEP_ALIVE";
	// private static final String ACTION_RECONNECT = Client_id + ".RECONNECT";

	// Static method to start the service
	public static void actionStart(Context ctx) {
		Intent i = new Intent(ctx, m.drl.mqtt.PushMsgService.class);
		i.setAction(ACTION_START);
		ctx.startService(i);
	}

	// Static method to stop the service
	public static void actionStop(Context ctx) {
		Intent i = new Intent(ctx, m.drl.mqtt.PushMsgService.class);
		i.setAction(ACTION_STOP);
		ctx.startService(i);
	}

	private static List<OnPushMesageListener> mOnPushMesageListeners = new ArrayList<OnPushMesageListener>();

	public final static List<OnPushMesageListener> getOnPushMesageListeners() {
		return java.util.Collections.unmodifiableList(mOnPushMesageListeners);
	}

	private final static void nofityPushMessage(String msg) {
		List<OnPushMesageListener> mLm = getOnPushMesageListeners();
		for (OnPushMesageListener l : mLm) {
			try {
				if (l != null)
					l.onNewPushMessage(msg);
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
	}

	public final static void addPushMesageListeners(
			OnPushMesageListener mOnPushMesageListeners) {
		PushMsgService.mOnPushMesageListeners.add(mOnPushMesageListeners);
	}

	public final static void romvePushMesageListeners(
			OnPushMesageListener mOnPushMesageListeners) {
		PushMsgService.mOnPushMesageListeners.remove(mOnPushMesageListeners);
	}

	@Override
	public void onCreate() {
		super.onCreate();
		mNotifMan = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		Client_id = "android_client";
	}

	@Override
	public void onStart(Intent intent, int startId) {
		super.onStart(intent, startId);
		// Do an appropriate action based on the intent.
		if (intent.getAction().equals(ACTION_STOP) == true) {
			stop();
			stopSelf();
		} else if (intent.getAction().equals(ACTION_START) == true) {
			onStartConnectioned();
		}
		// } else if (intent.getAction().equals(ACTION_KEEPALIVE) == true) {
		// keepAlive();
		// } else if (intent.getAction().equals(ACTION_RECONNECT) == true) {
		// if (isNetworkAvailable()) {
		// reconnectIfNecessary();
		// }
		// }
	}

	private void onStartConnectioned() {
		try {
			mMqttClient = new MqttClient("tcp://192.168.0.83:1883", Client_id,
					null);
			// mMqttClient = new MqttClient("tcp://" + MQTT_HOST + ":"
			// + MQTT_HOST_PORT, Client_id, null);
			MqttConnectOptions conOptions = new MqttConnectOptions();
			conOptions.setCleanSession(true);
			mMqttClient.setCallback(this);
			mMqttClient.connect(conOptions);
			mMqttClient.subscribe(SUBSCRIBE_MESAGE, 1);

		} catch (MqttException e) {
			e.printStackTrace();
		}
	}

	private void stop() {
		if (mMqttClient != null && mMqttClient.isConnected()) {
			try {
				mMqttClient.disconnect();
			} catch (MqttException e) {
				e.printStackTrace();
			}
		}
	}

	private NotificationManager mNotifMan;

	private void showNotification(String text) {
		Notification n = new Notification();
		n.flags |= Notification.FLAG_SHOW_LIGHTS;
		n.flags |= Notification.FLAG_AUTO_CANCEL;
		n.defaults = Notification.DEFAULT_ALL;
		n.icon = m.drl.mqtt.R.drawable.ic_launcher;
		n.when = System.currentTimeMillis();
		PendingIntent pi = PendingIntent
				.getActivity(this, 0, new Intent(""), 0);
		n.setLatestEventInfo(this, "Push", text, pi);

		mNotifMan.notify((int)System.currentTimeMillis(), n);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();

	}

	@Override
	public void connectionLost(Throwable arg0) {
		Log.d(TAG, "- connectionLost()");
		Log.e(TAG, "- ", arg0);
	}

	@Override
	public void deliveryComplete(IMqttDeliveryToken arg0) {
		Log.d(TAG, "- deliveryComplete()");
		try {
			MqttMessage msg = arg0.getMessage();
			if (msg != null) {
				byte bytes[] = msg.getPayload();
				if (bytes != null) {
					String message = new String(bytes);
					Log.d(TAG, "- deliveryComplete()-mesage-" + message);
					showNotification(message);
					nofityPushMessage(message);
				}
			}
		} catch (MqttException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void messageArrived(String arg0, MqttMessage arg1) throws Exception {
		Log.d(TAG, "- messageArrived()");
		if (arg1 != null) {
			byte bytes[] = arg1.getPayload();
			if (bytes != null) {
				String message = new String(bytes);
				Log.d(TAG, "- deliveryComplete()-mesage-" + message);
				showNotification(message);
				nofityPushMessage(message);
			}
		}
	}

}
