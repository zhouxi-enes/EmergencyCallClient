package org.enes.lanvideocall.threads;

import android.content.SharedPreferences;
import android.util.Log;

import com.google.gson.Gson;

import org.enes.lanvideocall.pojos.RingingPOJO;
import org.enes.lanvideocall.utils.Defines;
import org.enes.lanvideocall.utils.SharedPreferencesUtil;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;

public class RingingPulseThread extends MyThread {

    public interface RingingPulseThreadInterface {

        void onPulseSent();

        void onPulseThreadFinished();

    }

    private RingingPulseThreadInterface threadInterface;

    public void setRingingPulseThreadInterface(RingingPulseThreadInterface new_) {
        threadInterface = new_;
    }

    private String ip;

    private String check_str;

    private int call_method;

    public RingingPulseThread(String ip, String check_str, int call_method) {
        super();
        this.ip = ip;
        this.check_str = check_str;
        this.call_method = call_method;
    }

    @Override
    public void run() {
        long start_time_long = System.currentTimeMillis();
        boolean is_stop = false;
        Gson gson = new Gson();
        RingingPOJO ringingPOJO = new RingingPOJO();
        ringingPOJO.check_str = check_str;
        ringingPOJO.type = Defines.CALL_JSON_TYPE_REQ;
        ringingPOJO.id = Defines.CALL_BROADCAST_RINGING_NOW;
        ringingPOJO.call_method = call_method;
        SharedPreferences sharedPreferences = SharedPreferencesUtil.getSharedPreferences();
        ringingPOJO.name = sharedPreferences.getString(SharedPreferencesUtil.KEY_NAME,null);
        ringingPOJO.uuid = sharedPreferences.getString(SharedPreferencesUtil.KEY_UUID,null);
        //
        String send_str = gson.toJson(ringingPOJO);
        byte[] send_byte = send_str.getBytes();
        InetSocketAddress inetSocketAddress =
                new InetSocketAddress(ip, Defines.CONTROL_SERVER_PORT);
        DatagramPacket datagramPacket = null;
        DatagramSocket datagramSocket = null;
        try {
            datagramSocket = new DatagramSocket();
            datagramPacket  =
                    new DatagramPacket(send_byte, send_byte.length, inetSocketAddress);
        } catch (SocketException e) {
            e.printStackTrace();
        }
        long last_sent_time = 0;
        while (!is_stop && !isInterrupted() && datagramSocket != null) {
            // stop when time out
            long now_time_long = System.currentTimeMillis();
            if(now_time_long - start_time_long >= Defines.SEND_RINGING_PULSE_TIME) {
                is_stop = true;
            }
//            Log.e("send ringing pulse",send_str);
            if(now_time_long - last_sent_time > Defines.SEND_RINGING_PULSE_INTERVAL) {
                //
                try {
                    datagramSocket.send(datagramPacket);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                if(threadInterface != null) {
                    threadInterface.onPulseSent();
                }
                last_sent_time = now_time_long;
            }
        }
        if(datagramSocket != null) {
            datagramSocket.close();
        }
        if(threadInterface != null) {
            if(!isInterrupted())
                threadInterface.onPulseThreadFinished();
        }
    }
}
