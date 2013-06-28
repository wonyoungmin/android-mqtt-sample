/**
 * 
 */
package m.drl.mqtt;

import java.util.UUID;

import m.drl.mqtt.util.DeviceHelper;
import m.drl.mqtt.util.StringHelper;
import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

/**
 * @author hljdrl@gmail.com
 * 
 */
public class MqttApplication extends Application {

	private static MqttApplication mInstance = null;
	private String deviceId;

	public final String getDeviceId() {
		return deviceId;
	}

	@Override
	public void onCreate() {
		super.onCreate();
		mInstance = this;
		SharedPreferences sp = PreferenceManager
				.getDefaultSharedPreferences(this);
		deviceId = sp.getString("deviceId", "");

		if (!StringHelper.isText(deviceId)) {
			deviceId = DeviceHelper.getDeviceId(this);
			sp.edit().putString("deviceId", deviceId).commit();
		}
		if (!StringHelper.isText(deviceId)) {
			deviceId = UUID.randomUUID().toString();
			sp.edit().putString("deviceId", deviceId).commit();
		}

	}

	public static MqttApplication getInstance() {
		return mInstance;
	}

	private boolean mqttStart = Boolean.FALSE;

	public final void setMqttStart(boolean b) {
		this.mqttStart = b;
	}

	public final boolean isMqttStart() {
		return mqttStart;
	}

}
