package org.enes.lanvideocall.activities;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.widget.Toolbar;

import org.enes.lanvideocall.R;

public class MainActivity extends MyActivity implements View.OnClickListener {

    public static final String IS_NEW_INSTALL_FLAG = "isnewinstallflag";

    public static void gotoMainActivity(Context ctx, boolean isNewInstall) {
        Intent intent = new Intent(ctx,MainActivity.class);
        intent.putExtra(IS_NEW_INSTALL_FLAG,isNewInstall);
        ctx.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initView();
    }

    private void initView() {
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle(R.string.app_name);
        setSupportActionBar(toolbar);


        Button btn_video_test = findViewById(R.id.btn_video_test);
        btn_video_test.setOnClickListener(this);
        Button btn_audio_test = findViewById(R.id.btn_audio_test);
        btn_audio_test.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_video_test:
                Intent intent = new Intent(MainActivity.this,VideoCallTestActivity.class);
                startActivity(intent);
                break;
            case R.id.btn_audio_test:
                Intent intent1 = new Intent(MainActivity.this,AudioTestActivity.class);
                startActivity(intent1);
                break;
        }
    }
}
