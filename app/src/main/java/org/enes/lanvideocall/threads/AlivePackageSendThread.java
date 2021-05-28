package org.enes.lanvideocall.threads;

import android.util.Log;

import com.google.gson.Gson;

import org.enes.lanvideocall.pojos.CallPOJO;
import org.enes.lanvideocall.utils.Defines;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;

public class AlivePackageSendThread extends MyThread {

    public String ip;

    public String check_str;

    public AlivePackageSendThread(String ip, String check_str) {
        super();
        this.ip = ip;
        this.check_str = check_str;
    }

    @Override
    public void run() {
        CallPOJO callPOJO = new CallPOJO();
        callPOJO.check_str = check_str;
        callPOJO.id = Defines.CALL_BROADCAST_SEND_ALIVE_PACKAGE;
        callPOJO.type = Defines.CALL_JSON_TYPE_REQ;
        Gson gson = new Gson();
        String json = gson.toJson(callPOJO);
        byte[] send_byte = json.getBytes();
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
        while (!isInterrupted() && datagramSocket != null) {
            // stop when time out
            long now_time_long = System.currentTimeMillis();
            if(now_time_long - last_sent_time > Defines.SEND_ALIVE_PACKAGE_INTERVAL) {
                //
                try {
                    datagramSocket.send(datagramPacket);
                } catch (IOException e) {
                    e.printStackTrace();
                }
//                if(threadInterface != null) {
//                    threadInterface.onPulseSent();
//                }
                Log.e("udpThread","send alive package");
                last_sent_time = now_time_long;
            }
        }
        if(datagramSocket != null) {
            datagramSocket.close();
        }
    }
}
