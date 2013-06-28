package m.drl.mqtt.fragment;

import java.util.ArrayList;
import java.util.List;

import m.drl.mqtt.ft.InfoFragment;
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

	private final String[] TITLES;
	List<Fragment> fts = new ArrayList<Fragment>();

	public VPAFAdapter(FragmentManager fm, String args[]) {
		super(fm);
		TITLES = args;
		fts.add(new InfoFragment());
		fts.add(new MessageFragment());
		fts.add(new NoteFragment());
	}

	public Fragment getItem(int arg0) {
		return fts.get(arg0);
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
