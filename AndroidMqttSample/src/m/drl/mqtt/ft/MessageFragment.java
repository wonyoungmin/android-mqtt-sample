/**
 * 
 */
package m.drl.mqtt.ft;

import java.util.LinkedList;
import java.util.List;

import m.drl.mqtt.MesageAdapter;
import m.drl.mqtt.MqttApplication;
import m.drl.mqtt.OnPushMesageListener;
import m.drl.mqtt.PushMsgService;
import m.drl.mqtt.R;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ListView;

/**
 * @author hljdrl@gmail.com
 * 
 */
public class MessageFragment extends Fragment implements OnClickListener,
		OnPushMesageListener {

	private View view;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		view = inflater.inflate(R.layout.root_ft_message, container, false);
		return view;
	}

	private Button btn_start;
	private Button btn_stop;
	private ListView lv_mesages;
	private List<String> messagList = new LinkedList<String>();
	private MesageAdapter mesageAdapter;

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		btn_start = (Button) view.findViewById(R.id.button1);
		btn_stop = (Button) view.findViewById(R.id.button2);
		lv_mesages = (ListView) view.findViewById(R.id.lv_msg);
		mesageAdapter = new MesageAdapter(getActivity(), messagList);
		lv_mesages.setAdapter(mesageAdapter);

		btn_stop.setEnabled(false);

		boolean start = MqttApplication.getInstance().isMqttStart();
		if (start) {
			btn_start.setEnabled(false);
			btn_stop.setEnabled(true);
		}
		//
		btn_start.setOnClickListener(this);
		btn_stop.setOnClickListener(this);

	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.button1:
			PushMsgService.actionStart(getActivity());
			v.setEnabled(false);
			btn_stop.setEnabled(true);
			break;
		case R.id.button2:
			PushMsgService.actionStop(getActivity());
			v.setEnabled(false);
			btn_start.setEnabled(true);
			break;
		}
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		PushMsgService.addPushMesageListeners(this);
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		PushMsgService.romvePushMesageListeners(this);
	}

	@Override
	public void onNewPushMessage(String message) {

		Message msg = handler.obtainMessage(10);
		msg.obj = message;
		handler.sendMessage(msg);
	}

	android.os.Handler handler = new Handler() {
		public void handleMessage(android.os.Message msg) {
			switch (msg.what) {
			case 10:
				String mesage = (String) msg.obj;
				if (mesage != null) {
					messagList.add(mesage);
					mesageAdapter.notifyDataSetChanged();
				}
				break;
			}
		}
	};

}
