/**
 * 
 */
package m.drl.mqtt.ft;

import java.util.ArrayList;
import java.util.List;

import m.drl.mqtt.InfoAdapter;
import m.drl.mqtt.MqttApplication;
import m.drl.mqtt.PushMsgService;
import m.drl.mqtt.R;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

/**
 * @author hljdrl@gmail.com
 * 
 */
public class InfoFragment extends Fragment {

	private View view;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		view = inflater
				.inflate(m.drl.mqtt.R.layout.root_list, container, false);
		return view;
	}

	private ListView listview;

	private List<String> getNotes() {
		List<String> mLm = new ArrayList<String>();
		mLm.add("Client ID: "+MqttApplication.getInstance().getDeviceId());
		mLm.add("Host: "+PushMsgService.MQTT_HOST);
		mLm.add("Port: "+PushMsgService.MQTT_HOST_PORT);
		mLm.add("Clean Session: "+PushMsgService.CLEAN_SESSION);
		return mLm;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		listview = (ListView) view.findViewById(R.id.root_listview);
		listview.setAdapter(new InfoAdapter(getActivity(), getNotes()));
	}

}
