package com.greenway.landscapes.mobile.client;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;

public class SubmitActivity extends Activity {

	String firstName;
	String lastName;
	String addr;
	CheckBox spClean, snowRemove, lawn, soil, tree;
	boolean bspClean, bsnowRemove, blawn, bsoil, btree;
	Button submit;
	Thread tSend;
	Socket mySocket;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_submit);
		spClean = (CheckBox) findViewById(R.id.checkSp);
		snowRemove = (CheckBox) findViewById(R.id.checkSnow);
		lawn = (CheckBox) findViewById(R.id.checkLawn);
		soil = (CheckBox) findViewById(R.id.checkSoil);
		tree = (CheckBox) findViewById(R.id.checkTree);
		submit = (Button) findViewById(R.id.buttonSubmit);
//		firstName= (String) getIntent().getExtras().getString("STRING_FIRSTNAME");
//		lastName= (String) getIntent().getExtras().getString("STRING_LASTNAME");
//		addr= (String) getIntent().getExtras().getString("ADDR");
		
		submit.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				bspClean = spClean.isChecked();
				bsnowRemove = snowRemove.isChecked();
				blawn = lawn.isChecked();
				bsoil = soil.isChecked();
				btree = tree.isChecked();
				tSend = new Thread(send);
				submit.setEnabled(false);
				tSend.start();
			}
		});
		
	}
	
    private Runnable send = new Runnable() {
        @Override
        public void run() {
			try {
				mySocket = new Socket(InetAddress.getByName("108.5.228.222"), 2597);
				String job = "";
				if (bspClean)
					job += "Spring Fall Clean Up\n";
				if (bsnowRemove)
					job += "Snow Removal\n";
				if (blawn)
					job += "Weekly Lawn Maintenance\n";
				if (bsoil)
					job += "Top Soil\n";
				if (btree)
					job += "Tree Removal\n";
				DataOutputStream dataOutputStream = new DataOutputStream(mySocket.getOutputStream());
				dataOutputStream.writeUTF("Customer Name : " + firstName + " " + lastName + "\n" + 
				"Job Request : \n" + job);
				dataOutputStream.close();
				mySocket.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				Log.d("Jet", e.getMessage());
			}
        }
    };
}
