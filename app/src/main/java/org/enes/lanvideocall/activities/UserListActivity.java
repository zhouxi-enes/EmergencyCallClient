package org.enes.lanvideocall.activities;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.telecom.Call;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.robertlevonyan.views.customfloatingactionbutton.FloatingActionButton;

import org.enes.lanvideocall.R;
import org.enes.lanvideocall.adapters.SelectCallMethodAdapter;
import org.enes.lanvideocall.adapters.UserListAdapter;
import org.enes.lanvideocall.application.MyApplication;
import org.enes.lanvideocall.network.GetAllUserListThread;
import org.enes.lanvideocall.pojos.User;
import org.enes.lanvideocall.utils.NetworkUtil;
import org.enes.lanvideocall.utils.SafeReportUtil;
import org.enes.lanvideocall.utils.UserListUtil;
import org.enes.lanvideocall.views.SelectMethodToCallUserBottomSheetDialog;

import java.util.Timer;
import java.util.TimerTask;

public class UserListActivity extends MyActivity implements SwipeRefreshLayout.OnRefreshListener,
    UserListAdapter.UserListAdapterListener,
        SelectMethodToCallUserBottomSheetDialog.SelectMethodToCallUserBottomSheetDialogListener,
        FloatingActionButton.OnClickListener{

    public static void gotoUserListActivity(Activity activity) {
        Intent intent = new Intent(activity,UserListActivity.class);
        activity.startActivity(intent);
    }

    private boolean isGetUserListNow;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initView();
        initBroadcastReceiver();
        onRefresh();
    }

    @Override
    protected void onDestroy() {
        removeBroadcastReceiver();
        super.onDestroy();
    }

    private SwipeRefreshLayout refresh_layout;

    private RecyclerView recycler_view;

    private UserListAdapter userListAdapter;

    private TextView tv_list_empty;

    private FloatingActionButton fab,fab_message;

    private void initView() {
        setContentView(R.layout.activity_user_list);
        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle(getString(R.string.app_name));
        setSupportActionBar(toolbar);
        refresh_layout = findViewById(R.id.refresh_layout);
        refresh_layout.setColorSchemeColors(
                ContextCompat.getColor(this,R.color.colorAccent),
                ContextCompat.getColor(this,R.color.colorPrimaryDark));
        refresh_layout.setOnRefreshListener(this);
        recycler_view = findViewById(R.id.recycler_view);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        recycler_view.setLayoutManager(linearLayoutManager);
        userListAdapter = new UserListAdapter(this);
        userListAdapter.setUserListAdapterListener(this);
        recycler_view.setAdapter(userListAdapter);
        tv_list_empty = findViewById(R.id.tv_list_empty);
        tv_list_empty.setVisibility(View.GONE);
        fab = findViewById(R.id.fab_safe);
        fab.setOnClickListener(this);
        fab_message = findViewById(R.id.fab_message);
        fab_message.setOnClickListener(this);
    }

    private BroadcastReceiver broadcastReceiver;

    private void initBroadcastReceiver() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(GetAllUserListThread.ACTION_USER_LIST_UPDATE_SUCCESS);
        intentFilter.addAction(GetAllUserListThread.ACTION_USER_LIST_UPDATE_FAILED);
        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if(action.equals(GetAllUserListThread.ACTION_USER_LIST_UPDATE_SUCCESS)) {
                    onUserListRefreshSuccessful();
                }else if(action.equals(GetAllUserListThread.ACTION_USER_LIST_UPDATE_FAILED)) {
                    onUserListRefreshFailed();
                }
            }
        };
        registerReceiver(broadcastReceiver,intentFilter);
    }

    private void removeBroadcastReceiver() {
        if(broadcastReceiver!= null) {
            unregisterReceiver(broadcastReceiver);
        }
    }

    @Override
    public void onRefresh() {
        isGetUserListNow = true;
        // todo refresh
        if(NetworkUtil.isConnectToWifi(this)) {
            getUserList();
        }else {
            if(refresh_layout.isRefreshing()) {
                refresh_layout.setRefreshing(false);
            }
            isGetUserListNow = false;
        }
    }

    private void getUserList() {
        MyApplication.getInstance().updateUserList();
    }

    private void onUserListRefreshSuccessful() {
        Log.e("test","onUserListRefreshSuccessful");
        if(refresh_layout.isRefreshing()) {
            refresh_layout.setRefreshing(false);
        }
        if(userListAdapter != null) {
            userListAdapter.refresh();
        }
        if(userListAdapter != null &&
                userListAdapter.getItemCount() == 0) {
            if(tv_list_empty.getVisibility() != View.VISIBLE) {
                tv_list_empty.setVisibility(View.VISIBLE);
            }
        }else {
            if(tv_list_empty.getVisibility() != View.GONE) {
                tv_list_empty.setVisibility(View.GONE);
            }
        }
        isGetUserListNow = false;
    }

    private void onUserListRefreshFailed() {
        Log.e("test","onUserListRefreshFailed");
        if(refresh_layout.isRefreshing()) {
            refresh_layout.setRefreshing(false);
        }
        isGetUserListNow = false;

    }

    private int press_count;

    @Override
    public void onBackPressed() {
        Toast.makeText(this,getString(R.string.txt_2_press),Toast.LENGTH_SHORT).show();
        press_count ++;
        if(press_count >= 2) {
            super.onBackPressed();
        }
        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                press_count = 0;
            }
        };
        Timer timer = new Timer();
        timer.schedule(timerTask,1000 * 2);
    }

    @Override
    public void onUserListAdapterListenerPress(UserListAdapter adapter, int position) {
        if(adapter == userListAdapter) {
            User user = UserListUtil.getInstance().getUsers().get(position);
            position_tmp = position;
            showSelectCallMethodToUser(user);
        }
    }

    private User wait_select_user;
    private int position_tmp;

    private void showSelectCallMethodToUser(User user) {
        wait_select_user = user;
        SelectMethodToCallUserBottomSheetDialog selectMethodToCallUserBottomSheetDialog =
                new SelectMethodToCallUserBottomSheetDialog();
        selectMethodToCallUserBottomSheetDialog.
                setSelectMethodToCallUserBottomSheetDialogListener(this);
        selectMethodToCallUserBottomSheetDialog.setTitle(
                String.format(getString(R.string.txt_select_a_method_to_call_),user.name));
        selectMethodToCallUserBottomSheetDialog.show(getSupportFragmentManager(),user.name);
    }

    @Override
    public void onSelectMethodToCallUserBottomSheetDialogPressed(int position) {
//        Toast.makeText(UserListActivity.this,"ip"+wait_select_user.ip,Toast.LENGTH_SHORT).show();
        int color = userListAdapter.colorList.get(position_tmp);
        MyApplication.getInstance().startCallingNow(wait_select_user,color,position);
        wait_select_user = null;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.fab_safe:
                showReportSafeDialog();
                break;
            case R.id.fab_message:
                showSendMessageDialog();
                break;
        }
    }

    private SafeReportUtil safeReportUtil;

    private void showReportSafeDialog() {
        initReportSafe();
        safeReportUtil.showSafeReportDialog(this);
    }

    private void initReportSafe() {
        if(safeReportUtil != null) {
            safeReportUtil = null;
        }
        safeReportUtil = new SafeReportUtil();
    }

    private void showSendMessageDialog() {
        initReportSafe();
        safeReportUtil.showSendMessageDialog(this);
    }
}
