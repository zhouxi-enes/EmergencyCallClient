package org.enes.lanvideocall.utils;

import java.util.Locale;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;

public class HttpUtils {

    public static String getFullAPIAddress(String api_address) {
        return String.format(Locale.getDefault(),
                "http://%s:%d/%s/%s",Defines.LAN_SERVER_ADDRESS,
                Defines.LAN_SERVER_PORT,Defines.LAN_SERVER_WEB_SERVICE_NAME,api_address);
    }

    public static OkHttpClient getHttpClient() {
        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .connectTimeout(Defines.HTTP_TIMEOUT, TimeUnit.MILLISECONDS)
                .build();
        return okHttpClient;
    }

    public static Request getRequest(String url, RequestBody requestBody) {
        Request request = new Request.Builder()
                .url(url)
                .post(requestBody)
                .build();
        return request;
    }
}
