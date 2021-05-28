package org.enes.lanvideocall.network;

import android.content.Intent;

import com.google.gson.Gson;

import org.enes.lanvideocall.application.MyApplication;
import org.enes.lanvideocall.pojos.User;
import org.enes.lanvideocall.pojos.UserListReturnPOJO;
import org.enes.lanvideocall.utils.Defines;
import org.enes.lanvideocall.utils.ErrorCodeDefines;
import org.enes.lanvideocall.utils.HttpUtils;
import org.enes.lanvideocall.utils.SharedPreferencesUtil;
import org.enes.lanvideocall.utils.UserListUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.List;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okio.BufferedSink;

public class GetAllUserListThread extends Thread {

    public GetAllUserListThread() {
        super();
    }

    @Override
    public void run() {
        if(!isInterrupted()) {
            OkHttpClient okHttpClient = HttpUtils.getHttpClient();
            String url = HttpUtils.getFullAPIAddress(Defines.http_api_get_user_list);
            RequestBody requestBody = new RequestBody() {
                @Nullable
                @Override
                public MediaType contentType() {
                    return MediaType.get("text/html");
                }

                @Override
                public void writeTo(@NotNull BufferedSink bufferedSink) throws IOException {

                }
            };
            Request request = HttpUtils.getRequest(url,requestBody);
            try {
                Response response = okHttpClient.newCall(request).execute();
                String return_str = response.body().string();
                Gson gson = new Gson();
                UserListReturnPOJO userListReturnPOJO =
                        gson.fromJson(return_str, UserListReturnPOJO.class);
                if(userListReturnPOJO.status == ErrorCodeDefines.STATUS_OK) {
                    List<User> userList = userListReturnPOJO.users;
                    onSuccessful(userList);
                }else {
                    onFailed();
                }
            } catch (IOException e) {
                onFailed();
                e.printStackTrace();
            }
        }
    }

    private void onSuccessful(List<User> users) {
        //remove self
        String my_uuid = SharedPreferencesUtil.getUUID();
        for(int i = users.size() - 1 ; i >= 0 ; i--) {
            User user = users.get(i);
            if(user.uuid.equals(my_uuid)) {
                users.remove(i);
                break;
            }
        }
        UserListUtil.getInstance().updateList(users);
        sendBroadcast(true);
    }

    private void onFailed() {
        sendBroadcast(false);
    }

    public static final String ACTION_USER_LIST_UPDATE_SUCCESS = "ACTION_USER_LIST_UPDATE_SUCCESS";

    public static final String ACTION_USER_LIST_UPDATE_FAILED = "ACTION_USER_LIST_UPDATE_FAILED";

    private void sendBroadcast(boolean isSuccess) {
        Intent intent = new Intent();
        String str = null;
        if(isSuccess) {
            str = ACTION_USER_LIST_UPDATE_SUCCESS;
        }else {
            str = ACTION_USER_LIST_UPDATE_FAILED;
        }
        intent.setAction(str);
        MyApplication.getInstance().sendBroadcast(intent);
    }

}
