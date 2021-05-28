package org.enes.lanvideocall.utils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.provider.Settings;
import android.util.Log;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class NetworkUtil {

    public static String getMyIP(Context context) {
        String my_ip = "";
        WifiManager wm = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        int address = wm.getConnectionInfo().getIpAddress();
        String ip = null;
        try {
            ip = InetAddress.getByAddress(
                    ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(address).array())
                    .getHostAddress();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        if(ip != null) {
            my_ip = ip;
        }
        return my_ip;
    }

    public static boolean isConnectToWifi(Context context) {
        boolean isConnectToWifi = false;
        WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        if(wifiManager.isWifiEnabled()) {
            WifiInfo wifiInfo = wifiManager.getConnectionInfo();
            if(wifiInfo.getNetworkId() != -1) {
                isConnectToWifi = true;
            }
        }
        return isConnectToWifi;
    }

    public static final int REQUEST_CODE_OPEN_WIFI_SETTINGS = 0 << 1;

    public static void openWifiSettings(Context context) {
        Intent intent = new Intent(Settings.ACTION_WIFI_SETTINGS);
        try{
            if(context instanceof Activity) {
                Activity activity = (Activity) context;
                activity.startActivityForResult(intent,REQUEST_CODE_OPEN_WIFI_SETTINGS);
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }

}
