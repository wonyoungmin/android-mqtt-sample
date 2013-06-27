/**
 * 
 */
package m.drl.mqtt;

import java.util.List;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

/**
 * @author hljdrl@gmail.com
 * 
 */
public final class MesageAdapter extends ArrayAdapter<String> {
	LayoutInflater in;

	public MesageAdapter(Context context, List<String> lst) {
		super(context, 0, lst);
		in = LayoutInflater.from(context);
	}

	@Override
	public View getView(int position, View v, ViewGroup parent) {
		final String msg = getItem(position);
		if (v == null) {
			v = in.inflate(R.layout.list_item, parent, false);
		}
		TextView tv = (TextView) v.findViewById(R.id.tv_msg);
		tv.setText(msg);
		return v;
	}

}
