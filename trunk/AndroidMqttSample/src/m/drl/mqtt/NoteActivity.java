/**
 * 
 */
package m.drl.mqtt;

import java.util.ArrayList;
import java.util.List;

import android.os.Bundle;
import android.widget.ListView;

/**
 * @author hljdrl@gmail.com
 *
 */
public final class NoteActivity extends BaseActivity{
	
	private ListView listview;
	@Override
	protected void onCreate(Bundle arg0) {
		super.onCreate(arg0);
	}
	
	
	private List<String> getNotes()
	{
		List<String> mLm = new ArrayList<String>();
		mLm.add("Apache ActiveMQ:支持MQTT协议服务器");
		mLm.add("Apache Apollo:支持MQTT协议服务器");
		mLm.add("MQTT调试器 : WMQTT Unility.exe");
		mLm.add("MQTT资料网址 : http://mqtt.org/software");
		mLm.add("MQTT Java Jar : Eclipse Paho");
		
		return mLm;
	}

	
	
	
}
