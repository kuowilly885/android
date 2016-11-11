package com.insyde.sharekantan;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.app.Activity;
import android.app.ListActivity;
import android.content.Intent;
import android.util.SparseBooleanArray;
import android.view.Menu;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.AdapterView.OnItemClickListener;

public class AppSyncList extends ListActivity {
//	  private ArrayAdapter appAdapter; 
//	  private String[] appSyncList = new String[] {
//			"Android", "iPhone", "WindowsMobile",
//	        "Blackberry", "WebOS", "Ubuntu", "Windows7", "Max OS X",
//	        "Linux", "OS/2"};
	  
	  /* buttons */
	  private Button btAll;
	  private Button btDeselectAll;
	  private Button btSync;

	  /* app listview */
	  private ListView listView;
	  	  
	  private Thread thGetApp;
	  
	  private String connectAppPath;
	  private String connectAppSend;
	  private File dirRoot;
	  private File dirSave;
	  
	  /* for update list about apps got from a device */
	  private List<String> apkFilePath;
	  
	  public void onCreate(Bundle savedInstanceState) {
		  super.onCreate(savedInstanceState);
		  setContentView(R.layout.app);
		  
	      /* get storage path */
	      dirRoot = new File(Environment.getExternalStorageDirectory() + "/AppSync");
	      dirSave = new File(Environment.getExternalStorageDirectory() + "/AppSync/data/app");
      	  
		  btAll = (Button)findViewById(R.id.selectAllButton);
      	  btDeselectAll = (Button)findViewById(R.id.deleteAllButton);
      	  btSync = (Button)findViewById(R.id.startSyncButton);
      	  
          listView = (ListView) findViewById(R.id.appList);
		  
		  
		   
//	    appAdapter = new ArrayAdapter<String>(this,android.R.layout.simple_list_item_multiple_choice, appSyncList);
//	    setListAdapter(appAdapter);
	  }
	  
	    public void changeMyList() {
	        
	    	runOnUiThread(ruUpdateMyList);

	    }
	  
