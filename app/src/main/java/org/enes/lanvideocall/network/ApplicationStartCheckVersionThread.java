package org.enes.lanvideocall.network;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import com.google.gson.Gson;

import org.enes.lanvideocall.application.MyApplication;
import org.enes.lanvideocall.pojos.NumberOnlyReturnPOJO;
import org.enes.lanvideocall.utils.Defines;
import org.enes.lanvideocall.utils.ErrorCodeDefines;
import org.enes.lanvideocall.utils.HttpUtils;

import java.io.IOException;
import java.nio.charset.Charset;

import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class ApplicationStartCheckVersionThread extends Thread{

    private ApplicationStartCheckVersionTCPThreadListener listener;

    public interface ApplicationStartCheckVersionTCPThreadListener {

        void onVersionChecked();

        void onVersionCheckFailed();

    }

    public void setListener(ApplicationStartCheckVersionTCPThreadListener l) {
        this.listener = l;
    }

    public ApplicationStartCheckVersionThread() {
        super();
    }

    @Override
    public void run() {
        String package_name = MyApplication.getInstance().getPackageName();
        String version_name = null;
        try {
            PackageInfo packageInfo = MyApplication.getInstance().getPackageManager().
                    getPackageInfo(package_name, 0);
            version_name = packageInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        //
        OkHttpClient okHttpClient = HttpUtils.getHttpClient();
        String url = HttpUtils.getFullAPIAddress(Defines.http_api_check_version);
        FormBody formBody = new FormBody.Builder(Charset.forName("utf-8"))
                .addEncoded("pkg_name",package_name)
                .addEncoded("v_name",version_name)
                .build();
        Request request = HttpUtils.getRequest(url,formBody);
        try {
            Response response = okHttpClient.newCall(request).execute();
            String return_str = response.body().string();
            if(return_str.isEmpty()) {
                common_error();
            }else {
                NumberOnlyReturnPOJO pojo =
                        new Gson().fromJson(return_str, NumberOnlyReturnPOJO.class);
                int status = pojo.status;
                if(status == ErrorCodeDefines.STATUS_OK) {
                    finish();
                }else {
                    common_error();
                }
            }
        } catch (IOException e) {
            network_error();
            e.printStackTrace();
        }
    }

    private void finish() {
        if(listener != null) {
            listener.onVersionChecked();
        }
    }

    private void common_error() {
        if(listener != null) {
            listener.onVersionCheckFailed();
        }
    }

    private void network_error() {
        if(listener != null) {
            listener.onVersionCheckFailed();
        }
    }

}
