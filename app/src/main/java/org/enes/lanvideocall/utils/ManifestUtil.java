package org.enes.lanvideocall.utils;

import android.content.Context;
import android.content.pm.PackageManager;

public class ManifestUtil {

    /**
     * Read Permissions From AndroidManifest.xml
     * @param context
     * @return
     */
    public static String[] getPermissionsFromManifest(Context context) {
        try {
            return context
                    .getPackageManager()
                    .getPackageInfo(context.getPackageName(), PackageManager.GET_PERMISSIONS)
                    .requestedPermissions;
        }catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

}
