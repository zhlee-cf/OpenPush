package com.im.openpush.utils;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.content.ComponentName;
import android.content.Context;
import android.widget.Toast;

import java.util.List;

public class MyToast {

    /**
     * 方法：主线程和子线程弹吐司
     *
     * @param act
     * @param msg
     */
    public static void showToast(final Activity act, final String msg) {

        if ("main".equals(Thread.currentThread().getName())) {
            // 主线程弹吐司
            Toast.makeText(act, msg, Toast.LENGTH_SHORT).show();
        } else {
            // 子线程弹吐司
            act.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(act, msg, Toast.LENGTH_SHORT).show();
                }
            });
        }
    }
}