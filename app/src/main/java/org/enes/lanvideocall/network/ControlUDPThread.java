package org.enes.lanvideocall.network;

import android.text.Selection;
import android.util.Log;

import com.google.gson.Gson;

import org.enes.lanvideocall.application.MyApplication;
import org.enes.lanvideocall.pojos.CallPOJO;
import org.enes.lanvideocall.pojos.CallReturnPOJO;
import org.enes.lanvideocall.pojos.RingingPOJO;
import org.enes.lanvideocall.pojos.User;
import org.enes.lanvideocall.threads.MyThread;
import org.enes.lanvideocall.utils.Defines;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Iterator;
import java.util.Timer;
import java.util.TimerTask;

public class ControlUDPThread extends Thread {

    private static final String TAG = ControlUDPThread.class.getName();

    private DatagramChannel datagramChannel;

    private Selector selector;

    private Timer check_calling_pulse_timer;

    private TimerTask check_calling_pulse_timer_task;

    public void closeCallingPulseTimerAndTask() {
        if(check_calling_pulse_timer != null) {
            check_calling_pulse_timer.cancel();
            check_calling_pulse_timer = null;
        }
        if(check_calling_pulse_timer_task != null) {
            check_calling_pulse_timer_task.cancel();
            check_calling_pulse_timer_task = null;
        }
    }

    public ControlUDPThread() {
        super();
    }

