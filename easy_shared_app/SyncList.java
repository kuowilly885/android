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

public class SyncList extends TabActivity {
	static final String TAB_APP = "Application";
	static final String TAB_CONTACT = "Contact";
	

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		final TabHost tabHost = getTabHost();
		TabHost.TabSpec tabSpec1, tabSpec2;
		tabSpec1=tabHost.newTabSpec(TAB_APP)
			.setIndicator(getString(R.string.app))
			.setContent(new Intent(this, AppSyncList.class));

		tabSpec2=tabHost.newTabSpec(TAB_CONTACT)
			.setIndicator(getString(R.string.contact))
			.setContent(new Intent(this, ContactSyncList.class));
	

		tabHost.addTab(tabSpec1);
		tabHost.addTab(tabSpec2);
		
		tabHost.setCurrentTabByTag(TAB_APP);

		
	}
}