package com.im.rabbitmqpush.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.im.rabbitmqpush.R;
import com.im.rabbitmqpush.fork.NativeRuntime;
import com.im.rabbitmqpush.service.IMPushService;
import com.im.rabbitmqpush.utils.MyFileUtils;
import com.im.rabbitmqpush.utils.MyLog;
import com.im.rabbitmqpush.utils.MyToast;
import com.im.rabbitmqpush.utils.MyUtils;
import com.im.rabbitmqpush.utils.ThreadUtil;

public class MainActivity extends AppCompatActivity {

    private Intent service;
    private MainActivity act;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        act = this;
        setContentView(R.layout.activity_main);
        service = new Intent(this, IMPushService.class);
        initDaemonService();
    }

    public void startService(View view) {
        if (MyUtils.isServiceRunning(act, "com.im.openpush.service.IMPushService")) {
            MyToast.showToast(this, "服务已经在运行了");
            return;
        }
        ThreadUtil.runOnBackThread(new Runnable() {
            @Override
            public void run() {
                try {
                    startService(service);
                    MyToast.showToast(act, "开启服务成功");
                    MyLog.showLog("路径:" + MyFileUtils.createRootPath());
                    NativeRuntime.getInstance().stringFromJNI();
                    NativeRuntime.getInstance().startService(getPackageName() + "/com.im.openpush.service.IMPushService", MyFileUtils.createRootPath());
                    MyToast.showToast(act, "开启守护进程成功");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void initDaemonService() {
        String executable = "libhelper.so";
        String aliasfile = "helper";
        String parafind = "/data/data/" + getPackageName() + "/" + aliasfile;
        String retx = "false";
        NativeRuntime.getInstance().RunExecutable(getPackageName(), executable, aliasfile, getPackageName() + "/com.im.openpush.service.IMPushService");
    }

    public void stopService(View view) {
//        finish();
        ThreadUtil.runOnBackThread(new Runnable() {
            @Override
            public void run() {
                try {
                    NativeRuntime.getInstance().stopService();
                    stopService(service);
                    android.os.Process.killProcess(android.os.Process.myPid());
                    System.exit(0);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }
}
