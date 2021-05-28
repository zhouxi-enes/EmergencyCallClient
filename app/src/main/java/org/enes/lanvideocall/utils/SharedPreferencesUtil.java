package org.enes.lanvideocall.utils;

import android.content.Context;
import android.content.SharedPreferences;

import org.enes.lanvideocall.application.MyApplication;

import java.util.UUID;

public class SharedPreferencesUtil {

    public static final String KEY_UUID = "uuid";

    public static final String KEY_NAME = "name";

    /**
     * Read Device UUID From Disk
     * @return
             */
    public static String getUUID() {
        SharedPreferences sharedPreferences = getSharedPreferences();
        return sharedPreferences.getString(KEY_UUID,null);
    }

    private static final String SHARED_PREFERENCES_NAME_UUID = "SHARED_PREFERENCES_NAME_UUID";

    /**
     * Get SharedPreferences From Disk
     * @return
     */
    public static SharedPreferences getSharedPreferences() {
        return MyApplication.getInstance().
                getSharedPreferences(SHARED_PREFERENCES_NAME_UUID, Context.MODE_PRIVATE);
    }

    public static String createNewUUID() {
        String uuid = UUID.randomUUID().toString();
        return uuid;
    }

}
