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

public class EndCallThread extends MyThread {

    private String ip;

    private String now_check_str;

    public EndCallThread(String ip, String now_check_str) {
        super();
        this.ip = ip;
        this.now_check_str = now_check_str;
    }

    @Override
    public void run() {
        CallPOJO callPOJO = new CallPOJO();
        callPOJO.type = Defines.CALL_JSON_TYPE_REQ;
        callPOJO.id = Defines.CALL_BROADCAST_CLOSE;
        callPOJO.check_str = now_check_str;
        Gson gson = new Gson();
        String json = gson.toJson(callPOJO);
        byte[] data = json.getBytes();
        InetSocketAddress inetSocketAddress =
                new InetSocketAddress(ip, Defines.CONTROL_SERVER_PORT);
        try {
            DatagramSocket datagramSocket = new DatagramSocket();
            DatagramPacket datagramPacket  =
                    new DatagramPacket(data, data.length, inetSocketAddress);
            datagramSocket.send(datagramPacket);
            datagramSocket.close();
        } catch (SocketException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Log.e("test","這邊發起的掛斷:"+json);
    }
}
