package com.im.openpush;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

public class MainActivity extends AppCompatActivity {

    private Intent service;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        service = new Intent(this,IMPushService.class);
    }

    public void startService(View view) {
        startService(service);
        MyUtils.showToast(this,"开启服务成功");
    }

    public void stopService(View view) {
        stopService(service);
        MyUtils.showToast(this,"关闭服务成功");
    }
}
