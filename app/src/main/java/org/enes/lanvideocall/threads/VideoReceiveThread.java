package org.enes.lanvideocall.threads;

import android.graphics.SurfaceTexture;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.util.Log;
import android.view.Surface;

import org.enes.lanvideocall.activities.CallActivity;
import org.enes.lanvideocall.utils.Defines;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.util.Arrays;
import java.util.LinkedList;

public class VideoReceiveThread extends MyThread {

    public CallActivity callActivity;

    public VideoReceiveThread(CallActivity callActivity) {
        super();
        this.callActivity = callActivity;
    }

    private LinkedList<byte[]> linkedList;

    @Override
    public void run() {
        if(decoder == null) {
            initVideoDecode();
        }
        linkedList = new LinkedList<>();
        while (!isInterrupted() && callActivity.in_datagram_channel != null
            && callActivity.in_datagram_channel.isOpen()) {
            try {
                ByteBuffer a_part_of_buffer = ByteBuffer.allocate(65536);
                SocketAddress socketAddress = callActivity.in_datagram_channel.receive(a_part_of_buffer);
                InetSocketAddress inetSocketAddress = (InetSocketAddress) socketAddress;
                String host_name = null;
                if(inetSocketAddress != null) {
                    host_name = inetSocketAddress.getHostName();
                }
                if(host_name != null){
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
        decoder.stop();
    }

    public static final String header = "264_h";

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
        if(!isInterrupted()) {
            if(decoder != null) {
                decoder.stop();
            }
            interrupt();
        }
    }

    private MediaCodec decoder;

    private void initVideoDecode() {
        try {
            decoder = MediaCodec.createDecoderByType(MediaFormat.MIMETYPE_VIDEO_AVC);
            MediaFormat mediaFormat = MediaFormat.
                    createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC,
                            CallActivity.VIDEO_SCREEN_WIDTH,CallActivity.VIDEO_SCREEN_HEIGHT);
            mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT,
                    MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible);
//            mediaFormat.setInteger(MediaFormat.KEY_ROTATION,getCameraRotate());

            SurfaceTexture surfaceTexture = callActivity.tv_friend_view.getSurfaceTexture();
//            surfaceTexture.setDefaultBufferSize(encoded_width, encoded_height);
            Surface surface = new Surface(surfaceTexture);

            decoder.configure(mediaFormat, surface, null, 0);
            decoder.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();

    private void onDecodeData(byte[] codeData) {
        if(decoder != null && codeData != null) {
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

}
