package com.insyde.sharekantan;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Enumeration;
import java.util.List;

import org.apache.http.conn.util.InetAddressUtils;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.DhcpInfo;
import android.net.wifi.WifiManager;
import android.os.IBinder;

public class WifiSyncService extends Service {
    private static final boolean INCLUDE_SYSTEM_APPS = false;
    private boolean isServiceStarted = false;

    @Override
    public void onStart(Intent intent, int startId) {
        if (!isServiceStarted) {
            new Thread(ruServiceBroadcast).start(); // replay devices scan
            new Thread(ruServerSocket).start(); // replay application data
            new Thread(ruSyncApp).start(); // sync application file
            isServiceStarted = true;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private Runnable ruServiceBroadcast = new Runnable() {
        @Override
        public void run() {
            try {
                byte[] serviceData = new byte[1024];
                DatagramSocket svSocket = new DatagramSocket(null);
                svSocket.setBroadcast(true);
                svSocket.setReuseAddress(true);
                svSocket.bind(new InetSocketAddress(WifiSetting.DISCOVERY_PORT));
                while (true) {
                    DatagramPacket svPacket = new DatagramPacket(serviceData, serviceData.length);
                    svSocket.receive(svPacket);
                    String msg = new String(svPacket.getData(), 0, svPacket.getLength());
                    if (msg.equals(WifiSetting.APPSYNC_SCAN) && !svPacket.getAddress().getHostName().equals(getLocalIpAddress())) {
                        String remsg = WifiSetting.APPSYNC_REPLAY + WifiSetting.SPLIT_DATA + android.os.Build.MANUFACTURER + " " + android.os.Build.MODEL;
                        byte[] replayData = remsg.getBytes();
                        DatagramPacket replayPacket = new DatagramPacket(replayData, replayData.length, getBroadcastAddress(), WifiSetting.DISCOVERY_PORT);
                        svSocket.send(replayPacket);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };

    private String getLocalIpAddress() {
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
                NetworkInterface intf = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    String ipv4;
                    if (!inetAddress.isLoopbackAddress() && InetAddressUtils.isIPv4Address(ipv4 = inetAddress.getHostAddress())) {
                        return ipv4;
                    }
                }
            }
        } catch (Exception ex) {
        }
        return null;
    }

    private InetAddress getBroadcastAddress() throws IOException {
        WifiManager wifi = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        DhcpInfo dhcp = wifi.getDhcpInfo();
        int broadcast = (dhcp.ipAddress & dhcp.netmask) | ~dhcp.netmask;
        byte[] quads = new byte[4];
        for (int k = 0; k < 4; k++) {
            quads[k] = (byte) ((broadcast >> k * 8) & 0xFF);
        }
        return InetAddress.getByAddress(quads);
    }

    private Runnable ruServerSocket = new Runnable() {
        @Override
        public void run() {
            ServerSocket serverSocket = null;
            Socket socket = null;
            DataInputStream dataInputStream = null;
            DataOutputStream dataOutputStream = null;

            try {
                serverSocket = new ServerSocket(WifiSetting.CONNECT_PORT);
            } catch (IOException e) {
                e.printStackTrace();
            }

            while (true) {
                try {
                    socket = serverSocket.accept();
                    dataInputStream = new DataInputStream(socket.getInputStream());
                    dataOutputStream = new DataOutputStream(socket.getOutputStream());
                    dataOutputStream.writeUTF(loadInstalledApps(INCLUDE_SYSTEM_APPS));
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
                    if (dataInputStream != null) {
                        try {
                            dataInputStream.close();
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
                }
            }
        }
    };

    private String loadInstalledApps(boolean includeSysApps) {
        PackageManager packageManager = getPackageManager();
        List<PackageInfo> packs = packageManager.getInstalledPackages(0);
        String appInfo = null;
        String appList = "";
        for (int i = 0; i < packs.size(); i++) {
            PackageInfo p = packs.get(i);
            ApplicationInfo a = p.applicationInfo;
            if ((!includeSysApps) && ((a.flags & ApplicationInfo.FLAG_SYSTEM) == 1)) {
                continue;
            }
            if (p.packageName.equals(WifiSetting.IGNORE_APP_DATA)) {
                continue;
            }
            appInfo = p.applicationInfo.loadLabel(packageManager).toString() + WifiSetting.SPLIT_DATA + a.sourceDir + WifiSetting.SPLIT_NEXT;
            appList = appList + appInfo;
        }
        if (appInfo == null) {
            appList = getString(R.string.no_app_found) + WifiSetting.SPLIT_DATA + WifiSetting.IGNORE_PATH + WifiSetting.SPLIT_NEXT;
        }
        return appList;
    }

    private Runnable ruSyncApp = new Runnable() {
        @Override
        public void run() {
            ServerSocket serverSocket = null;
            Socket socket = null;
            DataInputStream dataInputStream = null;
            DataOutputStream dataOutputStream = null;
            FileInputStream fis = null;
            byte[] buffer = new byte[2048];
            int bytesReaded;

            try {
                serverSocket = new ServerSocket(WifiSetting.APPSYNC_PORT);
            } catch (IOException e) {
                e.printStackTrace();
            }

            while (true) {
                try {
                    socket = serverSocket.accept();
                    dataInputStream = new DataInputStream(socket.getInputStream());
                    dataOutputStream = new DataOutputStream(socket.getOutputStream());

                    String msg = dataInputStream.readUTF();
                    String[] tokenPath = msg.split(WifiSetting.SPLIT_DATA);
                    String checkReplay = tokenPath[0];
                    String checkPath = tokenPath[1];
                    if (checkReplay.equals(WifiSetting.CHECKOUT_SYNC)) {
                        fis = new FileInputStream(checkPath);
                        while ((bytesReaded = fis.read(buffer)) > -1 ) {
                            dataOutputStream.write(buffer, 0, bytesReaded);
                            dataOutputStream.flush();
                        }
                    }
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
                    if (dataInputStream != null) {
                        try {
                            dataInputStream.close();
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
                    if (fis != null) {
                        try {
                            fis.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
    };
}