package com.insyde.sharekantan;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import android.net.ConnectivityManager;
import android.net.DhcpInfo;
import android.net.NetworkInfo;
import android.net.NetworkInfo.State;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiConfiguration.KeyMgmt;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.app.TabActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.util.SparseBooleanArray;
import android.view.Menu;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TabHost;
import android.widget.TabWidget;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;

public class WifiSetting extends Activity {
	
	/* sync parameters */
    public static final int DISCOVERY_PORT = 57132;
    public static final int CONNECT_PORT = 57133;
    public static final int APPSYNC_PORT = 57134;
    public static final String APPSYNC_SCAN = "APP_SYNC_SCAN_FOR_DEVICES";
    public static final String CHECKOUT_SYNC = "APP_SYNC_CHECKOUT_SYNC";
    public static final String APPSYNC_REPLAY = "APP_SYNC_DEVICES_REPLAY";
    public static final int TIMEOUT_MS = 1500;//mili second
    public static final String SPLIT_DATA = ",,,";
    public static final String SPLIT_NEXT = ";;;";
    public static final String IGNORE_APP_DATA = "com.insyde.wifisync";
    public static final String IGNORE_PATH = "com.insyde.wifisync.nofound";
    
    
    /* devices listview */
    private ListView listView;    
    
    /* buttons */
	private Button Connect;
	private Button Refresh;
    
	/* net management */
	private static NetworkInfo nwifi;
	private static ConnectivityManager conMan;
	private static WifiManager wiFiManager;
	
	/* thread for checking out network status */
	private Object thUiSave = new Object();
	private Thread thCheckNetwork;
	private int CheckNetSt;
	
	/* thread for find devices */
	private Object thDevFindSave =new Object();
	private Thread thDevFind;
    private ArrayList<Item> listDevices;
    public static List<String> listItem;
    
    /* for update devices list */
    public static String connectDevicesPath;
    
