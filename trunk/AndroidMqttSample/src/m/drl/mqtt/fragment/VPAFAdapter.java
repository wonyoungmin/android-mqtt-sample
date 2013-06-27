package m.drl.mqtt.fragment;

import m.drl.mqtt.ft.MessageFragment;
import m.drl.mqtt.ft.NoteFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
/**
 * @author hljdrl@gmail.com
 * 
 */
public class VPAFAdapter extends FragmentPagerAdapter {

	private String[] TITLES;

	public VPAFAdapter(FragmentManager fm) {
		super(fm);
		TITLES = new String[]{"消息","资料"};
	}

	public Fragment getItem(int arg0) {
		switch (arg0) {
		case 0:
			return new MessageFragment();
		case 1:
			return new NoteFragment();
		default:
			break;

		}
		return null;
	}

	@Override
	public int getCount() {
		return TITLES.length;
	}

	@Override
	public CharSequence getPageTitle(int position) {
		return TITLES[position % TITLES.length];
	}

}
