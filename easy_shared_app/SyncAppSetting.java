package com.insyde.sharekantan;

import android.os.Bundle;
import android.app.Activity;
import android.view.Menu;
import android.widget.ArrayAdapter;
import android.widget.ListView;

public class SyncAppSetting extends Activity {
	ListView mListView; 
	ArrayAdapter<String> appAdapter; 
	private String[] appList=
		{
			"1", "2", "3", "4" //needs to stuff with real app list
		};
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.app);
    

    mListView = (ListView) findViewById(R.id.deviceList); 
    appAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_multiple_choice, appList); 
    mListView.setAdapter(appAdapter); 
    }
}