    /* thread for connecting a device to get app list */
    private Object thConnectDevSave = new Object();
    private Thread thConnectDev;
    public static List<String> appPath;
	

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.wifisetting);
		
    	/* start service */
        Intent startIntent = new Intent(this,WifiSyncService.class);
        startService(startIntent);
		
		
        /* initialize net management */
    	conMan=(ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
    	wiFiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);                    	
    	nwifi = conMan.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
    	
    	/* find buttons */
    	Connect = (Button)findViewById(R.id.connectButton);
    	Refresh = (Button)findViewById(R.id.refreshButton);
    	
        listView = (ListView) findViewById(R.id.listview);
    	
    	
		
	}
	
	
	
    @Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
    	/* start to check net connection */
    	thCheckNetwork = new Thread(checkNetworkConnect);
    	thCheckNetwork.start();
		
	}



	private int checkNetworkState() {
    	
    	int netType=0;
    	State wifi=conMan.getNetworkInfo(ConnectivityManager.TYPE_WIFI).getState();


    	if (wifi==State.CONNECTED||wifi==State.CONNECTING)
    	{
    	netType=1;
    	}
    	
    	return netType;
    	
    }
    
    private InetAddress getBroadcastIpAddress() throws IOException {
    	
        DhcpInfo dhcp = wiFiManager.getDhcpInfo();
        int broadcast = (dhcp.ipAddress & dhcp.netmask) | ~dhcp.netmask;
        byte[] quads = new byte[4];
        
        for (int k = 0; k < 4; k++) 
        {
            quads[k] = (byte) ((broadcast >> k * 8) & 0xFF);
        }
        
        return InetAddress.getByAddress(quads);
        
    }
    
    private void changeListDevices() {
        
    	runOnUiThread(updateListDevices);

    }
    
    public void changeListApp() {
        
    	runOnUiThread(UpdateListApplication);

    }
    
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    	
        /* Tracking wifi connection state */
    	State swifi=conMan.getNetworkInfo(ConnectivityManager.TYPE_WIFI).getState();
        
    	if (swifi==State.CONNECTED||swifi==State.CONNECTING)
    	{
    		thDevFind = new Thread(findDev);
    		thDevFind.start();
    	}
    	else
    	{
    		 runOnUiThread(new Runnable() {
                 public void run()
                 {
                   synchronized (thUiSave) {
          	    	 Toast.makeText(WifiSetting.this, "Sorry, WIFI connecting failed."
        	    			 ,Toast.LENGTH_LONG).show();
                   }
                 }
             });
    	}
    }
    
    private void splitList(String appList) {
        listItem = new ArrayList<String>();
        appPath = new ArrayList<String>();
        String[] tokenList = appList.split(SPLIT_NEXT);
        for (int i = 0; i < tokenList.length; i++) {
            String appData = tokenList[i];
            String[] tokenApp = appData.split(SPLIT_DATA);
            listItem.add(tokenApp[0]);
            appPath.add(tokenApp[1]);
        }
    }
    
    private Runnable checkNetworkConnect = new Runnable() {
        @Override
        public void run() {
            CheckNetSt = checkNetworkState();
            
            if(CheckNetSt == 0)
            {
                                      	  
//                     if(!wiFiManager.isWifiEnabled())
//                     {         	                      		                       	
//                           wiFiManager.setWifiEnabled(true);
//                     }
                               
//                     /* Trying to link a fixed wifi field (no password) */ 
//                     WifiConfiguration wifiConfig = new WifiConfiguration();
//                                
//                     wifiConfig.SSID =  "\""+ "SomeSSID" +"\"";
//                                
//                     wifiConfig.status =WifiConfiguration.Status.ENABLED;
//                                
//                     wifiConfig.allowedKeyManagement.set(KeyMgmt.NONE);
//                                
//                                int netId =wiFiManager.addNetwork(wifiConfig);
//                                
//                                boolean success = wiFiManager.enableNetwork(netId, true);
                     
                     /* Dialog for wifi turnning on */
                     runOnUiThread(new Runnable() {
                         public void run()
                         {
                           synchronized (thUiSave) {
                               new AlertDialog.Builder(WifiSetting.this).setTitle("WIFI is not enabled")
                               .setMessage("Would you like to set wifi manually?")
                               .setPositiveButton
                               (
                                 "Yes", new DialogInterface.OnClickListener()
                                 {
                                   public void onClick(DialogInterface dialoginterface, int i)
                                   {
                                     Intent settingsIntent = new
                                     Intent(android.provider.Settings.ACTION_WIFI_SETTINGS);
                                     startActivityForResult(settingsIntent,1);
                                   }
                                 }
                               ).setNegativeButton
                               (
                                 "No", new DialogInterface.OnClickListener()
                                 {
                                     public void onClick(DialogInterface dialoginterface, int i)
                                     {
                              	    	 Toast.makeText(WifiSetting.this, "Sorry, you can't sync apps due to disconnecting."
                            	    			 ,Toast.LENGTH_LONG).show();
                                     }
                                 }
                               ).show();
                         	
                           }
                         }
                     });

            	
       
            }
            else if(CheckNetSt == 1)
            {
                runOnUiThread(new Runnable() {
                    public void run()
                    {
                      synchronized (thUiSave) {
                    	  
                          /* Tracking wifi connection state */
                      	State swifi=conMan.getNetworkInfo(ConnectivityManager.TYPE_WIFI).getState();
                          
                      	if (swifi==State.CONNECTED||swifi==State.CONNECTING)
                      	{
                      		thDevFind = new Thread(findDev);
                      		thDevFind.start();
                      	}
                      	else
                      	{
                      		 runOnUiThread(new Runnable() {
                                   public void run()
                                   {
                                     synchronized (thUiSave) {
                            	    	 Toast.makeText(WifiSetting.this, "Sorry, WIFI connecting failed."
                          	    			 ,Toast.LENGTH_LONG).show();
                                     }
                                   }
                               });
                      	}
                    	
                      }
                    }
                });
            }

        }
    };
    
    private Runnable findDev = new Runnable() {
        @Override
        public void run() {
            synchronized (thDevFindSave) {
            	
                DatagramSocket seSocket = null;
                DatagramSocket reSocket = null;
                
                listItem = new ArrayList<String>();     //for showing finded device
                listDevices = new ArrayList<Item>();    //for showing finded device
                
                
                try {
                    byte[] sendData = APPSYNC_SCAN.getBytes();
                    seSocket = new DatagramSocket(null);
                    seSocket.setBroadcast(true);
                    seSocket.setReuseAddress(true);
                    seSocket.bind(new InetSocketAddress(DISCOVERY_PORT));
                    DatagramPacket sePacket = new DatagramPacket(sendData, sendData.length, getBroadcastIpAddress(), DISCOVERY_PORT);
                    seSocket.send(sePacket);

                    byte[] receiveData = new byte[1024];
                    reSocket = new DatagramSocket(null);
                    reSocket.setBroadcast(true);
                    reSocket.setReuseAddress(true);
                    reSocket.bind(new InetSocketAddress(DISCOVERY_PORT));
                    reSocket.setSoTimeout(/*TIMEOUT_MS*/5000);
                    while (true) {
                        DatagramPacket rePacket = new DatagramPacket(receiveData, receiveData.length);
                        reSocket.receive(rePacket);
                        String msg = new String(rePacket.getData(), 0, rePacket.getLength());
                        String[] tokenDevices = msg.split(SPLIT_DATA);
                        String checkReplay = tokenDevices[0];
                        String checkPath = "";
                        if (tokenDevices.length == 2) {
                            checkPath = tokenDevices[1];
                        }
                        if (checkReplay.equals(APPSYNC_REPLAY)) {
                            listItem.add(rePacket.getAddress().getHostName());
                            listDevices.add(new Item(checkPath, rePacket.getAddress().getHostName()));
                        }
                    }
                } catch (SocketTimeoutException e) {
                } catch (SocketException e) {
                } catch (IOException e) {
                } finally {
                    if (listItem.size() == 0) {
                        listItem.add(getString(R.string.no_devices_found));
                        listDevices.add(new Item(getString(R.string.no_devices_found), ""));
                    }
                    seSocket.close();
                    reSocket.close();
                    changeListDevices();
                }
            		
            }
        }
    };
    private Runnable updateListDevices = new Runnable() {
    	
    	public void run() {
  		      	
            setTitle("Scan for devices");
            
            ItemAdapter adapter = new ItemAdapter(getApplicationContext(), listDevices);
            listView.setAdapter(adapter);
            
            listView.setOnItemClickListener(new OnItemClickListener() {
                public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
                    connectDevicesPath = listItem.get(position);
                    if (!connectDevicesPath.equals(getString(R.string.no_devices_found))) {
                        setTitle(connectDevicesPath);
                        thConnectDev = new Thread(devConnect);
                        thConnectDev.start();
                    }
                    else
                    {
//                            Refresh.setOnClickListener(new View.OnClickListener() {
//                            public void onClick(View v) {
//                                 new Thread(findDev).start();
//                            }
//                        });
                    
                    }
                }
            });
           
    	}
    };
    
    private Runnable devConnect = new Runnable() {
    	
    	public void run() {
    		
    	  synchronized(thConnectDevSave){
    		
    		Socket socket = null;
            DataInputStream dataInputStream = null;
            DataOutputStream dataOutputStream = null;

            try {
                socket = new Socket(connectDevicesPath, CONNECT_PORT);
                dataInputStream = new DataInputStream(socket.getInputStream());
                dataOutputStream = new DataOutputStream(socket.getOutputStream());
                splitList(dataInputStream.readUTF());
                changeListApp();
            } catch (UnknownHostException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (socket != null) {
                    try {
                        socket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                if (dataOutputStream != null) {
                    try {
                        dataOutputStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                if (dataInputStream != null) {
                    try {
                        dataInputStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
    	  }
    	}
    };
    
    private Runnable UpdateListApplication = new Runnable() {
        @Override
        public void run() {

        	Connect.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                	Intent intent = new Intent(WifiSetting.this, AppSyncList.class);
                	Bundle bundle = new Bundle();
                	startActivity(intent);
                }
            });

        }
    };
}
