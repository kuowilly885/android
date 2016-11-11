package com.insyde.sharekantan;

import android.os.Bundle;
import android.app.Activity;
import android.view.Menu;
import android.os.Bundle;
import android.app.TabActivity;
import android.content.Intent;
import android.widget.RelativeLayout;
import android.widget.TabHost;
import android.widget.TabWidget;
import android.widget.TextView;

@SuppressWarnings("deprecation")
public class SyncTool extends TabActivity {
	static final String TAB_WIFI = "Wifi";
	static final String TAB_BT = "Bluetooth";
	

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.tabs); 

		final TabHost tabHost = getTabHost();
		TabHost.TabSpec tabSpec1, tabSpec2;
		tabSpec1=tabHost.newTabSpec(TAB_WIFI)
			.setIndicator(getString(R.string.wifi))
			.setContent(new Intent(this, WifiSetting.class));

		tabSpec2=tabHost.newTabSpec(TAB_BT)
			.setIndicator(getString(R.string.bt))
			.setContent(new Intent(this, BtSetting.class));
	

		tabHost.addTab(tabSpec1);
		tabHost.addTab(tabSpec2);
		
		tabHost.setCurrentTabByTag(TAB_WIFI);

		
	}
}