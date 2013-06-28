package m.drl.mqtt;

import m.drl.mqtt.fragment.SwipeyTabsAdapter;
import m.drl.mqtt.fragment.SwipeyTabsView;
import m.drl.mqtt.fragment.TabsAdapter;
import m.drl.mqtt.fragment.VPAFAdapter;
import android.os.Bundle;
import android.support.v4.view.ViewPager;

/**
 * @author hljdrl@gmail.com
 * 
 */
public class HomeActivity extends BaseActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setuViews();
	}

	private ViewPager mPager;
	private SwipeyTabsView mSwipeyTabs;
	private TabsAdapter mSwipeyTabsAdapter;
	private String titles [] = new String[] { "Info", "消息", "资料" };
	private void setuViews() {
		setContentView(R.layout.root_home_r2);
		initViewPager(10, 0xFFFFFFFF, 0xFF000000);
		mSwipeyTabs = (SwipeyTabsView) findViewById(R.id.swipey_tabs);
		mSwipeyTabsAdapter = new SwipeyTabsAdapter(this,titles);
		mSwipeyTabs.setAdapter(mSwipeyTabsAdapter);
		mSwipeyTabs.setViewPager(mPager);
	}

	private void initViewPager(int pageCount, int backgroundColor, int textColor) {
		mPager = (ViewPager) findViewById(R.id.pager);
		mPager.setAdapter(new VPAFAdapter(getSupportFragmentManager(),titles));
		mPager.setCurrentItem(1);
		mPager.setPageMargin(1);
	}

}
