package com.insyde.sharekantan;

import android.os.Bundle;
import android.app.Activity;
import android.view.Menu;
import android.widget.ArrayAdapter;
import android.widget.ListView;

public class SyncContactSetting extends Activity {
	ListView mListView; 
	ArrayAdapter<String> contactAdapter; 
	private String[] appList=
		{
			"7", "8", "9", "10" //needs to stuff with real contact list
		};
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.contact);
    

    mListView = (ListView) findViewById(R.id.deviceList); 
    contactAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_multiple_choice, appList); 
    mListView.setAdapter(contactAdapter); 
    }
}