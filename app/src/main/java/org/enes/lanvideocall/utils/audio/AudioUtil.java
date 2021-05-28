package org.enes.lanvideocall.utils.audio;

import org.enes.lanvideocall.utils.Defines;

public class AudioUtil {

    private static AudioUtil audioUtil;

    public static AudioUtil getInstance() {
        if(audioUtil == null) {
            audioUtil = new AudioUtil();
        }
        return audioUtil;
    }

    private AudioUtil(){
        super();
    }

    private PlayAudioThread playAudioThread;

    private RecordAudioThread recordAudioThread;

    public void startAudioService(String ip) {
        stopAudioService();
        int sample_rate = Defines.AUDIO_SAMPLE_RATE;
        int port = Defines.AUDIO_SERVER_PORT;
        playAudioThread = new PlayAudioThread(port,sample_rate);
        playAudioThread.start();
        recordAudioThread = new RecordAudioThread(ip,port,sample_rate);
        recordAudioThread.start();
    }

    public void stopAudioService() {
        if(playAudioThread != null) {
            playAudioThread.free();
            playAudioThread = null;
        }
        if(recordAudioThread != null) {
            recordAudioThread.free();
            recordAudioThread = null;
        }
    }

    public void setMute(boolean isMute) {
        if(recordAudioThread != null) {
            recordAudioThread.setMute(isMute);
        }
    }


}
