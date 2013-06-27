package m.drl.mqtt;

import java.util.LinkedList;
import java.util.List;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ListView;

/**
 * @author hljdrl@gmail.com
 * 
 */
public class HomeActivity extends Activity implements OnPushMesageListener,
		OnClickListener {

	private Button btn_start;
	private Button btn_stop;
	private ListView lv_mesages;
	private List<String> messagList = new LinkedList<String>();
	private MesageAdapter mesageAdapter;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.root_home);
		
		btn_start = (Button) findViewById(R.id.button1);
		btn_stop = (Button) findViewById(R.id.button2);
		lv_mesages = (ListView) findViewById(R.id.lv_msg);
		mesageAdapter = new MesageAdapter(this, messagList);
		lv_mesages.setAdapter(mesageAdapter);

		btn_start.setOnClickListener(this);
		btn_stop.setOnClickListener(this);
		btn_stop.setEnabled(false);
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.button1:
			PushMsgService.actionStart(this);
			v.setEnabled(false);
			btn_stop.setEnabled(true);
			break;
		case R.id.button2:
			PushMsgService.actionStop(this);
			v.setEnabled(false);
			btn_start.setEnabled(true);
			break;
		}
	}

	@Override
	protected void onStart() {
		super.onStart();
		PushMsgService.addPushMesageListeners(this);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
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
