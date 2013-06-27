/**
 * 
 */
package m.drl.mqtt;

import android.app.Application;

/**
 * @author hljdrl@gmail.com
 * 
 */
public class MqttApplication extends Application {

	private static MqttApplication mInstance = null;

	@Override
	public void onCreate() {
		super.onCreate();
		mInstance = this;
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
