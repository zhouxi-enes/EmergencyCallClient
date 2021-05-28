package org.enes.lanvideocall.activities;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;

import org.enes.lanvideocall.R;
import org.enes.lanvideocall.application.MyApplication;
import org.enes.lanvideocall.utils.Defines;
import org.enes.lanvideocall.utils.audio.RingtoneUtil;

public class IncomingCallActivity extends MyActivity implements View.OnClickListener {

    public static final String KEY_NAME = "name";

    public static final String KEY_UUID = "uuid";

    public static final String KEY_METHOD = "method";

    public static final String KEY_IP = "ip";

    public static void openIncomingCallActivity(Context context, String name, String uuid,
                                                int method, String ip) {
        Intent intent = new Intent(context,IncomingCallActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK);
        intent.putExtra(KEY_NAME,name);
        intent.putExtra(KEY_UUID,uuid);
        intent.putExtra(KEY_METHOD,method);
        intent.putExtra(KEY_IP,ip);
        context.startActivity(intent);
    }

    @Override
    public void onBackPressed() {
//        super.onBackPressed();
    }


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(!MyApplication.getInstance().isCallingNow()) {
            Log.e("test","isCallingNow");
            gotoApp();
            finish();
            return;
        }
        registerBroadcastReceiver();
        initView();
        initData();
        RingtoneUtil.getInstance().play(this);
    }

    private BroadcastReceiver broadcastReceiver;

    private void registerBroadcastReceiver() {
        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if(action.equals(Defines.CALL_BROADCAST_ACTION)) {
                    int broadcast_type =
                            intent.getIntExtra(Defines.CALL_BROADCAST_KEY,-255);
                    if(broadcast_type == Defines.CALL_BROADCAST_CLOSE_INCOMING_VIEW) {
                        finish();
                    }
                }
            }
        };
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Defines.CALL_BROADCAST_ACTION);
        registerReceiver(broadcastReceiver,intentFilter);
    }

    private void unRegisterBroadcastReceiver() {
        if(broadcastReceiver != null) {
            unregisterReceiver(broadcastReceiver);
        }
    }

    @Override
    protected void onDestroy() {
        unRegisterBroadcastReceiver();
        RingtoneUtil.getInstance().stop();
        Log.e("test","onDestroy");
        super.onDestroy();
    }

    private void gotoApp() {
        Intent intent = new Intent(this, InitActivity.class);
        startActivity(intent);
    }

    private TextView tv_name,tv_method;

    private LinearLayout ll_accept,ll_denied;

    private void initView() {
        setContentView(R.layout.activity_incoming_call);
        tv_name = findViewById(R.id.tv_name);
        tv_method = findViewById(R.id.tv_method);
        ll_accept = findViewById(R.id.ll_accept);
        ll_accept.setOnClickListener(this);
        ll_denied = findViewById(R.id.ll_denied);
        ll_denied.setOnClickListener(this);

    }

    private String ip;

    private void initData() {
        Intent intent = getIntent();
        String name = intent.getStringExtra(KEY_NAME);
        tv_name.setText(name);
        int method = intent.getIntExtra(KEY_METHOD,-1);
        String call_method_name = null;
        if(method == CallActivity.METHOD_AUDIO) {
            call_method_name = getString(R.string.txt_audio_call);
        }else if(method == CallActivity.METHOD_VIDEO) {
            call_method_name = getString(R.string.txt_video_call);
        }
        if(call_method_name != null) {
            tv_method.setText(call_method_name);
        }
        ip = intent.getStringExtra(KEY_IP);
        MyApplication.getInstance().setCallMethod(method);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.ll_accept:
                MyApplication.getInstance().acceptIncomingCall(ip);
                finish();
                break;
            case R.id.ll_denied:
                MyApplication.getInstance().ignoreIncomingCall(ip);
                finish();
                break;

        }
    }
}
