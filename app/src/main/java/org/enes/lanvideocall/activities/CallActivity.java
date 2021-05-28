package org.enes.lanvideocall.activities;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorManager;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.AudioManager;
import android.media.Image;
import android.media.ImageReader;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.github.abdularis.civ.AvatarImageView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.enes.lanvideocall.R;
import org.enes.lanvideocall.adapters.CallButtonAdapter;
import org.enes.lanvideocall.application.MyApplication;
import org.enes.lanvideocall.pojos.User;
import org.enes.lanvideocall.threads.MyThread;
import org.enes.lanvideocall.threads.VideoReceiveThread;
import org.enes.lanvideocall.utils.Defines;
import org.enes.lanvideocall.utils.audio.AudioUtil;
import org.enes.lanvideocall.utils.video.X264Util;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.util.Arrays;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;


public class CallActivity extends MyActivity implements View.OnClickListener,
        android.hardware.SensorEventListener,CallButtonAdapter.CallButtonAdapterListener,
        TextureView.SurfaceTextureListener,ImageReader.OnImageAvailableListener,
        Animation.AnimationListener {

    /**
     *
     */
    public final static int METHOD_AUDIO = 0;

    /**
     *
     */
    public final static int METHOD_VIDEO = 1;

    public static int VIDEO_SCREEN_WIDTH = 0;

    public static int VIDEO_SCREEN_HEIGHT= 0;

    private static final int recycler_view_span_count = 3;

    private static final String WAKE_LOCK_TAG = MyApplication.getInstance().getPackageName() + ":"
            + CallActivity.class.getName();

    public static final String KEY_IS_FROM_LIST = "KEY_IS_FROM_LIST";

    private boolean needStartVideo;

    public static void gotoCallActivity(Context context) {
        Intent intent = new Intent(context,CallActivity.class);
        intent.putExtra(KEY_IS_FROM_LIST,true);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK);
        context.startActivity(intent);
    }

    private int now_method;

    private int color;

    private boolean isExit;

    /**
     * is use speaker;
     */
    private boolean isUseSpeaker;

    public boolean isUseSpeaker() {
        return isUseSpeaker;
    }

    private boolean isSilent;

    /**
     *
     * @return
     */
    public boolean isSilent() {
        return isSilent;
    }

    @Override
    public void onBackPressed() {
//        super.onBackPressed();
    }

    private User user;

    private Thread videoFromCameraThread;

    private Handler videoFromCameraHandler;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        dismissNotification();
        boolean isCallingNow = MyApplication.getInstance().isCallingNow();
        if(!isCallingNow) {
            gotoApp();
            return;
        }
        initData();
        initView();
        initSensors();
        initWakeLock();
        readNowDataFromApplication();
        initBroadcastReceiver();
    }

    private BroadcastReceiver broadcastReceiver;

    private void initBroadcastReceiver() {
        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if(action.equals(Defines.CALL_BROADCAST_ACTION)) {
                    int broadcast_type =
                            intent.getIntExtra(Defines.CALL_BROADCAST_KEY,-255);
                    if(broadcast_type == Defines.CALL_BROADCAST_CLOSE ||
                        broadcast_type == Defines.CALL_BROADCAST_BUSY ||
                        broadcast_type == Defines.CALL_BROADCAST_USER_NOT_ACCEPT_STILL_PULSE_FINISH ||
                        broadcast_type == Defines.CALL_BROADCAST_NORMAL_CLOSE_CALL ||
                        broadcast_type == Defines.CALL_BROADCAST_SEND_ALIVE_PACKAGE_TIME_OUT)
                        {
                        String str = null;
                        if(broadcast_type == Defines.CALL_BROADCAST_CLOSE ||
                            broadcast_type == Defines.CALL_BROADCAST_SEND_ALIVE_PACKAGE_TIME_OUT) {
                            str = getString(R.string.txt_dialing_timeout);
                        }else if(broadcast_type == Defines.CALL_BROADCAST_BUSY ||
                                broadcast_type ==
                                        Defines.CALL_BROADCAST_USER_NOT_ACCEPT_STILL_PULSE_FINISH) {
                            str = getString(R.string.txt_user_is_busy_now);
                        }
                        if(str != null) {
                            // 直接關閉
                            Toast.makeText(CallActivity.this,str,Toast.LENGTH_SHORT).show();
                        }
                        isExit = true;
                        finish();
                    }else if(broadcast_type == Defines.CALL_BROADCAST_DIALING_NOW ||
                        broadcast_type == Defines.CALL_BROADCAST_RINGING_NOW) {
                        readNowDataFromApplication();
                    }else if(broadcast_type == Defines.CALL_BROADCAST_CONNECTED_NOW) {
                        readNowDataFromApplication();


                    }else if(broadcast_type == Defines.OPEN_CALL_THREAD) {
                        startCallThread();
                    }
                    Log.e("test","收到了廣播:"+broadcast_type);


                }
            }
        };
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Defines.CALL_BROADCAST_ACTION);
        registerReceiver(broadcastReceiver,intentFilter);
    }

    private void unregisterBroadcastReceiver() {
        if(broadcastReceiver != null)
            unregisterReceiver(broadcastReceiver);
    }

    private SensorManager sensorManager;

    private Sensor proximity_sensor;

    private void initSensors() {
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        proximity_sensor = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
    }

    private PowerManager.WakeLock wakeLock;

    private void initWakeLock() {
        PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wakeLock = powerManager.
                newWakeLock(PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK,WAKE_LOCK_TAG);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        float value = event.values[0];
        int now_status = MyApplication.getInstance().getNowCallingStatus();
        if(value == 0f) {
            if(now_method == METHOD_AUDIO && now_status == Defines.CALL_BROADCAST_CONNECTED_NOW) {
                if (!wakeLock.isHeld())
                    wakeLock.acquire();
            }
        }else {
            if(now_method == METHOD_AUDIO && now_status == Defines.CALL_BROADCAST_CONNECTED_NOW) {
                if (wakeLock.isHeld())
                    wakeLock.release();
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    private void registerProximitySensor() {
        if(proximity_sensor != null && now_method == METHOD_AUDIO) {
            sensorManager.registerListener(this,proximity_sensor,
                    SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    private void releaseProximitySensor() {
        if(proximity_sensor != null && now_method == METHOD_AUDIO) {
            sensorManager.unregisterListener(this,proximity_sensor);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        MyApplication.getInstance().cleanNotifications();
        registerProximitySensor();
        if(now_method == METHOD_VIDEO) {
            if(tv_my_view.isAvailable() &&
                    isCameraOn()
                ) {
                turn_on_camera();
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(!isExit){
            MyApplication.getInstance().showNotification();
        }
        releaseProximitySensor();
        if(now_method == METHOD_VIDEO) {
            closeCamera();
        }
    }

    @Override
    protected void onDestroy() {
        unregisterBroadcastReceiver();
        releaseWakeLock();
        dismissNotification();
        closeAudioThreads();
        freeVideoFromCameraThread();
        closeCamera();
        closeOutUDP();
        closeInUDP();
        Log.e("CallActivity","onDestroy");
        super.onDestroy();
    }

    private void releaseWakeLock() {
        if(wakeLock != null) {
            if(wakeLock.isHeld()) {
                wakeLock.release();
            }
            wakeLock = null;
        }
    }

    private AvatarImageView avatar_image_view;

    private TextView tv_name, tv_timer, tv_status;

    private FloatingActionButton fab_hang_up,fab_hang_up_2;

    private RecyclerView recycler_view;

    private CallButtonAdapter callButtonAdapter;

    private LinearLayout ll_not_connected, ll_friend_view;

    private FrameLayout fl_connected,fl_my_view;

    private TextureView tv_my_view;

    public TextureView tv_friend_view;

    private LinearLayout ll_control_view;

    private ImageView iv_my_camera_off;

    private void initView() {
        if(now_method == METHOD_AUDIO) {
            setContentView(R.layout.activity_call);
            tv_timer = findViewById(R.id.tv_timer);
            tv_status = findViewById(R.id.tv_status);
        }else if(now_method == METHOD_VIDEO) {
            setContentView(R.layout.activity_call_video);
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            ll_not_connected = findViewById(R.id.ll_not_connected);
            fl_connected = findViewById(R.id.fl_connected);
            fl_connected.setOnClickListener(this);
            fl_my_view = findViewById(R.id.fl_my_view);
            tv_my_view = findViewById(R.id.tv_my_view);
            iv_my_camera_off = findViewById(R.id.iv_my_camera_off);
            tv_my_view.setSurfaceTextureListener(this);
            ll_friend_view = findViewById(R.id.ll_friend_view);
            tv_friend_view = findViewById(R.id.tv_friend_view);
            tv_friend_view.setSurfaceTextureListener(this);
            ll_control_view = findViewById(R.id.ll_control_view);
            fab_hang_up_2 = findViewById(R.id.fab_hang_up_2);
            fab_hang_up_2.setOnClickListener(this);
        }
        //common
        recycler_view = findViewById(R.id.recycler_view);
        GridLayoutManager gridLayoutManager =
                new GridLayoutManager(this,recycler_view_span_count);
        recycler_view.setLayoutManager(gridLayoutManager);
        callButtonAdapter = new CallButtonAdapter(this,recycler_view);
        recycler_view.setItemAnimator(null);
        callButtonAdapter.setListener(this);
        callButtonAdapter.setMethodType(now_method);
        recycler_view.setAdapter(callButtonAdapter);
        //
        avatar_image_view = findViewById(R.id.avatar_image_view);
        tv_name = findViewById(R.id.tv_name);
        fab_hang_up = findViewById(R.id.fab_hang_up);
        fab_hang_up.setOnClickListener(this);

        if(avatar_image_view != null) {
            avatar_image_view.setAvatarBackgroundColor(color);
            avatar_image_view.setText(String.format("%1s",user.name));
            tv_name.setText(user.name);
        }

    }

    private void initData() {
        user = MyApplication.getInstance().getNowCallingUser();
        color = MyApplication.getInstance().getColor();
        now_method = MyApplication.getInstance().getCallMethod();
    }

    @Override
    public void onClick(View v) {
        if(!isShowAnimationNow) {
            switch (v.getId()) {
                case R.id.fab_hang_up_2:
                    if(ll_control_view.getVisibility() == View.VISIBLE) {
                        onHangupPressed();
                    }else {
                        showOrHideButtons();
                    }
                    break;
                case R.id.fab_hang_up:
                    onHangupPressed();
                    break;
                case R.id.fl_connected:
                    showOrHideButtons();
                    break;

            }
        }
    }

    private void onHangupPressed() {
        MyApplication.getInstance().stopCalling();
        isExit = true;
        gotoApp();
        finish();
    }

    private void gotoApp() {
        Intent intent = new Intent(this, InitActivity.class);
        startActivity(intent);
    }


    private void dismissNotification() {
        MyApplication.getInstance().cleanNotifications();
    }

    private void readNowDataFromApplication() {
        int now_status = MyApplication.getInstance().getNowCallingStatus();
        String status_str = null;
        if(now_status == Defines.CALL_BROADCAST_DIALING_NOW) {
            status_str = getString(R.string.txt_calling_dialing_now);
        }else if(now_status == Defines.CALL_BROADCAST_RINGING_NOW) {
            status_str = getString(R.string.txt_calling_ringing_now);
        }else if(now_status == Defines.CALL_BROADCAST_CONNECTED_NOW) {
            status_str = getString(R.string.txt_connected_now);
        }

        if(tv_status != null)
            tv_status.setText(status_str);

        if(now_status == Defines.CALL_BROADCAST_CONNECTED_NOW) {
            if(now_method == METHOD_AUDIO) {
                if(recycler_view != null && recycler_view.getVisibility() != View.VISIBLE) {
                    recycler_view.setVisibility(View.VISIBLE);
                }
            }else if(now_method == METHOD_VIDEO) {
                recycler_view.setVisibility(View.VISIBLE);
                ll_not_connected.setVisibility(View.GONE);
                fl_connected.setVisibility(View.VISIBLE);
                check_support_pixels_android_change_width_and_height();
                resizeTwoVideoViews();
                initVideoFromCameraThread();
                needStartVideo = true;
                if(tv_my_view.isAvailable()) {
                    turn_on_camera();
                }
            }
            startCallThread();
        }else {
            if(recycler_view != null && recycler_view.getVisibility() != View.GONE) {
                recycler_view.setVisibility(View.GONE);
            }
        }
    }

    private void closeAudioThreads() {
        AudioUtil.getInstance().stopAudioService();
    }
    private boolean isInitedAudioThreads;

    private void initAudioThreads() {
        if(!isInitedAudioThreads) {
            Log.e("CallActivity","initAudioThreads");
            closeAudioThreads();
            String send_to_ip = MyApplication.getInstance().getNowCallingUser().ip;
            AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
            audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
            setVolumeControlStream(AudioManager.STREAM_VOICE_CALL);
            if(now_method == METHOD_AUDIO)
                setUseSpeaker(false);
            else
                setUseSpeaker(true);
            AudioUtil.getInstance().startAudioService(send_to_ip);
            isInitedAudioThreads = true;
        }
    }

    private void setUseSpeaker(boolean isUseSpeaker) {
        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        if(isUseSpeaker) {
            audioManager.setSpeakerphoneOn(true);
        }else {
            audioManager.setSpeakerphoneOn(false);
        }
        this.isUseSpeaker = isUseSpeaker;
    }

    private boolean isStartCallThread;

    private void startCallThread() {
        if(!isStartCallThread) {
            if(MyApplication.getInstance().isCallingNow()) {
//                int method = MyApplication.getInstance().getCallMethod();
//                if(method == CallActivity.METHOD_VIDEO) {
//                    // 啓動相機
//                }
                initAudioThreads();
            }
            isStartCallThread = true;
        }
    }

    private void setMute(boolean isMute) {
        AudioUtil.getInstance().setMute(isMute);
        this.isSilent = isMute;
    }

    @Override
    public void onButtonPress(int position) {
        if(now_method == METHOD_AUDIO) {
            if(position == 0) {
                // 切換揚聲器
                setUseSpeaker(!isUseSpeaker());
            }else if(position == 1) {
                setMute(!isSilent());
            }
        }else if(now_method == METHOD_VIDEO) {
            if(ll_control_view.getVisibility() != View.GONE) {
                if(position == 0) {
                    if(!isChangeCameraNow)
                        setCameraOnOrOff();
                }else if(position == 1) {
                    if(isCameraOn() && !isChangeCameraNow)
                        changeCameraView();
                }else if(position == 2) {
                    setMute(!isSilent());
                }
            }else {
                showOrHideButtons();
            }
            startAutoHideButtonsTimer();
        }
        callButtonAdapter.notifyItemChanged(position);
    }

    private void resizeTwoVideoViews() {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        WindowManager windowManager = getWindowManager();
        windowManager.getDefaultDisplay().getMetrics(displayMetrics);
        int display_width = displayMetrics.widthPixels;
        int display_height = displayMetrics.heightPixels;
        //
        float ratio = (float) VIDEO_SCREEN_HEIGHT / (float) VIDEO_SCREEN_WIDTH;
        int my_view_width = (int) ((float) display_width / 3.0f);
        int my_view_height = (int) ((float) my_view_width / ratio);
        //
        fl_my_view.getLayoutParams().width = my_view_width;
        fl_my_view.getLayoutParams().height = my_view_height;
        //
        int friend_view_width = (int) (display_height * ratio);
        ll_friend_view.getLayoutParams().width = friend_view_width;
        ll_friend_view.getLayoutParams().height = display_height;
    }

    /**
     * 默認前置攝像頭
     */
    private int now_camera_type = CameraCharacteristics.LENS_FACING_BACK;

    private void check_support_pixels_android_change_width_and_height() {
        int want_width = 320;
        String camera_id = Integer.toString(now_camera_type);
        CameraManager cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        CameraCharacteristics cameraCharacteristics = null;
        try {
            cameraCharacteristics = cameraManager.getCameraCharacteristics(camera_id);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        StreamConfigurationMap streamConfigurationMap =
                cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
        Size[] all_support_size = streamConfigurationMap.getOutputSizes(ImageFormat.YUV_420_888);

        int absolute_value = -1;
        int now_width = 0 ;
        int now_height = 0;

        for (int i = 0; i < all_support_size.length; i++) {
            Size size = all_support_size[i];
            int width = size.getWidth();
            int height = size.getHeight();
//            Log.e("test->support_size",width+","+height);
            double result_width = (double)width / 4.0f;
            double result_height = (double)height / 3.0f;
            if(result_width == result_height) {
                int abs = Math.abs(width - want_width);
                if(absolute_value == -1 || abs < absolute_value) {
                    now_width = width;
                    now_height = height;
                    absolute_value = abs;
                }
            }
        }
        if(absolute_value != -1) {
            VIDEO_SCREEN_WIDTH = now_width;
            VIDEO_SCREEN_HEIGHT = now_height;
        }
    }

    private CameraDevice cameraDevice;

    private boolean isCameraOpen;

    public boolean isCameraOn() {
        return isCameraOpen;
    }

    private void closeCamera() {
        Log.e("CallActivity","closeCamera");
        isCameraOpen = false;
        try {
            if (cameraDevice != null) {
                cameraDevice.close();
                cameraDevice = null;
            }
        }catch (Exception e) {
            e.printStackTrace();
        }
        X264Util.clean_env();
//        closeOutUDP();
    }

    private void turn_on_camera() {
        showOrHideButtons();
        CameraManager cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        String camera_id = Integer.toString(now_camera_type);
        check_support_pixels_android_change_width_and_height();
//
        CameraDevice.StateCallback cameraCallback = new CameraDevice.StateCallback() {
            @Override
            public void onOpened(@NonNull CameraDevice camera) {
                cameraDevice = camera;
                isCameraOpen = true;
                startPreview();
                if(callButtonAdapter != null) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            callButtonAdapter.notifyDataSetChanged();
                        }
                    });
                }
//                if(ip != null) {
//                    initOutUDP();
//                }
            }

            @Override
            public void onDisconnected(@NonNull CameraDevice camera) {
                Log.e("test","onDisconnected");
                cameraDevice = null;
            }

            @Override
            public void onError(@NonNull CameraDevice camera, final int error) {
                Log.e("test","onError");
                cameraDevice = null;
            }
        };
        try {
            if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    Activity#requestPermissions
                return;
            }
            cameraManager.openCamera(camera_id, cameraCallback, videoFromCameraHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void initVideoFromCameraThread() {
        if(videoFromCameraThread == null) {
            videoFromCameraThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    Looper.prepare();
                    videoFromCameraHandler = new Handler(Looper.myLooper());
                    Looper.loop();
                }
            });
            videoFromCameraThread.start();
        }
    }

    private void freeVideoFromCameraThread() {
        if(videoFromCameraThread != null && !videoFromCameraThread.isInterrupted()) {
            videoFromCameraThread.interrupt();
            videoFromCameraThread = null;
        }
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        if(tv_my_view.getSurfaceTexture() == surface) {
            if(needStartVideo) {
                turn_on_camera();
            }
        }else if(tv_friend_view.getSurfaceTexture() == surface) {
            initVideoReceiveThread();
        }
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {

    }

    private ImageReader imageReader;

    private void startPreview() {
        SurfaceTexture surfaceTexture = tv_my_view.getSurfaceTexture();
        surfaceTexture.setDefaultBufferSize(VIDEO_SCREEN_WIDTH, VIDEO_SCREEN_HEIGHT);
        X264Util.initYUVTool(VIDEO_SCREEN_WIDTH,VIDEO_SCREEN_HEIGHT,getCameraRotate());
        Surface surface = new Surface(surfaceTexture);
//        if(imageReader != null) {
//            imageReader.close();
//            imageReader = null;
//        }
        Log.e("test",VIDEO_SCREEN_WIDTH+","+VIDEO_SCREEN_HEIGHT);
        imageReader = ImageReader.
                newInstance(VIDEO_SCREEN_WIDTH, VIDEO_SCREEN_HEIGHT, ImageFormat.YUV_420_888, 2);
        imageReader.setOnImageAvailableListener(this, videoFromCameraHandler);
        try {
            final CaptureRequest.Builder previewCaptureRequestBuilder =
                    cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            previewCaptureRequestBuilder.addTarget(surface);
            previewCaptureRequestBuilder.addTarget(imageReader.getSurface());
            List<Surface> surfaces =
                    Arrays.asList(
                            surface
                            , imageReader.getSurface()
                    );
            CameraCaptureSession.StateCallback stateCallback = new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession session) {
                    if (cameraDevice == null) {
                        return;
                    }
//                    previewCaptureRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE,
//                            CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
//                    previewCaptureRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE,
//                            CaptureRequest.CONTROL_AE_MODE_OFF);
                    CaptureRequest previewRequest = previewCaptureRequestBuilder.build();
                    try {
                        session.setRepeatingRequest(previewRequest, null, videoFromCameraHandler);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }

                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                    Log.e("test","onConfigureFailed");
                }
            };
            cameraDevice.createCaptureSession(surfaces, stateCallback, videoFromCameraHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }

    }

    private int getCameraRotate() {
        String camera_id = Integer.toString(now_camera_type);
        CameraCharacteristics cameraCharacteristics = null;
        try {
            CameraManager cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
            cameraCharacteristics = cameraManager.getCameraCharacteristics(camera_id);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        return cameraCharacteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
    }

    @Override
    public void onImageAvailable(ImageReader reader) {
        if (reader == imageReader && isCameraOpen) {
            Image image = reader.acquireLatestImage();
            if(image !=null && image.getPlanes()!= null && image.getPlanes().length == 3
//                    && out_datagram_channel != null
            ) {
                // is nv12
                Image.Plane planes_y = image.getPlanes()[0];
//                int row_stride_y = planes_y.getRowStride();
                Image.Plane planes_u = image.getPlanes()[1];
                Image.Plane planes_v = image.getPlanes()[2];
                ByteBuffer bufferY = planes_y.getBuffer();
                ByteBuffer bufferUV_loss_end_v = planes_u.getBuffer();
                ByteBuffer bufferV = planes_v.getBuffer();
                // write nv12 y and uv data
                byte[] nv12_y_data = new byte[bufferY.remaining()];
                byte[] nv12_uv_data_loss_end_v = new byte[bufferUV_loss_end_v.remaining()];
                bufferY.get(nv12_y_data);
                bufferUV_loss_end_v.get(nv12_uv_data_loss_end_v);
                byte[] nv12_v_data = new byte[bufferV.remaining()];
                bufferV.get(nv12_v_data);
                byte[] nv12_uv_data =
                        Arrays.copyOf(nv12_uv_data_loss_end_v,
                                nv12_uv_data_loss_end_v.length + 1);
                nv12_uv_data[nv12_uv_data.length - 1] = nv12_v_data[nv12_v_data.length - 2];
                nv12_uv_data_loss_end_v = null;
                nv12_v_data = null;
                //
                image.close();
                image = null;
                byte[] return_data_buffer = new byte[VIDEO_SCREEN_WIDTH * VIDEO_SCREEN_HEIGHT];
                int return_length =
                        X264Util.encode_a_nv12_frame(nv12_y_data,nv12_uv_data,return_data_buffer);
                nv12_y_data = null;
                nv12_uv_data = null;
                byte[] return_data = Arrays.copyOfRange(return_data_buffer,0,return_length-1);
                return_data_buffer = null;
                send_use_udp(return_data);
                return_data = null;
            }
            if(image != null) {
                image.close();
            }
        }
    }

    private boolean isShowAnimationNow;

    private boolean isButtonsHide;

    private void showOrHideButtons() {
        if(isButtonsHide) {
            showButtons();
        }else {
            hideButtons();
        }
    }

    private static final long duration = 300;

    private void hideButtons() {
        AlphaAnimation alphaAnimation = new AlphaAnimation(1,0);
        alphaAnimation.setDuration(duration);
        alphaAnimation.setFillAfter(true);
        alphaAnimation.setAnimationListener(this);
        ll_control_view.startAnimation(alphaAnimation);
        alphaAnimation.start();
    }

    private void showButtons() {
        AlphaAnimation alphaAnimation = new AlphaAnimation(0,1);
        alphaAnimation.setDuration(duration);
        alphaAnimation.setFillAfter(true);
        alphaAnimation.setAnimationListener(this);
        ll_control_view.startAnimation(alphaAnimation);
        alphaAnimation.start();
    }

    @Override
    public void onAnimationStart(Animation animation) {
        isShowAnimationNow = true;
    }

    @Override
    public void onAnimationEnd(Animation animation) {
        if(isButtonsHide) {
            ll_control_view.setVisibility(View.VISIBLE);
            startAutoHideButtonsTimer();
            isButtonsHide = false;
        }else {
            ll_control_view.setVisibility(View.GONE);
            stopAutoHideButtonsTimer();
            isButtonsHide = true;
        }
        isShowAnimationNow = false;
    }

    @Override
    public void onAnimationRepeat(Animation animation) {

    }

    private void stopAutoHideButtonsTimer() {
        if(autoHideButtonsTimer != null) {
            autoHideButtonsTimer.cancel();
            autoHideButtonsTimer = null;
        }
        if(autoHideButtonsTimerTask != null) {
            autoHideButtonsTimerTask.cancel();
            autoHideButtonsTimerTask = null;
        }
    }

    private Timer autoHideButtonsTimer;

    private TimerTask autoHideButtonsTimerTask;

    private void startAutoHideButtonsTimer() {
        stopAutoHideButtonsTimer();
        autoHideButtonsTimer = new Timer();
        autoHideButtonsTimerTask = new TimerTask() {
            @Override
            public void run() {
                hideButtonsBackground();
            }
        };
        autoHideButtonsTimer.schedule(autoHideButtonsTimerTask,2000);
    }

    private void hideButtonsBackground() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                hideButtons();
            }
        });
    }

    private void setCameraOnOrOff() {
        if (isCameraOn()) {
            closeCamera();
            tv_my_view.setVisibility(View.GONE);
            iv_my_camera_off.setVisibility(View.VISIBLE);
        }else {
            turn_on_camera();
            tv_my_view.setVisibility(View.VISIBLE);
            iv_my_camera_off.setVisibility(View.GONE);
        }
    }

    public boolean isFrontCamera() {
        if(now_camera_type == CameraCharacteristics.LENS_FACING_BACK) {
            return true;
        }else
            return false;
    }

    private boolean isChangeCameraNow;

    private void changeCameraView() {
        isChangeCameraNow = true;
        closeCamera();
        if(now_camera_type == CameraCharacteristics.LENS_FACING_BACK) {
            now_camera_type = CameraCharacteristics.LENS_FACING_FRONT;
        }else {
            now_camera_type = CameraCharacteristics.LENS_FACING_BACK;
        }
        turn_on_camera();
        isChangeCameraNow = false;
    }

    private void send_use_udp(byte[] return_data) {
        if(out_datagram_channel == null) {
            initOutUDP();
        }
        if(MyApplication.getInstance() == null ||
                MyApplication.getInstance().getNowCallingUser() == null) {
            return;
        }
        if(return_data != null && out_datagram_channel != null) {
            byte [] header_byte = VideoReceiveThread.header.getBytes();
            int header_length = header_byte.length;
            int data_length = return_data.length;
            int all_size = header_length + data_length;
            ByteBuffer byteBuffer = ByteBuffer.allocate(all_size);
            byteBuffer.put(header_byte);
            byteBuffer.put(return_data);
            byteBuffer.flip();
            try {
                out_datagram_channel.send(byteBuffer,out_socket_address);
            } catch (IOException e) {
                e.printStackTrace();
            }
            byteBuffer.clear();
            byteBuffer = null;
        }


//        String ip = MyApplication.getInstance().getNowCallingUser().ip;
//        if(return_data != null && ip != null) {
//            byte [] header_byte = VideoReceiveThread.header.getBytes();
//            int header_length = header_byte.length;
//            int data_length = return_data.length;
//            int all_size = header_length + data_length;
//            ByteBuffer byteBuffer = ByteBuffer.allocate(all_size);
//            byteBuffer.put(header_byte);
//            byteBuffer.put(return_data);
//            byteBuffer.flip();
//            try {
//                byte [] final_byte = byteBuffer.array();
//                InetAddress inetAddress =
//                        InetAddress.getByName(ip);
//                DatagramPacket datagramPacket =
//                        new DatagramPacket(final_byte,0,final_byte.length,
//                                inetAddress,Defines.VIDEO_SERVER_PORT);
//                DatagramSocket s = new DatagramSocket();
//                s.send(datagramPacket);
//                s.close();
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//
//            byteBuffer.clear();
//            byteBuffer = null;
//            return_data = null;
//        }
    }

    private VideoReceiveThread videoReceiveThread;

    private void initVideoReceiveThread() {
        releaseVideoReceiveThread();
        initInUDP();
        videoReceiveThread = new VideoReceiveThread(this);
        videoReceiveThread.start();
    }

    private void releaseVideoReceiveThread() {
        if(videoReceiveThread != null) {
            videoReceiveThread.free();
            videoReceiveThread = null;
        }
    }

    private DatagramChannel out_datagram_channel;

    private InetSocketAddress out_socket_address;

    private void initOutUDP() {
        closeOutUDP();
        try {
            out_datagram_channel = DatagramChannel.open();
            out_datagram_channel.configureBlocking(true);
            out_socket_address = new InetSocketAddress(MyApplication.getInstance().getNowCallingUser().ip,
                    Defines.VIDEO_SERVER_PORT);
            Log.e("test","initOutUDP");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void closeOutUDP() {
        if(out_datagram_channel != null) {
            try {
                out_datagram_channel.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            out_socket_address = null;
        }
    }

    public DatagramChannel in_datagram_channel;

    private void initInUDP() {
        closeInUDP();
        try {
            in_datagram_channel = DatagramChannel.open();
            in_datagram_channel.configureBlocking(true);
            in_datagram_channel.socket().bind(new InetSocketAddress(Defines.VIDEO_SERVER_PORT));
            Log.e("test","initInUDP");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void closeInUDP() {
        Log.e("test","closeInUDP");
        if(in_datagram_channel != null) {
            try {
                in_datagram_channel.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            in_datagram_channel = null;
        }
    }

}
