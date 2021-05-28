package org.enes.lanvideocall.activities;

import android.content.Context;
import android.media.AudioManager;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.Nullable;

import org.enes.lanvideocall.R;
import org.enes.lanvideocall.utils.Defines;
import org.enes.lanvideocall.utils.audio.PlayAudioThread;
import org.enes.lanvideocall.utils.audio.RecordAudioThread;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class AudioTestActivity extends MyActivity implements View.OnClickListener, PlayAudioThread.PlayAudioThreadCallback {

    private TextView tv_my_ip;

    private EditText et_press_ip;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initView();
        initAudioSetting();
    }

    private void initView() {
        setContentView(R.layout.activity_audio_call_test);
        tv_my_ip = findViewById(R.id.tv_my_ip);
        et_press_ip = findViewById(R.id.et_press_ip);
        Button btn_connect_to_this_device = findViewById(R.id.btn_connect_to_this_device);
        btn_connect_to_this_device.setOnClickListener(this);
        Button btn_disconnect = findViewById(R.id.btn_disconnect);
        btn_disconnect.setOnClickListener(this);
        Button switch_speaker = findViewById(R.id.switch_speaker);
        switch_speaker.setOnClickListener(this);
    }

    private PlayAudioThread playAudioThread;

    private RecordAudioThread recordAudioThread;

    private void initAudioSetting() {
        WifiManager wm = (WifiManager) getSystemService(WIFI_SERVICE);
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
            setSpeakerphoneOn(true);
            tv_my_ip.setText("my ip:"+ip);
            Log.e("test","ip:"+ip);
        }
    }

    private void startPlayAudioThread() {
        if(playAudioThread != null) {
            playAudioThread.free();
            playAudioThread = null;
        }
        playAudioThread = new PlayAudioThread(Defines.AUDIO_SERVER_PORT,Defines.AUDIO_SAMPLE_RATE);
        playAudioThread.setPlayAudioThreadCallback(this);
        playAudioThread.start();
        tv_my_ip.append("playAudioThread start\n");
    }

    private void startRecordAudioThread(String ip) {
        if(recordAudioThread != null) {
            recordAudioThread.free();
            recordAudioThread = null;
        }
        recordAudioThread = new
                RecordAudioThread(ip,Defines.AUDIO_SERVER_PORT,Defines.AUDIO_SAMPLE_RATE);
        recordAudioThread.start();
        tv_my_ip.append("recordAudioThread start\n");
    }


    @Override
    protected void onDestroy() {
        if(playAudioThread != null) {
            playAudioThread.free();
            playAudioThread = null;
        }
        if(recordAudioThread != null) {
            recordAudioThread.free();
            recordAudioThread = null;
        }
        super.onDestroy();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_connect_to_this_device:
                connect();
                break;
            case R.id.btn_disconnect:
                disconnect();
                break;
            case R.id.switch_speaker:
                switch_speaker();
                break;
        }
    }

    private void connect() {
        String ip = et_press_ip.getText().toString();
        if(playAudioThread == null) {
            startPlayAudioThread();
        }
        if(recordAudioThread == null) {
            startRecordAudioThread(ip);
        }
    }

    private void disconnect() {
        if(recordAudioThread != null) {
            recordAudioThread.free();
            recordAudioThread = null;
        }
        if(playAudioThread != null) {
            playAudioThread.free();
            playAudioThread = null;
        }
    }

    @Override
    public void onPlayAudioThreadReceivedData(PlayAudioThread which) {
        InetSocketAddress inetSocketAddress = (InetSocketAddress) which.socketAddress;
        if(recordAudioThread == null && inetSocketAddress != null) {
            startRecordAudioThread(inetSocketAddress.getHostName());
        }
    }

    private void switch_speaker() {
        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        if(audioManager.isSpeakerphoneOn()) {
            setSpeakerphoneOn(false);
        }else {
            setSpeakerphoneOn(true);
        }
    }

    private void setSpeakerphoneOn(boolean on) {
        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        if (on) {
            audioManager.setSpeakerphoneOn(true);
            Log.e("test","1111111");
        } else {
            audioManager.setSpeakerphoneOn(false);
            Log.e("test","222222");
        }
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
        audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
    }
}
