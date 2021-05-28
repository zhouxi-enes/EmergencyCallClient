package org.enes.lanvideocall.network;

import android.content.SharedPreferences;

import com.google.gson.Gson;

import org.enes.lanvideocall.application.MyApplication;
import org.enes.lanvideocall.pojos.NumberOnlyReturnPOJO;
import org.enes.lanvideocall.utils.Defines;
import org.enes.lanvideocall.utils.ErrorCodeDefines;
import org.enes.lanvideocall.utils.HttpUtils;
import org.enes.lanvideocall.utils.NetworkUtil;
import org.enes.lanvideocall.utils.SharedPreferencesUtil;

import java.io.IOException;
import java.nio.charset.Charset;

import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class UpdateUserListThread extends Thread{

    public interface UpdateUserListThreadListener {

        void onUpdateUserListSuccess(UpdateUserListThread which);

        void onUpdateUserListFailed(UpdateUserListThread which);

    }

    private UpdateUserListThreadListener listener;

    public void setUpdateUserListThreadListener(UpdateUserListThreadListener new_listener) {
        listener = new_listener;
    }

    private String name;

    private String uuid;

    public UpdateUserListThread(String name) {
        super();
        init(name);
    }

    public UpdateUserListThread() {
        super();
        init(null);
    }

    private void init(String name) {
        if(name != null) {
            // new
            this.name = name;
        }
    }

    @Override
    public void run() {
        if (!isInterrupted()) {
            // if is new user, generate a new uuid for it
            if(name != null) {
                // new
                uuid = SharedPreferencesUtil.createNewUUID();
            }else {
                SharedPreferences sharedPreferences = SharedPreferencesUtil.getSharedPreferences();
                name = sharedPreferences.getString(SharedPreferencesUtil.KEY_NAME,null);
                uuid = sharedPreferences.getString(SharedPreferencesUtil.KEY_UUID,null);
            }
            OkHttpClient okHttpClient = HttpUtils.getHttpClient();
            String url = HttpUtils.getFullAPIAddress(Defines.http_api_add_user);
            FormBody formBody = new FormBody.Builder(Charset.forName("utf-8"))
                    .addEncoded("name",name)
                    .addEncoded("uuid",uuid)
                    .addEncoded("ip", NetworkUtil.getMyIP(MyApplication.getInstance()))
                    .build();
            Request request = HttpUtils.getRequest(url,formBody);
            try {
                Response response = okHttpClient.newCall(request).execute();
                String return_str = response.body().string();
                if(return_str.isEmpty()) {
                    other_error();
                }else {
                    NumberOnlyReturnPOJO pojo =
                            new Gson().fromJson(return_str, NumberOnlyReturnPOJO.class);
                    int status = pojo.status;
                    if(status == ErrorCodeDefines.STATUS_OK) {
                        finish();
                    }else {
                        other_error();
                    }
                }
            } catch (IOException e) {
                network_error();
                e.printStackTrace();
            }

        }
    }

    private void finish() {
        SharedPreferences sharedPreferences =
                SharedPreferencesUtil.getSharedPreferences();
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(SharedPreferencesUtil.KEY_UUID,uuid);
        editor.putString(SharedPreferencesUtil.KEY_NAME,name);
        editor.apply();
        if(listener != null) {
            listener.onUpdateUserListSuccess(this);
        }
    }

    private void network_error() {
        if(listener != null) {
            listener.onUpdateUserListFailed(this);
        }
    }

    private void other_error() {
        if(listener != null) {
            listener.onUpdateUserListFailed(this);
        }
    }

}
