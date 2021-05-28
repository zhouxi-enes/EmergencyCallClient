package org.enes.lanvideocall.services;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

public class BackgroundService extends Service {

    private static final String TAG = "BackgroundService";

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        Log.e(TAG,"onBind");
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.e(TAG,"onCreate");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.e(TAG,"onStartCommand");
        return super.onStartCommand(intent, flags, startId);
    }

}
