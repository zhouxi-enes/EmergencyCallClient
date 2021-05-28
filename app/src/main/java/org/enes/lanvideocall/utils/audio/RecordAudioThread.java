package org.enes.lanvideocall.utils.audio;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.LinkedList;

public class RecordAudioThread extends Thread {

    private boolean isMute;

    public void setMute(boolean isMute) {
        this.isMute = isMute;
    }

    private int simple_rate_out_hz;

    private String send_to_address;

    private int send_to_port;

    private int buffer_size;

    private AudioRecord audio_record;

    private LinkedList<byte[]> linkedList;

//    private DatagramChannel datagramChannel;

    private DatagramSocket datagramSocket;

    public RecordAudioThread(String send_to_address,int send_to_port,int simple_rate_out_hz) {
        super();
        this.simple_rate_out_hz = simple_rate_out_hz;
        this.send_to_address = send_to_address;
        this.send_to_port = send_to_port;
    }

    private SocketAddress socketAddress;
    private void init() {
        try {
            //configure datagram
//            datagramChannel = DatagramChannel.open();
//            datagramChannel.configureBlocking(true);

            datagramSocket = new DatagramSocket();
            socketAddress = new InetSocketAddress(send_to_address, send_to_port);
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            //calculate buffer size
            buffer_size = AudioRecord.getMinBufferSize(simple_rate_out_hz, AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT);

            //allocate record audio
            audio_record = new AudioRecord(MediaRecorder.AudioSource.VOICE_COMMUNICATION, simple_rate_out_hz,
                    AudioFormat.CHANNEL_IN_STEREO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    buffer_size);

            linkedList = new LinkedList<>();

            //init socket

        }catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void free() {
        interrupt();
        if(!datagramSocket.isClosed())
            datagramSocket.close();
    }

    @Override
    public void run() {
        init();
        Log.e("test","recordAudioThread start:"+send_to_address);
//        ByteBuffer byteBuffer = ByteBuffer.allocate(buffer_size);
        audio_record.startRecording();
        while (!isInterrupted()) {
            byte[] tmp_buffer_now = new byte[buffer_size];
            audio_record.read(tmp_buffer_now,0,buffer_size);
            if(linkedList.size() >= 2) {
                byte[] first_bytes = linkedList.removeFirst();
//                for(int i=0;i<first_bytes.length;i++){
//                    first_bytes[i]= (byte) (first_bytes[i]*2); // add volume
//                }
//                Log.e("record_audio_thread","send data");
//                byteBuffer.put(first_bytes);
//                byteBuffer.flip();
                try {
                    DatagramPacket datagramPacket =
                            new DatagramPacket(first_bytes,0,first_bytes.length);
                    datagramPacket.setSocketAddress(socketAddress);
//                    Log.e("test","send audio");
                    if(!isMute) {
                        if(!datagramSocket.isClosed())
                            datagramSocket.send(datagramPacket);
                    }
//                    datagramChannel.send(byteBuffer,socketAddress);
                } catch (IOException e) {
                    e.printStackTrace();
                }
//                byteBuffer.clear();
                first_bytes = null;
            }
            linkedList.add(tmp_buffer_now);
        }
        audio_record.stop();
        audio_record = null;

//        try {
//            datagramChannel.disconnect();
//            datagramChannel.close();
//            datagramChannel = null;
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
        if(!datagramSocket.isClosed())
            datagramSocket.close();
        Log.e("test","recordAudioThreadFree");
    }



}