    @Override
    public void run() {
        Log.e(TAG, "start Thread");
        if(!isInterrupted()) {
            try {
                datagramChannel = DatagramChannel.open();
                datagramChannel.configureBlocking(false);
                datagramChannel.socket().bind(new InetSocketAddress(Defines.CONTROL_SERVER_PORT));
                selector = Selector.open();
                datagramChannel.register(selector, SelectionKey.OP_READ);
            } catch (IOException e) {
                e.printStackTrace();
            }
            ByteBuffer byteBuffer = ByteBuffer.allocate(65536);
            while (!isInterrupted()) {
                try {
                    int number = selector.select();
                    if(number > 0) {
                        Iterator iterator = selector.selectedKeys().iterator();
                        while (iterator.hasNext()) {
                            SelectionKey selectionKey = (SelectionKey) iterator.next();
                            iterator.remove();
                            if(selectionKey.isReadable()) {
                                DatagramChannel datagramChannel =
                                        (DatagramChannel) selectionKey.channel();
                                byteBuffer.clear();
                                InetSocketAddress address =
                                        (InetSocketAddress) datagramChannel.receive(byteBuffer);
                                byteBuffer.flip();
                                // handle
                                String str = new
                                        String(byteBuffer.array(),0,byteBuffer.remaining());
                                Log.e("udpThread","\n"+str+",\n"+address.getHostString()+",\n"+address.getPort());

                                if(!str.isEmpty()) {
                                    int type = Defines.CALL_JSON_TYPE_REQ;
                                    try {
                                        JSONObject jsonObject = new JSONObject(str);
                                        type = jsonObject.getInt("type");
                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    }
                                    Gson gson = new Gson();
                                    String check_str = null;
                                    if(type == Defines.CALL_JSON_TYPE_REQ) {
                                        CallPOJO callPOJO =  gson.fromJson(str,CallPOJO.class);
                                        check_str = callPOJO.check_str;
                                        int id = callPOJO.id;
                                        if(id == Defines.CALL_BROADCAST_DIALING_NOW) {
//                                            Log.e("test","testa");
                                            CallReturnPOJO callReturnPOJO = new CallReturnPOJO();
                                            callReturnPOJO.type = Defines.CALL_JSON_TYPE_RESP;
                                            callReturnPOJO.id = id;
                                            callReturnPOJO.check_str = check_str;
                                            if(!MyApplication.getInstance().isCallingNow()) {
                                                callReturnPOJO.resp =
                                                        Defines.CALL_ACCEPT_CALLING_NOW;
                                            }else {
                                                callReturnPOJO.resp =
                                                        Defines.CALL_REJECT_CALLING_NOW;
                                            }
                                            InetSocketAddress sendOutAddress =
                                            new InetSocketAddress(address.getHostName(),
                                                    Defines.CONTROL_SERVER_PORT);
                                            byte[] out_bytes =
                                                    gson.toJson(callReturnPOJO).getBytes();
                                            ByteBuffer outByteBuffer = ByteBuffer
                                                    .allocate(out_bytes.length);
                                            outByteBuffer.put(out_bytes);
                                            outByteBuffer.flip();
                                            datagramChannel.send(outByteBuffer,sendOutAddress);
                                        }else if(id == Defines.CALL_BROADCAST_RINGING_NOW) {
                                            //接到響鈴信號
                                            Log.e("udpThread","接到響鈴信號");
                                            MyApplication.getInstance().
                                                    setCallingStatus(Defines.CALL_BROADCAST_RINGING_NOW);
                                            //設置check str
                                            MyApplication.getInstance().setNowCheckStr(check_str);
                                            //補上user信息
                                            if(MyApplication.getInstance().getNowCallingUser() == null) {
                                                User user = new User();
                                                MyApplication.getInstance().setNowCallingUser(user);
                                            }
                                            User user = MyApplication.getInstance().getNowCallingUser();
                                            RingingPOJO pojo = gson.fromJson(str,RingingPOJO.class);
                                            user.ip = address.getHostString();
                                            user.name = pojo.name;
                                            user.uuid = pojo.uuid;
                                            //需要打開 activity
                                            MyApplication.getInstance().onPulseReceived(user.name,
                                                    user.uuid,pojo.call_method,user.ip);
                                            //設定定時器
                                            closeCallingPulseTimerAndTask();
                                            check_calling_pulse_timer = new Timer();
                                            check_calling_pulse_timer_task = new TimerTask() {
                                                @Override
                                                public void run() {
                                                    MyApplication.getInstance().onPulseTimeout();
                                                    closeCallingPulseTimerAndTask();
                                                }
                                            };
                                            check_calling_pulse_timer.
                                                    schedule(check_calling_pulse_timer_task,
                                                            Defines.CHECK_RECEIVED_RINGING_PULSE_INTERVAL);
                                        }else if(id == Defines.CALL_BROADCAST_CLOSE) {
                                            //對方按了掛電話
                                            if(MyApplication.getInstance().getNowCallingStatus() >=
                                                Defines.CALL_BROADCAST_DIALING_NOW &&
                                                MyApplication.getInstance().isCallingNow()) {
                                                closeCallingPulseTimerAndTask();
                                                MyApplication.getInstance().onOtherSideEndCall(check_str);
                                            }
                                        }else if(id == Defines.CALL_BROADCAST_SEND_ALIVE_PACKAGE) {
                                            if(MyApplication.getInstance().getNowCallingStatus() ==
                                                Defines.CALL_BROADCAST_CONNECTED_NOW) {
                                            }
                                            if(check_str.equals(MyApplication.getInstance().getNowCheckStr())) {
                                                Log.e("udpThread","收到心跳包");
                                                //設定定時器
                                                closeCallingPulseTimerAndTask();
                                                check_calling_pulse_timer = new Timer();
                                                check_calling_pulse_timer_task = new TimerTask() {
                                                    @Override
                                                    public void run() {
                                                        MyApplication.getInstance().onAlivePackageTimeout();
                                                        closeCallingPulseTimerAndTask();
                                                    }
                                                };
                                                check_calling_pulse_timer.
                                                        schedule(check_calling_pulse_timer_task,
                                                                Defines.CHECK_RECEIVED_ALIVE_PACKAGE_INTERVAL);
                                                //給對方發個包
                                                CallReturnPOJO callPOJOr = new CallReturnPOJO();
                                                callPOJOr.check_str = check_str;
                                                callPOJOr.id = Defines.CALL_BROADCAST_SEND_ALIVE_PACKAGE;
                                                callPOJOr.type = Defines.CALL_JSON_TYPE_RESP;
                                                gson = new Gson();
                                                String json = gson.toJson(callPOJOr);
                                                byte[] send_byte = json.getBytes();
                                                ByteBuffer outByteBuffer = ByteBuffer
                                                        .allocate(send_byte.length);
                                                outByteBuffer.put(send_byte);
                                                outByteBuffer.flip();
                                                InetSocketAddress sendOutAddress =
                                                        new InetSocketAddress(address.getHostName(),
                                                                Defines.CONTROL_SERVER_PORT);
                                                datagramChannel.send(outByteBuffer,sendOutAddress);
                                            }
                                        }
                                    }else if (type == Defines.CALL_JSON_TYPE_RESP) {
                                        CallReturnPOJO callReturnPOJO =
                                                gson.fromJson(str,CallReturnPOJO.class);
                                        check_str = callReturnPOJO.check_str;
                                        int resp = callReturnPOJO.resp;
                                        //能應答
                                        if(callReturnPOJO.id == Defines.CALL_BROADCAST_DIALING_NOW) {
                                            MyApplication.getInstance().
                                                    onDialingResponseReceived(check_str,resp);
                                        }else if(callReturnPOJO.id == Defines.CALL_BROADCAST_RINGING_NOW) {
                                            // 如果對方按了接聽或者按了不同意
                                            if(MyApplication.getInstance().isCallingNow()) {

                                                String now_check_str =
                                                        MyApplication.getInstance().getNowCheckStr();
                                                if(check_str.equals(now_check_str)) {
                                                    //說明是這次的session
                                                    closeCallingPulseTimerAndTask();
                                                    if(resp == Defines.CALL_OTHER_ONE_IGNORE) {
                                                        //對方不想接
                                                        MyApplication.getInstance().
                                                                sendBroadcast(Defines.CALL_BROADCAST_BUSY);
                                                        Log.e("test","對方不想接");
                                                        MyApplication.getInstance().stopCalling();
                                                    }else if(resp == Defines.CALL_OTHER_ONE_ACCEPT) {
                                                        //對方接
                                                         MyApplication.getInstance()
                                                                .handleAcceptCommandFromOtherOne(address.getHostName(),now_check_str);
                                                    }




                                                }
                                            }
                                        }else if(callReturnPOJO.id == Defines.CALL_BROADCAST_SEND_ALIVE_PACKAGE) {
                                            Log.e("udpThread","收到了心跳包的返回心跳包");
//                                            //設定定時器
                                            closeCallingPulseTimerAndTask();
                                            check_calling_pulse_timer = new Timer();
                                            check_calling_pulse_timer_task = new TimerTask() {
                                                @Override
                                                public void run() {
                                                    MyApplication.getInstance().onAlivePackageTimeout();
                                                    closeCallingPulseTimerAndTask();
                                                }
                                            };
                                            check_calling_pulse_timer.
                                                    schedule(check_calling_pulse_timer_task,
                                                            Defines.CHECK_RECEIVED_ALIVE_PACKAGE_INTERVAL);
                                        }


                                    }


                                }


                                // i'm calling now !!!
//                                if(MyApplication.getInstance().isCallingNow()) {
//
////                                    datagramChannel.send()
//                                }else {
////                                    CallReturnPOJO callReturnPOJO = new CallReturnPOJO();
//
//
//                                }
                            }
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

}
