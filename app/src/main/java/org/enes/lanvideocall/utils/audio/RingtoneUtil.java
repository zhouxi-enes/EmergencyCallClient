package org.enes.lanvideocall.utils.audio;

import android.content.Context;
import android.media.AudioManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;

public class RingtoneUtil {

    private static RingtoneUtil ringtoneUtil;

    public static RingtoneUtil getInstance() {
        if(ringtoneUtil == null) {
            ringtoneUtil = new RingtoneUtil();
        }
        return ringtoneUtil;
    }

    private RingtoneUtil() {
        super();
    }

    private boolean isPlayingRingtoneNow;

    public boolean isPlaying() {
        return isPlayingRingtoneNow;
    }

    public void play(Context context) {
        if(isPlayingRingtoneNow) {
            stop();
        }
        startPlayRingtone(context);
        isPlayingRingtoneNow = true;
    }

    public void stop() {
        if(isPlayingRingtoneNow) {
            stopPlayRingtone();
        }
    }

    private Ringtone ringtone;
    Vibrator vibrator;

    private void startPlayRingtone(Context ctx) {
        if(ringtone != null) {
            stopPlayRingtone();
        }
        Uri ringtone_uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
        ringtone = RingtoneManager.getRingtone(ctx,ringtone_uri);
        ringtone.play();

        // check is mute mode or not
        AudioManager audioManager = (AudioManager)ctx.getSystemService(Context.AUDIO_SERVICE);
        int ringerMode = audioManager.getRingerMode();
        vibrator = (Vibrator) ctx.getSystemService(Context.VIBRATOR_SERVICE);
        if(vibrator.hasVibrator()) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                VibrationEffect vibrationEffect =
                        VibrationEffect.createWaveform(new long[]{500, 100, 500, 100, 500, 100},
                                0);
                if(ringerMode != AudioManager.RINGER_MODE_SILENT) {
                    vibrator.vibrate(vibrationEffect);
                }
            }
        }
    }

    private void stopPlayRingtone() {
        if(ringtone != null) {
            ringtone.stop();
            ringtone = null;
            if(vibrator.hasVibrator()) {
                vibrator.cancel();
            }
        }
    }

}
