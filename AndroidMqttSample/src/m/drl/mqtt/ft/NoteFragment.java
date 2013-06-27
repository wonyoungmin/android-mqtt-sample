/**
 * 
 */
package m.drl.mqtt.ft;

import java.util.ArrayList;
import java.util.List;

import m.drl.mqtt.MesageAdapter;
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
public class NoteFragment extends Fragment {

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
		mLm.add("Apache ActiveMQ:支持MQTT协议服务器");
		mLm.add("Apache Apollo:支持MQTT协议服务器");
		mLm.add("MQTT调试器 : WMQTT Unility.exe");
		mLm.add("MQTT资料网址 : http://mqtt.org/software");
		mLm.add("MQTT Java Jar : Eclipse Paho");

		return mLm;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		listview = (ListView) view.findViewById(R.id.root_listview);
		listview.setAdapter(new MesageAdapter(getActivity(), getNotes()));
	}

}
