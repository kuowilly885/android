package com.greenway.landscapes.mobile.client;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

import com.greenway.landscapes.common.UserObject;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;

public class LoginActivity extends Activity {

	Button nextPage;
	EditText Eusername, Epassword;
	String username, password;
	Thread tSend;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_user);
		Eusername = (EditText) findViewById(R.id.editTextUsername);
		Epassword = (EditText) findViewById(R.id.editTextPassword);

		nextPage = (Button) findViewById(R.id.buttonNextPage);
		nextPage.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				username = Eusername.getText().toString();
				password = Epassword.getText().toString();
				if (!username.equals("") && !password.equals(""))
				{
					new Thread(send).start();
				}
			}
		});
	}

    private Runnable send = new Runnable() {
        @Override
        public void run() {
			try {
				Util.mySocket = new Socket("jeremyhoc.com", 2597);
				String up = username + " " + password;
				ObjectOutputStream objectOutputStream = new ObjectOutputStream(Util.mySocket.getOutputStream());
				UserObject user = new UserObject();
				user.setUsername(username);
				user.setPassword(password);
				objectOutputStream.writeObject(user);
				objectOutputStream.flush();
				ObjectInputStream objectInputStream = new ObjectInputStream(Util.mySocket.getInputStream());
				user = (UserObject)objectInputStream.readObject();
				Log.d("Jet", "Get something");
				if (user.getStatus() == 1)
				{
				    //startActivity
					Intent intent = new Intent();
					intent.setClass(LoginActivity.this, MainActivity.class);
					startActivity(intent);
					LoginActivity.this.finish();
				}
				else
				{
					Log.d("Jet", "no match");
				}
				objectInputStream.close();
				objectOutputStream.close();
				Util.mySocket.close();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				Log.d("Jet", e.getMessage());
			}
        }
    };
}
