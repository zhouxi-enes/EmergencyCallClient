package org.enes.lanvideocall.activities;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.PermissionChecker;

import org.enes.lanvideocall.R;
import org.enes.lanvideocall.application.MyApplication;
import org.enes.lanvideocall.network.ApplicationStartCheckVersionThread;
import org.enes.lanvideocall.network.UpdateUserListThread;
import org.enes.lanvideocall.utils.ManifestUtil;
import org.enes.lanvideocall.utils.NetworkUtil;
import org.enes.lanvideocall.utils.SharedPreferencesUtil;

import java.util.ArrayList;
import java.util.List;

public class InitActivity extends MyActivity implements
        ApplicationStartCheckVersionThread.ApplicationStartCheckVersionTCPThreadListener,
        UpdateUserListThread.UpdateUserListThreadListener {

    private boolean need_to_check_wifi;

    private boolean is_timer_out;

    private UpdateUserListThread userListThread;

    private String TAG = "InitActivity";

    private boolean isNewInstall, isPermissionGranted;

    private LinearLayout ll_not_new_install, ll_new_install;

    private TextView tv_starting;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        MyApplication.getInstance().cleanNotifications();
        if(MyApplication.getInstance().isCallingNow()) {
            CallActivity.gotoCallActivity(this);
            finish();
            return;
        }
        initView();
        initData();
    }

    private void initView() {
        setContentView(R.layout.activity_init);
        ll_not_new_install = findViewById(R.id.ll_not_new_install);
        ll_new_install = findViewById(R.id.ll_new_install);
        tv_starting = findViewById(R.id.tv_starting);
    }

    private void initData() {
        String string = SharedPreferencesUtil.getUUID();
        if(string == null) {
            isNewInstall = true;
        }else {
            isNewInstall = false;
        }
        if(checkPermissions()) {
            initSystem();
        }
    }

    public static final int INIT_ACTIVITY_CHECK_PERMISSIONS_CODE = 111;

    private boolean checkPermissions() {
        boolean isPermissionGranted = false;
        String [] all_permissions = ManifestUtil.getPermissionsFromManifest(this);
        if(all_permissions != null) {
            boolean needToGetPermission = false;
            List<String> denied_list = new ArrayList<>();
            List<String> denied_app_op_list = new ArrayList<>();
            for(int i = 0 ; i < all_permissions.length ; i ++) {
                String permission = all_permissions[i];
                int permission_state = PermissionChecker.checkSelfPermission(this,permission);
                if(permission_state != PermissionChecker.PERMISSION_GRANTED) {
                    if(permission_state == PermissionChecker.PERMISSION_DENIED) {
                        denied_list.add(permission);
                    }else if(permission_state == PermissionChecker.PERMISSION_DENIED_APP_OP) {
                        denied_app_op_list.add(permission);
                    }
                    needToGetPermission = true;
                }
            }
            if(needToGetPermission) {
                // check all denied permissions
                Log.e(TAG,denied_app_op_list.toString());
                Log.e(TAG,denied_list.toString());
                requestPermissions(denied_list.toArray(new String[0]),
                        INIT_ACTIVITY_CHECK_PERMISSIONS_CODE);
            }else {
                isPermissionGranted = true;
            }
        }
        return isPermissionGranted;
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case INIT_ACTIVITY_CHECK_PERMISSIONS_CODE:
                Log.e(TAG,permissions.length+","+grantResults.length);
                for(int i = 0 ; i < permissions.length ; i ++) {
                    String permission = permissions[i];
                    int result = grantResults[i];
                    Log.e(TAG,permission+","+result);
                }
                initSystem();
                break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    private ApplicationStartCheckVersionThread check_version_thread;

    private void initSystem() {
        ll_new_install.setVisibility(View.GONE);
        ll_not_new_install.setVisibility(View.VISIBLE);
        if(NetworkUtil.isConnectToWifi(this)) {
            need_to_check_wifi = false;
            if(check_version_thread != null) {
                check_version_thread.interrupt();
                check_version_thread = null;
            }
            check_version_thread = new ApplicationStartCheckVersionThread();
            check_version_thread.setListener(this);
            check_version_thread.start();
        }else {
            need_to_check_wifi = true;
            DialogInterface.OnClickListener onClickListener = new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    switch (which) {
                        case DialogInterface.BUTTON_NEUTRAL:
                            NetworkUtil.openWifiSettings(InitActivity.this);
                            break;
                        case DialogInterface.BUTTON_NEGATIVE:
                            finish();
                            break;
                        case DialogInterface.BUTTON_POSITIVE:
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    initSystem();
                                }
                            });
                            break;
                    }
                }
            };
            AlertDialog alertDialog = new AlertDialog.Builder(this)
                    .setTitle(getString(R.string.txt_not_connect_to_wifi_hint_title))
                    .setMessage(String.format("%s\n%s",
                            getString(R.string.txt_not_connect_to_wifi_hint_message),
                            getString(R.string.ap_name)))
                    .setNeutralButton(getString(R.string.txt_open_wifi_settings),onClickListener)
                    .setNegativeButton(getString(R.string.txt_exit),onClickListener)
                    .setPositiveButton(getString(R.string.txt_retry),onClickListener)
                    .create();
            alertDialog.setCancelable(false);
            alertDialog.show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if(requestCode  == NetworkUtil.REQUEST_CODE_OPEN_WIFI_SETTINGS) {
            initSystem();
        }else
            super.onActivityResult(requestCode, resultCode, data);
    }

    private void showNameInputDialog() {
        View dialog_view = LayoutInflater.from(this).
                inflate(R.layout.dialog_view_input_name,null);
        final EditText et_input_name = dialog_view.findViewById(R.id.et_input_name);
        DialogInterface.OnClickListener onClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case DialogInterface.BUTTON_POSITIVE:
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                checkInputName(et_input_name.getText().toString());
                            }
                        });
                        break;
                    case DialogInterface.BUTTON_NEGATIVE:
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                showConfirmExitDialog();
                            }
                        });
                        break;
                }
            }
        };
        AlertDialog alertDialog = new AlertDialog.Builder(this)
                .setTitle(getString(R.string.txt_input_name))
                .setPositiveButton(getString(R.string.txt_confirm),onClickListener)
                .setNegativeButton(getString(R.string.txt_exit),onClickListener)
                .setView(dialog_view)
                .create();
        alertDialog.setCancelable(false);
        alertDialog.show();
    }

    private void check_is_old_user() {
        Log.e(TAG,"initSystem,isNewInstall:"+isNewInstall);
        if(isNewInstall) {
            tv_starting.setText(getString(R.string.txt_set_name_background));
            showNameInputDialog();
        }else {
            Log.e("test","old_user");
            userListThread = new UpdateUserListThread();
            userListThread.setUpdateUserListThreadListener(this);
            userListThread.start();
        }
//        gotoMainActivity();
//        long delayMillis = Defines.HTTP_TIMEOUT;
//        if(!isNewInstall) {
//
//        }else {
//            ll_not_new_install.postDelayed(new Runnable() {
//                @Override
//                public void run() {
//                    if(!is_timer_out) {
//                        is_timer_out = true;
//                        if(isVersionChecked) {
//                            Log.e("test","test1");
//                            gotoMainActivity();
//                        }
//                    }
//                }
//            },delayMillis);
//        }
    }

    private String name_tmp;

    private void checkInputName(String name) {
        if(name.isEmpty()) {
            Toast.makeText(this,getString(R.string.txt_name_cannot_be_empty),
                    Toast.LENGTH_SHORT).show();
            check_is_old_user();
        }else {
            name_tmp = name;
            if(userListThread != null) {
                userListThread.isInterrupted();
            }
            userListThread = new UpdateUserListThread(name);
            userListThread.setUpdateUserListThreadListener(this);
            userListThread.start();
        }
    }

    private void showConfirmExitDialog() {
        DialogInterface.OnClickListener onClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case DialogInterface.BUTTON_POSITIVE:
                        finish();
                        break;
                    case DialogInterface.BUTTON_NEGATIVE:
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                check_is_old_user();
                            }
                        });
                        break;
                }
            }
        };
        AlertDialog alertDialog = new AlertDialog.Builder(this)
                .setTitle(getString(R.string.txt_warning))
                .setMessage(getString(R.string.txt_exit_app_confirm))
                .setPositiveButton(getString(R.string.txt_confirm),onClickListener)
                .setNegativeButton(getString(R.string.txt_back),onClickListener)
                .create();
        alertDialog.setCancelable(false);
        alertDialog.show();
    }

    private boolean isVersionChecked;

    @Override
    public void onVersionChecked() {
        isVersionChecked = true;
//        if(!is_timer_out) {
//            is_timer_out = true;
//            Log.e("test","test2");
//            gotoMainActivity();
//        }
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                check_is_old_user();
            }
        });
    }

    @Override
    public void onVersionCheckFailed() {
        Log.e("test","onVersionCheckFailed");
        isVersionChecked = false;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                serverDown();
            }
        });
    }

    private void serverDown () {
        if(NetworkUtil.isConnectToWifi(this)) {
            // handle server down
            DialogInterface.OnClickListener onClickListener = new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    switch (which) {
                        case DialogInterface.BUTTON_POSITIVE:
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    initSystem();
                                }
                            });
                        break;
                        case DialogInterface.BUTTON_NEGATIVE:
                            finish();
                        break;
                    }
                }
            };
            AlertDialog alertDialog = new AlertDialog.Builder(this)
                    .setTitle(getString(R.string.txt_warning))
                    .setMessage(getString(R.string.txt_server_down_hint))
                    .setPositiveButton(getString(R.string.txt_retry),onClickListener)
                    .setNegativeButton(getString(R.string.txt_exit),onClickListener)
                    .create();
            alertDialog.setCancelable(false);
            alertDialog.show();
        }else {
            initSystem();
        }
    }

    @Override
    public void onUpdateUserListSuccess(UpdateUserListThread which) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                gotoMainActivity();
            }
        });
    }

    private void gotoMainActivity() {
        UserListActivity.gotoUserListActivity(this);
        finish();
    }

    @Override
    public void onUpdateUserListFailed(UpdateUserListThread which) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                update_user_list_failed();
            }
        });
    }

    private void update_user_list_failed() {
        if(NetworkUtil.isConnectToWifi(this)) {
            showNameInputDialog();
        }else {
            DialogInterface.OnClickListener onClickListener = new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    switch (which) {
                        case DialogInterface.BUTTON_NEUTRAL:
                            NetworkUtil.openWifiSettings(InitActivity.this);
                            break;
                        case DialogInterface.BUTTON_NEGATIVE:
                            finish();
                            break;
                        case DialogInterface.BUTTON_POSITIVE:
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    update_user_name_retry();
                                }
                            });
                            break;
                    }
                }
            };
            AlertDialog alertDialog = new AlertDialog.Builder(this)
                    .setTitle(getString(R.string.txt_not_connect_to_wifi_hint_title))
                    .setMessage(String.format("%s\n%s",
                            getString(R.string.txt_not_connect_to_wifi_hint_message),
                            getString(R.string.ap_name)))
                    .setNeutralButton(getString(R.string.txt_open_wifi_settings),onClickListener)
                    .setNegativeButton(getString(R.string.txt_exit),onClickListener)
                    .setPositiveButton(getString(R.string.txt_retry),onClickListener)
                    .create();
            alertDialog.setCancelable(false);
            alertDialog.show();
        }
    }

    private void update_user_name_retry() {
        if(name_tmp != null) {
            checkInputName(name_tmp);
        }else {
            check_is_old_user();
        }

    }
}
