package org.enes.lanvideocall.threads;


import com.google.gson.Gson;

import org.enes.lanvideocall.pojos.CallPOJO;
import org.enes.lanvideocall.utils.Defines;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.util.UUID;

public class DialThread extends MyThread {

    public interface DialThreadInterface {

        void onDialSent(String check_str);

    }

    private DialThreadInterface dialThreadInterface;

    public void setDialThreadInterface(DialThreadInterface new_one) {
        dialThreadInterface = new_one;
    }

    private String ip;

    private String check_str;

    public DialThread(String ip) {
        super();
        this.ip = ip;
    }

    @Override
    public void run() {
        if(!isInterrupted()) {
            try {
                InetSocketAddress inetSocketAddress =
                        new InetSocketAddress(ip, Defines.CONTROL_SERVER_PORT);
                DatagramSocket datagramSocket = new DatagramSocket();

                CallPOJO callPOJO = new CallPOJO();
                callPOJO.id = Defines.CALL_BROADCAST_DIALING_NOW;
                callPOJO.type = Defines.CALL_JSON_TYPE_REQ;
                callPOJO.check_str = UUID.randomUUID().toString();
                check_str = callPOJO.check_str;

                byte[] data = new Gson().toJson(callPOJO).getBytes();
                DatagramPacket datagramPacket  =
                        new DatagramPacket(data, data.length, inetSocketAddress);
                datagramSocket.send(datagramPacket);
                datagramSocket.close();
            } catch (SocketException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            if(dialThreadInterface != null) {
                dialThreadInterface.onDialSent(check_str);
            }
        }
    }
}
