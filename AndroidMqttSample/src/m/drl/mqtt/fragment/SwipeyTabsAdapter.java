package m.drl.mqtt.fragment;

import m.drl.mqtt.R;
import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;

public class SwipeyTabsAdapter implements TabsAdapter {
	
	private Activity mContext;
	
	private String[] mTitles = {
	    "消息", "资料"
	};
	
	public SwipeyTabsAdapter(Activity ctx) {
		this.mContext = ctx;
	}
	
	@Override
	public View getView(int position) {
		SwipeyTabButton tab;
		
		LayoutInflater inflater = mContext.getLayoutInflater();
		tab = (SwipeyTabButton) inflater.inflate(R.layout.astuetz_tab_swipey, null);
		
		if (position < mTitles.length) tab.setText(mTitles[position]);
		
		return tab;
	}
	
}