	    private Runnable UpdateListApplication = new Runnable() {
	        @Override
	        public void run() {

	            ArrayAdapter<String> adapter = new ArrayAdapter<String>(getApplicationContext(), android.R.layout.simple_list_item_multiple_choice, WifiSetting.listItem);
	            listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
	            listView.setAdapter(adapter);
	            listView.setOnItemClickListener(new OnItemClickListener() {
	                public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
	                }
	            });

	            btAll.setOnClickListener(new View.OnClickListener() {
	                public void onClick(View v) {
	                    for (int i = 0; i < listView.getCount(); i++) {
	                        listView.setItemChecked(i, true);
	                    }
	                }
	            });

	            btDeselectAll.setOnClickListener(new View.OnClickListener() {
	                public void onClick(View v) {
	                    for (int i = 0; i < listView.getCount(); i++) {
	                        listView.setItemChecked(i, false);
	                    }
	                }
	            });

	            btSync.setOnClickListener(new View.OnClickListener() {
	                public void onClick(View v) {
	                    boolean isEmpty = true;
	                    int cntChoice = listView.getCount();
	                    SparseBooleanArray sparseBooleanArray = listView.getCheckedItemPositions();
	                    for (int i = 0; i < cntChoice; i++) {
	                        if (sparseBooleanArray.get(i)) {
	                            if (!WifiSetting.appPath.get(i).equals(WifiSetting.IGNORE_PATH)) {
	                                isEmpty = false;
	                            }
	                        }
	                    }
	                    if (!isEmpty) {
	                        thGetApp = new Thread(GetApplication);
	                        thGetApp.start();
	                    }
	                }
	            });
	        }
	    };
	    
	    private Runnable GetApplication = new Runnable() {
	        @Override
	        public void run() {
//	            handlerUI.post(ruProgressDialogShow);

	            int cntChoice = listView.getCount();
	            SparseBooleanArray sparseBooleanArray = listView.getCheckedItemPositions();
	            for (int i = 0; i < cntChoice; i++) {
	                if (sparseBooleanArray.get(i)) {
	                    connectAppPath = WifiSetting.appPath.get(i);
	                    connectAppSend = WifiSetting.CHECKOUT_SYNC + WifiSetting.SPLIT_DATA + WifiSetting.appPath.get(i);
	                    if (!connectAppPath.equals(getString(R.string.no_app_found))) {
	                        Socket socket = null;
	                        BufferedInputStream bis = null;
	                        DataInputStream dis = null;
	                        DataOutputStream dos = null;

	                        File writtenFile = null;
	                        FileOutputStream fos = null;
	                        BufferedOutputStream bos = null;

	                        if (!dirSave.isDirectory()) {
	                            dirSave.mkdirs();
	                        }

	                        try {
	                            socket = new Socket(WifiSetting.connectDevicesPath, WifiSetting.APPSYNC_PORT);
	                            dos = new DataOutputStream(socket.getOutputStream());
	                            dos.writeUTF(connectAppSend);

	                            bis = new BufferedInputStream(socket.getInputStream());
	                            dis = new DataInputStream(bis);

	                            writtenFile = new File(dirRoot + connectAppPath);
	                            fos = new FileOutputStream(writtenFile);
	                            bos = new BufferedOutputStream(fos);

	                            byte[] buffer = new byte[2048];
	                            int bytesReaded;

	                            while ((bytesReaded = dis.read(buffer)) > -1) {
	                                bos.write(buffer, 0, bytesReaded);
	                                bos.flush();
	                            }
	                            //SYNC_RESULT = true;
	                        } catch (UnknownHostException e) {
	                            //SYNC_RESULT = false;
	                            e.printStackTrace();
	                        } catch (IOException e) {
	                            //SYNC_RESULT = false;
	                            e.printStackTrace();
	                        } finally {
	                            if (bos != null) {
	                                try {
	                                    bos.close();
	                                } catch (IOException e) {
	                                    e.printStackTrace();
	                                }
	                            }
	                            if (fos != null) {
	                                try {
	                                    fos.close();
	                                } catch (IOException e) {
	                                    e.printStackTrace();
	                                }
	                            }
	                            if (dis != null) {
	                                try {
	                                    dis.close();
	                                } catch (IOException e) {
	                                    e.printStackTrace();
	                                }
	                            }
	                            if (bis != null) {
	                                try {
	                                    bis.close();
	                                } catch (IOException e) {
	                                    e.printStackTrace();
	                                }
	                            }
	                        }
	                    }
	                }
	            }

	            // Hide API, need source code.
//	            if (THIS_APP_INSTALL_IN_THE_SYSTEM) {
	/*
	                for (int i = 0; i < cntChoice; i++) {
	                    if (sparseBooleanArray.get(i)) {
	                        connectAppPath = appPath.get(i);
	                        Uri mPackageURI = Uri.fromFile(new File(dirRoot + connectAppPath));
	                        int installFlags = 0;
	                        PackageManager pm = getPackageManager();
	                        try {
	                            PackageInfo pi = pm.getPackageInfo(getPackageName(), PackageManager.GET_UNINSTALLED_PACKAGES);
	                            if (pi != null) {
	                                installFlags |= PackageManager.INSTALL_REPLACE_EXISTING;
	                            }
	                        } catch (NameNotFoundException e) {
	                        }

	                        IPackageInstallObserver observer = new IPackageInstallObserver() {
	                            @Override
	                            public void packageInstalled(String packageName, int returnCode) throws RemoteException {
	                            }
	                            @Override
	                            public IBinder asBinder() {
	                                return null;
	                            }
	                        };
	                        pm.installPackage(mPackageURI, observer, installFlags, "");
	                    }
	                }
	*/
//	           }

//	            if (!THIS_APP_INSTALL_IN_THE_SYSTEM) {
//	                changeMyList();
//	            }
	            
	              changeMyList();

//	            handlerUI.post(ruProgressDialogClose);
//	            handlerUI.post(ruToast);
	        }
	    };
	    
	    private Runnable ruUpdateMyList = new Runnable() {
	        @Override
	        public void run() {
	            btAll.setVisibility(View.GONE);
	            btDeselectAll.setVisibility(View.GONE);
	            btSync.setVisibility(View.GONE);
	            setTitle("Apps I got");

	            List<String> apkFileName = new ArrayList<String>();
	            apkFilePath = new ArrayList<String>();
	            File[] files = dirSave.listFiles();
	            if (files != null) {
	                for (File file : files) {
	                    if(file.getName().endsWith(".apk")){
	                        apkFileName.add(file.getName());
	                        apkFilePath.add(file.toString());
	                    }
	                }
	            }
	            if (apkFileName.size() == 0) {
	                apkFileName.add("No application file");
	            }

	            ArrayAdapter<String> adapter = new ArrayAdapter<String>(getApplicationContext(), android.R.layout.simple_list_item_1, apkFileName);
	            listView.setAdapter(adapter);
	            listView.setOnItemClickListener(new OnItemClickListener() {
	                public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
	                    if (apkFilePath.size() != 0) {
	                        Uri uri = Uri.fromFile(new File(apkFilePath.get(position)));
	                        Intent intent =new Intent(Intent.ACTION_VIEW);
	                        intent.setDataAndType(uri, "application/vnd.android.package-archive");
	                        startActivity(intent);
	                    }
	                }
	            });
	        }
	    };
} 