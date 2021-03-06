package org.enes.lanvideocall.application;

import android.app.Application;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.telecom.Call;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import org.enes.lanvideocall.R;
import org.enes.lanvideocall.activities.CallActivity;
import org.enes.lanvideocall.activities.IncomingCallActivity;
import org.enes.lanvideocall.network.ControlUDPThread;
import org.enes.lanvideocall.network.GetAllUserListThread;
import org.enes.lanvideocall.network.UpdateUserListThread;
import org.enes.lanvideocall.pojos.User;
import org.enes.lanvideocall.threads.AlivePackageSendThread;
import org.enes.lanvideocall.threads.DialThread;
import org.enes.lanvideocall.threads.EndCallThread;
import org.enes.lanvideocall.threads.MyThread;
import org.enes.lanvideocall.threads.RingingPulseThread;
import org.enes.lanvideocall.threads.SendAcceptOrIgnoreThread;
import org.enes.lanvideocall.utils.Defines;
import org.enes.lanvideocall.utils.SharedPreferencesUtil;

import java.util.Timer;
import java.util.TimerTask;

public class MyApplication extends Application implements DialThread.DialThreadInterface,
    RingingPulseThread.RingingPulseThreadInterface {

    private static MyApplication instance;

    public static MyApplication getInstance() {
        return instance;
    }

    public static void killMySelf() {
        android.os.Process.killProcess(android.os.Process.myPid());
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        cleanNotifications();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        cleanNotifications();
        startServices();
        startAutoRefreshUser();
        startControlUDPServer();
    }

    private void startServices() {


    }

    private GetAllUserListThread getAllUserListThread;

    public void updateUserList() {
        if(getAllUserListThread != null) {
            getAllUserListThread.interrupt();
            getAllUserListThread = null;
        }
        getAllUserListThread = new GetAllUserListThread();
        getAllUserListThread.start();
    }

    private UpdateUserListThread userListThread;

    public void updateMyLoginStatus() {
        if(userListThread != null) {
            userListThread.interrupt();
            userListThread = null;
        }
        userListThread = new UpdateUserListThread();
        userListThread.start();
    }

    private void startAutoRefreshUser() {
        long one_minute_millis = 60 * 1000;
        Timer timer = new Timer();
        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                Log.e("test","TimerTask");
                if (SharedPreferencesUtil.getUUID() != null) {
                    updateMyLoginStatus();
                    updateUserList();
                }
            }
        };
        timer.schedule(timerTask,0,one_minute_millis);
    }

    private ControlUDPThread controlUDPThread;

    private void startControlUDPServer() {
        if(controlUDPThread != null) {
            controlUDPThread.interrupt();
            controlUDPThread = null;
        }
        controlUDPThread = new ControlUDPThread();
        controlUDPThread.start();
    }

    private boolean isCallingNow;

    public boolean isCallingNow() {
        return isCallingNow;
    }

    public void setCallingNow(boolean newStatus) {
        this.isCallingNow = newStatus;
    }

    private User now_calling_user;

    public User getNowCallingUser() {
        return now_calling_user;
    }

    public void setNowCallingUser(User new_user) {
        now_calling_user = new_user;
    }

    private int color;

    public int getColor() {
        if(color == 0) {
            return getResources().getColor(R.color.color_cycle_1,getTheme());
        }
        return color;
    }

    private int call_method;

    public int getCallMethod() {
        return call_method;
    }

    public void setCallMethod(int call_method) {
        this.call_method = call_method;
    }

    /**
     * ?????????????????????
     */
    private int now_calling_status = Defines.CALL_BROADCAST_NO_CALLING_NOW;

    public int getNowCallingStatus() {
        return now_calling_status;
    }

    public void setCallingStatus(int status) {
        now_calling_status = status;
        sendBroadcast(now_calling_status);
    }

    public void sendBroadcast(int status) {
        Intent intent = new Intent();
        intent.setAction(Defines.CALL_BROADCAST_ACTION);
        intent.putExtra(Defines.CALL_BROADCAST_KEY,status);
        sendBroadcast(intent);
    }

    private static final String notification_channel = "lanvideocall" +
            ".call_notification";

    private static final int notification_id = 0;

    public void showNotification() {
        Intent intent = new Intent(this, CallActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        String content = getString(R.string.txt_is_calling_now);
        NotificationCompat.BigTextStyle bigTextStyle = new NotificationCompat.BigTextStyle();
        bigTextStyle.bigText(content);
        Notification notification =
                new NotificationCompat.Builder(this,notification_channel)
                        .setSmallIcon(R.drawable.ic_notification_icon)
                        .setContentText(content)
                        .setContentTitle(getString(R.string.app_name))
                        .setStyle(bigTextStyle)
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setContentIntent(pendingIntent)
                        .setAutoCancel(false)
                        .setOngoing(true)
                        .build();
        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = getString(R.string.txt_channel_name);
            String description = getString(R.string.txt_channel_description);
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(notification_channel, name, importance);
            channel.setDescription(description);
            channel.setShowBadge(true);
            channel.setImportance(NotificationManager.IMPORTANCE_HIGH);
            notificationManager.createNotificationChannel(channel);
        }
        notificationManager.notify(notification_id, notification);
    }

    public void cleanNotifications() {
        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        notificationManager.cancelAll();
    }

    private MyThread now_thread;

    private String now_check_str;

    public void setNowCheckStr(String check_str) {
        now_check_str = check_str;
    }

    public String getNowCheckStr() {
        return now_check_str;
    }

    private TimerTask dialWaitingTimerTask;

    private Timer dialWaitingTimer;

    /**
     * start calling
     * @param user
     * @param color
     * @param method
     */
    public void startCallingNow(User user,int color,int method) {
        if(!isCallingNow()) {
            setCallingNow(true);
            now_calling_user = user;
            this.color = color;
            call_method = method;
            CallActivity.gotoCallActivity(MyApplication.getInstance().getApplicationContext());
            setCallingStatus(Defines.CALL_BROADCAST_DIALING_NOW);
            //
            startDialWaitingTimerTask();
            //
            if(now_thread != null) {
                now_thread.kill();
            }
            now_thread = new DialThread(now_calling_user.ip);
            ((DialThread) now_thread).setDialThreadInterface(this);
            now_thread.start();
        }
    }

    private void cancelDialWaitingTimer() {
        if(dialWaitingTimer != null) {
            dialWaitingTimer.cancel();
            dialWaitingTimer = null;
        }
    }

    private void cancelDialWaitingTimerTask() {
        if(dialWaitingTimerTask != null) {
            dialWaitingTimerTask.cancel();
            dialWaitingTimerTask = null;
        }
    }

    private void startDialWaitingTimerTask() {
        cancelDialWaitingTimer();
        cancelDialWaitingTimerTask();
        dialWaitingTimerTask = new TimerTask() {
            @Override
            public void run() {
                //???????????? ???????????????
                Log.e("startDialWaitingTimerTask","??????????????????");
                MyApplication.getInstance().sendBroadcast(Defines.CALL_BROADCAST_CLOSE);
                stopCalling();
            }
        };
        dialWaitingTimer = new Timer();
        dialWaitingTimer.schedule(dialWaitingTimerTask,Defines.Dialing_WAITING_TIME);
    }

    @Override
    public void onDialSent(String check_str) {
        Log.e("onDialSent",check_str);
        now_check_str = check_str;
    }

    /**
     * ?????????????????????????????????
     * @param check_str
     * @param resp
     */
    public void onDialingResponseReceived(String check_str,int resp) {
        if(check_str.equals(now_check_str)) {
            cancelDialWaitingTimer();
            cancelDialWaitingTimerTask();
            if(resp == Defines.CALL_ACCEPT_CALLING_NOW && now_calling_user != null) {
                // ?????????????????????????????????????????????
                setCallingStatus(Defines.CALL_BROADCAST_RINGING_NOW);
                // ???????????????thread 5???????????????
                if(now_thread != null) {
                    now_thread.kill();
                    now_thread = null;
                }
                now_thread = new RingingPulseThread(now_calling_user.ip,now_check_str,call_method);
                ((RingingPulseThread) now_thread).setRingingPulseThreadInterface(this);
                now_thread.start();
            }else if(resp == Defines.CALL_REJECT_CALLING_NOW) {
                // ???????????? ?????????????????????
                sendBroadcast(Defines.CALL_BROADCAST_BUSY);
                stopCalling();
            }
        }
    }

    @Override
    public void onPulseSent() {
        Log.e("test","?????????????????????");
    }

    @Override
    public void onPulseThreadFinished() {
        Log.e("onPulseThreadFinished","????????????????????????????????????");
        sendBroadcast(Defines.CALL_BROADCAST_USER_NOT_ACCEPT_STILL_PULSE_FINISH);
        stopCalling();
    }

    /**
     * stop
     */
    public void stopCalling() {
        if(isCallingNow()) {
            now_calling_status = Defines.CALL_BROADCAST_NO_CALLING_NOW;
            if(now_thread != null) {
                now_thread.kill();
            }
            //?????????????????????
            if(getNowCallingUser() != null) {
                EndCallThread endCallThread =
                        new EndCallThread(getNowCallingUser().ip,now_check_str);
                endCallThread.start();
            }
            setCallingNow(false);
            cancelDialWaitingTimer();
            cancelDialWaitingTimerTask();
            isIncomingCallActivityOpened = false;
            now_calling_user = null;
            if(controlUDPThread != null) {
                controlUDPThread.closeCallingPulseTimerAndTask();
            }
        }
    }

    /**
     * ????????????????????????
     */
    public void onPulseTimeout() {
        Log.e("test","????????????????????????????????????");
        sendBroadcast(Defines.CALL_BROADCAST_CLOSE_INCOMING_VIEW);
        stopCalling();
    }

    private boolean isIncomingCallActivityOpened;

    /**
     * ???????????????????????????
     */
    public void onPulseReceived(String name, String uuid, int method, String ip) {
        setCallingNow(true);
        if(!isIncomingCallActivityOpened) {
            showAcceptActivity(name,uuid,method,ip);
            isIncomingCallActivityOpened = true;
        }
    }

    public void showAcceptActivity(String name, String uuid, int method, String ip) {
        Log.e("test","???????????? activity:");
        IncomingCallActivity.openIncomingCallActivity(
                getInstance().getApplicationContext(),name,uuid,method,ip);
    }

    /**
     * ??????????????????
     * @param check_str
     */
    public void onOtherSideEndCall(String check_str) {
        if(check_str.equals(now_check_str)) {
            if(getNowCallingStatus() == Defines.CALL_BROADCAST_RINGING_NOW) {
                // ???????????????????????????
                Log.e("test","?????????");
//                if(isIncomingCallActivityOpened) {
//                    //??????activity
//                    sendBroadcast(Defines.CALL_BROADCAST_CLOSE_INCOMING_VIEW);
//                }
                sendBroadcast(Defines.CALL_BROADCAST_NORMAL_CLOSE_CALL);
                sendBroadcast(Defines.CALL_BROADCAST_CLOSE_INCOMING_VIEW);
                stopCalling();
            }else if(getNowCallingStatus() == Defines.CALL_BROADCAST_CONNECTED_NOW) {
                // ????????????????????????
                Log.e("test","?????????");


                sendBroadcast(Defines.CALL_BROADCAST_NORMAL_CLOSE_CALL);
                stopCalling();
            }
            Log.e("test","????????????");
        }
    }

    /**
     * ?????????????????????
     * @param ip
     */
    public void acceptIncomingCall(String ip) {
        sendAcceptOrIgnore(true,ip);
        setCallingStatus(Defines.CALL_BROADCAST_CONNECTED_NOW);
        //??????activity
        CallActivity.gotoCallActivity(MyApplication.getInstance().getApplicationContext());
        //????????????
        sendBroadcast(Defines.OPEN_CALL_THREAD);
    }

    /**
     * ?????????????????????
     * @param ip
     */
    public void ignoreIncomingCall(String ip) {
        sendAcceptOrIgnore(false,ip);
        stopCalling();
    }

    private void sendAcceptOrIgnore(boolean isAccept,String ip) {
        if(isCallingNow()) {
            String check_str = now_check_str;
            SendAcceptOrIgnoreThread sendAcceptOrIgnoreThread =
                    new SendAcceptOrIgnoreThread(ip,check_str,isAccept);
            sendAcceptOrIgnoreThread.start();
        }
    }


    /**
     * ?????????????????????????????????????????????
     */
    public void handleAcceptCommandFromOtherOne(String ip, String check_str) {
        if(check_str.equals(now_check_str)) {
            //????????????????????????????????????
            if(now_thread != null && now_thread instanceof RingingPulseThread) {
                now_thread.kill();
            }
            //???????????????????????????
            setCallingStatus(Defines.CALL_BROADCAST_CONNECTED_NOW);
            //?????????????????????
            now_thread = new AlivePackageSendThread(ip,check_str);
            now_thread.start();
            //????????????
            sendBroadcast(Defines.OPEN_CALL_THREAD);
        }
    }

    /**
     *
     */
    public void onAlivePackageTimeout() {
        Log.e("udpThread","???????????????");
        if(isCallingNow()) {
            sendBroadcast(Defines.CALL_BROADCAST_SEND_ALIVE_PACKAGE_TIME_OUT);
            stopCalling();
        }
    }

}
