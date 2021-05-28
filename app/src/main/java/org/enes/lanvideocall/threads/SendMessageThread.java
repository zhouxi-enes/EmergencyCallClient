package org.enes.lanvideocall.threads;

import android.util.Log;

import org.enes.lanvideocall.utils.Defines;
import org.enes.lanvideocall.utils.HttpUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class SendMessageThread extends MyThread {

    public interface SendMessageInterface {

        void onSendMessageInterfaceSuccessful();

        void onSendMessageInterfaceFailed();

    }

    private SendMessageInterface listener;

    public void setListener(SendMessageInterface new_listener) {
        this.listener = new_listener;
    }

    private RequestBody content;

    private String api_name;

    public SendMessageThread(RequestBody content, String api_name) {
        super();
        this.content = content;
        this.api_name = api_name;
    }

    @Override
    public void run() {
        if(!isInterrupted()) {
            String url = String.format("%s%s", Defines.MESSAGE_SERVER_ADDRESS,api_name);
            Request request = HttpUtils.getRequest(url,content);
            OkHttpClient okHttpClient = new OkHttpClient.Builder()
                    .connectTimeout(10 * 1000, TimeUnit.MILLISECONDS)
                    .build();
            try {
                Response response = okHttpClient.newCall(request).execute();
                String return_str = response.body().string();
                JSONObject jsonObject = new JSONObject(return_str);
                int code = jsonObject.getInt("code");
                if(code == 200) {
                    onSuccessful();
                }else {
                    onFailed();
                }
            } catch (IOException e) {
                onFailed();
                e.printStackTrace();
            } catch (JSONException e) {
                onFailed();
                e.printStackTrace();
            }
        }
    }

    private void onSuccessful() {
        if(listener != null) {
            listener.onSendMessageInterfaceSuccessful();
        }
    }

    private void onFailed() {
        if(listener != null) {
            listener.onSendMessageInterfaceFailed();
        }
    }

}
