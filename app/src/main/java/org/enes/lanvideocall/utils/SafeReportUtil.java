package org.enes.lanvideocall.utils;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.location.Location;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import org.enes.lanvideocall.R;
import org.enes.lanvideocall.threads.SendMessageThread;
import org.enes.lanvideocall.utils.location.GetLocationThread;

import okhttp3.FormBody;
import okhttp3.RequestBody;

public class SafeReportUtil implements GetLocationThread.GetLocationListener,
        SendMessageThread.SendMessageInterface {

    private GetLocationThread getLocationThread;

    private String name;

    public SafeReportUtil() {
        super();
        name = SharedPreferencesUtil.getSharedPreferences().
                getString(SharedPreferencesUtil.KEY_NAME,null);
    }

    private AlertDialog messageAlertDialog;

    public void showSendMessageDialog(Context context) {
        last_context = context;
        if(messageAlertDialog != null) {
            messageAlertDialog.cancel();
            messageAlertDialog = null;
        }
        View view = LayoutInflater.from(context).inflate(R.layout.view_message_dialog,null);
        final EditText et_message = view.findViewById(R.id.et_message);
        DialogInterface.OnClickListener onClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if(which == DialogInterface.BUTTON_POSITIVE) {
                    String message = et_message.getText().toString().trim();
                    sendMessage(message);
                }
            }
        };
        messageAlertDialog = new AlertDialog.Builder(context)
                .setTitle(context.getString(R.string.txt_compose))
                .setView(view)
                .setPositiveButton(context.getString(R.string.txt_send),onClickListener)
                .setNegativeButton(context.getString(R.string.txt_back),onClickListener)
                .create();

        messageAlertDialog.show();
    }

    private AlertDialog waitingProgressDialog;

    private Context last_context;

    public void showSafeReportDialog(Context context) {
        last_context = context;
        if(waitingProgressDialog != null) {
            waitingProgressDialog.dismiss();
            waitingProgressDialog = null;
        }
        View view = LayoutInflater.from(context).inflate(R.layout.view_waiting_dialog,null);
        TextView tv_message = view.findViewById(R.id.tv_message);
        tv_message.setText(context.getString(R.string.txt_check_location));
        DialogInterface.OnClickListener onClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (which == DialogInterface.BUTTON_POSITIVE) {
                    if(getLocationThread != null) {
                        getLocationThread.stop();
                    }
                }
            }
        };
        waitingProgressDialog = new AlertDialog.Builder(context)
                .setCancelable(false)
                .setView(view)
                .setPositiveButton(context.getString(R.string.txt_back),onClickListener)
                .create();
        if(getLocationThread != null) {
            getLocationThread.stop();
            getLocationThread = null;
        }
        getLocationThread = new GetLocationThread(waitingProgressDialog);
        getLocationThread.setLocationListener(this);
        waitingProgressDialog.show();
        getLocationThread.start();
    }


    @Override
    public void onGotLocation(Location location) {
        if(location != null) {
            sendIsSafe(location);
        }
    }

    private void sendIsSafe(Location location) {
        Log.e("test",location.toString());
        FormBody formBody = new FormBody.Builder()
                .addEncoded("name",name)
                .addEncoded("lng",location.getLongitude()+"")
                .addEncoded("lat",location.getLatitude()+"")
                .build();
        upload(last_context,formBody,Defines.API_LOCATION);
    }

    private void sendMessage(String string) {
        if(string.length() > 0) {
            FormBody formBody = new FormBody.Builder()
                    .addEncoded("name",name)
                    .addEncoded("message",string)
                    .build();
            upload(last_context,formBody,Defines.API_MESSAGE);
        }else {
            Toast.makeText(last_context,last_context.getString(R.string.txt_input_the_message),
                    Toast.LENGTH_SHORT).show();
            showSendMessageDialog(last_context);
        }
    }

    private AlertDialog progressAlertDialog;

    private SendMessageThread sendMessageThread;

    private void upload(Context context, RequestBody content, String API) {
        if(progressAlertDialog != null) {
            progressAlertDialog.cancel();
            progressAlertDialog = null;
        }
        View view = LayoutInflater.from(context).inflate(R.layout.view_waiting_dialog,null);
        TextView tv_message = view.findViewById(R.id.tv_message);
        tv_message.setText(context.getString(R.string.txt_data_sending_now));
        progressAlertDialog = new AlertDialog.Builder(context)
                .setCancelable(false)
                .setView(view)
                .create();
        if(sendMessageThread != null) {
            sendMessageThread.kill();
            sendMessageThread = null;
        }
        sendMessageThread = new SendMessageThread(content,API);
        sendMessageThread.setListener(this);
        sendMessageThread.start();
        progressAlertDialog.show();
    }

    @Override
    public void onSendMessageInterfaceSuccessful() {
        closeAlertDialog();
        if(last_context != null && last_context instanceof Activity) {
            final Activity activity = (Activity) last_context;
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(activity,activity.getString(R.string.txt_data_sent_successful)
                            ,Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    @Override
    public void onSendMessageInterfaceFailed() {
        closeAlertDialog();
        if(last_context != null && last_context instanceof Activity) {
            final Activity activity = (Activity) last_context;
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(activity,activity.getString(R.string.txt_data_failed)
                            ,Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void closeAlertDialog() {
        if(progressAlertDialog != null) {
            progressAlertDialog.dismiss();
            progressAlertDialog.cancel();
            progressAlertDialog = null;
        }
    }

}
