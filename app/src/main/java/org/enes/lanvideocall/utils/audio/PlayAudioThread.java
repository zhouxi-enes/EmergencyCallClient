package org.enes.lanvideocall.utils.audio;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.util.Log;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Iterator;

/**
 * this thread is used as play income audio stream
 */
public class PlayAudioThread extends Thread {

    public interface PlayAudioThreadCallback {

        void onPlayAudioThreadReceivedData(PlayAudioThread which);

    }

    private PlayAudioThreadCallback playAudioThreadCallback;

    public void setPlayAudioThreadCallback(PlayAudioThreadCallback playAudioThreadCallback) {
        this.playAudioThreadCallback = playAudioThreadCallback;
    }

    public SocketAddress socketAddress;

    private int listen_port;

    private AudioTrack audio_track;

    private int sample_rate_hz;

    private DatagramSocket datagramSocket;

    public PlayAudioThread(int listen_port,int sample_rate_hz) {
        super();
        this.listen_port = listen_port;
        this.sample_rate_hz = sample_rate_hz;
    }

    private void init() {
        try {
            datagramSocket = new DatagramSocket(new InetSocketAddress(listen_port));
        } catch (SocketException e) {
            e.printStackTrace();
        }

        try {
            //calculate the buffer size
            int buffer_size = AudioTrack.getMinBufferSize(sample_rate_hz, AudioFormat.CHANNEL_IN_STEREO,
                    AudioFormat.ENCODING_PCM_16BIT);

            //allocate audio track
            audio_track = new AudioTrack(AudioManager.STREAM_VOICE_CALL, sample_rate_hz,
                    AudioFormat.CHANNEL_IN_STEREO, AudioFormat.ENCODING_PCM_16BIT, buffer_size,
                    AudioTrack.MODE_STREAM);
            audio_track.setVolume(audio_track.getMaxVolume());
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
        Log.e("play_audio_thread","playAudioThread start");
        audio_track.play();
        while (!isInterrupted() && !datagramSocket.isClosed()) {
            byte[] receiveData = new byte[65536];
            DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
            if(!datagramSocket.isClosed()) {
                try {
                    datagramSocket.receive(receivePacket);
                }catch (Exception e) {
                    e.printStackTrace();
                }
                audio_track.write(receiveData, 0, receivePacket.getLength());
            }
            receiveData = null;
        }
        audio_track.stop();
        audio_track = null;
        if(!datagramSocket.isClosed())
            datagramSocket.close();
        Log.e("test","playAudioThread free");
    }
}
