package org.enes.lanvideocall.threads;

import android.util.Log;

import com.google.gson.Gson;

import org.enes.lanvideocall.pojos.CallReturnPOJO;
import org.enes.lanvideocall.utils.Defines;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;

public class SendAcceptOrIgnoreThread extends Thread {

    private String ip;

    private String check_str;

    private boolean isAccept;

    public SendAcceptOrIgnoreThread(String ip,String check_str, boolean isAccept) {
        super();
        this.ip = ip;
        this.check_str = check_str;
        this.isAccept = isAccept;
    }

    @Override
    public void run() {
        if(!isInterrupted()) {
            CallReturnPOJO callReturnPOJO = new CallReturnPOJO();
            callReturnPOJO.type = Defines.CALL_JSON_TYPE_RESP;
            callReturnPOJO.check_str = check_str;
            callReturnPOJO.id = Defines.CALL_BROADCAST_RINGING_NOW;
            if(isAccept) {
                callReturnPOJO.resp = Defines.CALL_OTHER_ONE_ACCEPT;
            }else {
                callReturnPOJO.resp = Defines.CALL_OTHER_ONE_IGNORE;
            }
            Gson gson = new Gson();
            String json = gson.toJson(callReturnPOJO);
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
        }
    }

}
