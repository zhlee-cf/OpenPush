package com.im.rabbitmqpush.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.im.rabbitmqpush.utils.MyLog;

/**
 *
 * Created by lzh12 on 2016/6/21.
 */
public class BootUpReceiver extends BroadcastReceiver{
    @Override
    public void onReceive(Context context, Intent intent) {
//        Toast.makeText(context,"监听到系统广播" + intent.getAction(),Toast.LENGTH_SHORT).show();
        MyLog.showLog("监听到系统广播" + intent.getAction());
    }
}
