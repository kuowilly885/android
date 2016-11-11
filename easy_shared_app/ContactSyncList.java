package com.insyde.sharekantan;

import android.os.Bundle;
import android.app.ListActivity;
import android.widget.ArrayAdapter;

public class ContactSyncList extends ListActivity {
	private ArrayAdapter contactAdapter; 
	private String[] contactSyncList = new String[] {
			"cat", "dog", "owl", "tiger", "fish", "rabbit"
	        }; 
	  public void onCreate(Bundle savedInstanceState) {
		  super.onCreate(savedInstanceState);
		   
	    contactAdapter = new ArrayAdapter<String>(this,android.R.layout.simple_list_item_multiple_choice, contactSyncList);
	    setListAdapter(contactAdapter);
	  }
	} 