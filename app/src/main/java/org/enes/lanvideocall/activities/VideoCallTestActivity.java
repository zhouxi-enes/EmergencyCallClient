package org.enes.lanvideocall.activities;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.enes.lanvideocall.R;
import org.enes.lanvideocall.utils.Defines;
import org.enes.lanvideocall.utils.NetworkUtil;
import org.enes.lanvideocall.utils.video.X264Util;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class VideoCallTestActivity extends MyActivity implements TextureView.SurfaceTextureListener,
    ImageReader.OnImageAvailableListener, View.OnClickListener {

    private TextView tv_log;

    private CameraDevice cameraDevice;

    private TextureView texture_view, texture_view2;

    private Handler handler;

    private Thread thread;

    private ImageReader imageReader;

    private int encoded_width, encoded_height;

    private MediaCodec decoder;

    private boolean isCameraOpen;

    private DatagramChannel in_datagram_channel;
    private DatagramChannel out_datagram_channel;

    private InetSocketAddress out_socket_address;

    private ReceiveThread receiveThread;

    private EditText et_input_ip;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setActivity();
        createThread();
        initView();
        tv_log.append("\n"+NetworkUtil.getMyIP(this)+"\n");
    }

    public static String ip = null;

    private void initOutUDP() {
        closeOutUDP();
        try {
            out_datagram_channel = DatagramChannel.open();
            out_datagram_channel.configureBlocking(true);
            out_socket_address = new InetSocketAddress(ip, Defines.VIDEO_SERVER_PORT);
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

    private void initInUDP() {
        closeInThread();
        closeInUDP();
        try {
            in_datagram_channel = DatagramChannel.open();
            in_datagram_channel.configureBlocking(true);
            in_datagram_channel.socket().bind(new InetSocketAddress(Defines.VIDEO_SERVER_PORT));
            Log.e("test","initInUDP");
        } catch (IOException e) {
            e.printStackTrace();
        }
        initInThread();
    }

    private void initInThread() {
        closeInThread();
        receiveThread = new ReceiveThread();
        receiveThread.start();
        Log.e("test","initInThread");
    }

    private void closeInThread() {
        if(receiveThread != null) {
            receiveThread.free();
        }
        receiveThread = null;
        Log.e("test","closeInThread");
    }

    private void closeInUDP() {
        closeInThread();
        if(in_datagram_channel != null) {
            try {
                in_datagram_channel.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            in_datagram_channel = null;
            Log.e("test","closeInUDP");
        }
    }

    private void closeUDP() {
        closeInUDP();
        closeOutUDP();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(texture_view.isAvailable()) {
            turn_on_camera();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        closeCamera();
    }

    private void setActivity() {
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    private void initView() {
        setContentView(R.layout.activity_video_call_test);
        Button btn_on_or_off = findViewById(R.id.btn_on_or_off);
        btn_on_or_off.setOnClickListener(this);
        Button btn_change_camera = findViewById(R.id.btn_change_camera);
        btn_change_camera.setOnClickListener(this);
        tv_log = findViewById(R.id.tv_log);
        texture_view = findViewById(R.id.texture_view);
        texture_view.setSurfaceTextureListener(this);
        texture_view2 = findViewById(R.id.texture_view2);
        texture_view2.setSurfaceTextureListener(this);
        et_input_ip = findViewById(R.id.et_input_ip);
        Button set_ip = findViewById(R.id.set_ip);
        set_ip.setOnClickListener(this);
    }

    private void createThread() {
        thread = new Thread(new Runnable() {
            @Override
            public void run() {
                Looper.prepare();
                handler = new Handler(Looper.myLooper());
                Looper.loop();
            }
        });
        thread.start();
    }

    private int now_camera_type;

    private void check_support_pixels_android_change_width_and_height() {
//        int want_width = 640;
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
//                Log.e("support size:", size.getWidth() + "," + size.getHeight());
            }
        }
        if(absolute_value != -1) {
            encoded_width = now_width;
            encoded_height = now_height;
        }
    }

    private void turn_on_camera() {
        CameraManager cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            String[] all_cameras = cameraManager.getCameraIdList();
            tv_log.append(getString(R.string.txt_get_cameras));
            for (int i = 0; i < all_cameras.length; i++) {
                tv_log.append(all_cameras[i]);
                if (i + 1 < all_cameras.length) {
                    tv_log.append(",");
                }
            }
            tv_log.append(".OK!\n");
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        String camera_id = Integer.toString(now_camera_type);
        check_support_pixels_android_change_width_and_height();

        CameraDevice.StateCallback cameraCallback = new CameraDevice.StateCallback() {
            @Override
            public void onOpened(@NonNull CameraDevice camera) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        tv_log.append("Camera opened");
                    }
                });
                cameraDevice = camera;
                isCameraOpen = true;
                startPreview();
                if(ip != null) {
                    initOutUDP();
                }
            }

            @Override
            public void onDisconnected(@NonNull CameraDevice camera) {
                Log.e("test","onDisconnected");
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        tv_log.append("Camera Disconnected");
                    }
                });
            }

            @Override
            public void onError(@NonNull CameraDevice camera, final int error) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        tv_log.append("onError: code = " + error);
                    }
                });
            }
        };
        try {
            if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    Activity#requestPermissions
                return;
            }
            cameraManager.openCamera(camera_id, cameraCallback, handler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void closeCamera() {
        Log.e("test","closeCamera");
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
        closeOutUDP();
    }

    private void startPreview() {
        SurfaceTexture surfaceTexture = texture_view.getSurfaceTexture();
        surfaceTexture.setDefaultBufferSize(encoded_width, encoded_height);
        X264Util.initYUVTool(encoded_width,encoded_height,getCameraRotate());
        Surface surface = new Surface(surfaceTexture);
        Log.e("teset",encoded_width+","+encoded_height);
        imageReader = ImageReader.
                newInstance(encoded_width, encoded_height, ImageFormat.YUV_420_888, 4);
        imageReader.setOnImageAvailableListener(this, handler);
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
                        session.setRepeatingRequest(previewRequest, null, handler);
                    } catch (CameraAccessException e) {
                        tv_log.append("setRepeatingRequest Failed");
                        e.printStackTrace();
                    }

                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                    tv_log.append("onConfigureFailed");
                }
            };
            cameraDevice.createCaptureSession(surfaces, stateCallback, handler);


        } catch (CameraAccessException e) {
            e.printStackTrace();
        }

    }

    @Override
    protected void onDestroy() {
        closeUDP();
        handler = null;
        thread = null;
        super.onDestroy();
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        tv_log.append("onSurfaceTextureAvailable, width:" + width + " height:" + height+"\n");
        if(surface == texture_view.getSurfaceTexture()) {
            now_camera_type = CameraCharacteristics.LENS_FACING_FRONT;
            turn_on_camera();
        }else if(surface == texture_view2.getSurfaceTexture()) {
            initInUDP();
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

    @Override
    public void onImageAvailable(ImageReader reader) {
        if (reader == imageReader && isCameraOpen) {
            Image image = reader.acquireLatestImage();
            if(image !=null && image.getPlanes()!= null && image.getPlanes().length == 3
                    && out_datagram_channel != null
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
                byte[] return_data_buffer = new byte[encoded_width * encoded_height];
                int return_length =
                        X264Util.encode_a_nv12_frame(nv12_y_data,nv12_uv_data,return_data_buffer);
                nv12_y_data = null;
                nv12_uv_data = null;
                byte[] return_data = Arrays.copyOfRange(return_data_buffer,0,return_length-1);
                return_data_buffer = null;
              send_use_udp(return_data);
//                onDecodeData(return_data);
                return_data = null;
            }
            if(image != null) {
                image.close();
            }
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


    private static final String header = "264_h";

    /**
     *
     * @param return_data
     */
    private void send_use_udp(byte[] return_data) {
        if(return_data != null && out_datagram_channel != null) {
            byte [] header_byte = header.getBytes();
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
    }

    private void initVideoDecode() {
        try {
            decoder = MediaCodec.createDecoderByType(MediaFormat.MIMETYPE_VIDEO_AVC);
            MediaFormat mediaFormat = MediaFormat.
                    createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC,encoded_width,encoded_height);
            mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT,
                    MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible);
//            mediaFormat.setInteger(MediaFormat.KEY_ROTATION,getCameraRotate());

            SurfaceTexture surfaceTexture = texture_view2.getSurfaceTexture();
//            surfaceTexture.setDefaultBufferSize(encoded_width, encoded_height);
            Surface surface = new Surface(surfaceTexture);

            decoder.configure(mediaFormat, surface, null, 0);
            decoder.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();

    public void onDecodeData(byte[] codeData) {
        if(decoder != null) {
            ByteBuffer[] inputBuffer = decoder.getInputBuffers();
            int inputIndex = decoder.dequeueInputBuffer(0);

            if (inputIndex >= 0) {
                ByteBuffer buffer = inputBuffer[inputIndex];
                try {
                    buffer.put(codeData);
                } catch (NullPointerException e) {
                    e.printStackTrace();
                }
                decoder.queueInputBuffer(inputIndex, 0, codeData.length, 0, 0);
                buffer.clear();
            }
            int outputIndex = decoder.dequeueOutputBuffer(info, 0);
            if (outputIndex >= 0) {
                decoder.releaseOutputBuffer(outputIndex, true);
            }
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_on_or_off:
                turn_on_and_off_camera();
                break;
            case R.id.btn_change_camera:
                changeCameraView();
                break;
            case R.id.set_ip:
                setIP();
                break;
        }
    }

    private void setIP() {
        String new_ip = et_input_ip.getText().toString();
        if(!new_ip.isEmpty()) {
            ip = new_ip;
            closeOutUDP();
            initOutUDP();
        }
    }

    private void turn_on_and_off_camera() {
        if(isCameraOpen) {
            closeCamera();
        }else {
            turn_on_camera();
        }
    }

    private void changeCameraView() {
        closeCamera();
        if(now_camera_type == CameraCharacteristics.LENS_FACING_BACK) {
            now_camera_type = CameraCharacteristics.LENS_FACING_FRONT;
        }else {
            now_camera_type = CameraCharacteristics.LENS_FACING_BACK;
        }
        turn_on_camera();
    }

    private boolean isFrontCamera() {
        if(now_camera_type == CameraCharacteristics.LENS_FACING_BACK) {
            return true;
        }else
            return false;
    }




    class ReceiveThread extends Thread {

        private boolean is_keep_running;

        private LinkedList<byte[]> linkedList;

        @Override
        public void run() {
            if(decoder == null) {
                initVideoDecode();
            }
            linkedList = new LinkedList<>();
            is_keep_running = true;
            while (is_keep_running) {
                try {
                    ByteBuffer a_part_of_buffer = ByteBuffer.allocate(65536);
                    SocketAddress socketAddress = in_datagram_channel.receive(a_part_of_buffer);
                    InetSocketAddress inetSocketAddress = (InetSocketAddress) socketAddress;
                    String host_name = null;
                    if(inetSocketAddress != null) {
                        host_name = inetSocketAddress.getHostName();
                    }
                    if(host_name != null && !host_name.equals(NetworkUtil.getMyIP(VideoCallTestActivity.this))){
                        a_part_of_buffer.flip();
                        byte [] bytes = new byte[a_part_of_buffer.remaining()];
                        a_part_of_buffer.get(bytes);
                        a_part_of_buffer.clear();
                        a_part_of_buffer = null;
                        boolean is_header = is_header(bytes);
                        if(is_header) {
                            int header_length = header.getBytes().length;
                            byte[] new_bytes = Arrays.copyOfRange(bytes,header_length,bytes.length);
                            bytes = null;
                            bytes = new_bytes;
                            new_bytes = null;
                        }
                        if(linkedList.size() >= 2) {
                            byte[] first_bytes = linkedList.removeFirst();
                            onDecodeData(first_bytes);
                            first_bytes = null;
                        }
                        linkedList.add(bytes);
                        //release
                        bytes = null;
                    }
                    if(a_part_of_buffer != null)
                        a_part_of_buffer.clear();
                    a_part_of_buffer = null;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            linkedList.clear();
        }

        private boolean is_header(byte[] bytes) {
            boolean is = false;
            byte[] header_data = header.getBytes();
            if(bytes.length > header_data.length) {
                boolean check = true;
                for (int i = 0 ; i < header_data.length ; i ++) {
                    byte h = header_data[i];
                    byte b = bytes[i];
                    if(h != b) {
                        check = false;
                        break;
                    }
                }
                if(check) {
                    is = true;
                }
            }
            return is;
        }

        public void free(){
            is_keep_running = false;
        }

    }
}
