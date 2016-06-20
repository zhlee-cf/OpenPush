package com.im.openpush.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Environment;
import android.os.PowerManager;

import com.im.openpush.service.IMPushService;
import com.im.openpush.utils.MyLog;
import com.im.openpush.utils.MyUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;

/**
 * 系统时间改变广播 每分钟发一次
 * Created by lzh12 on 2016/6/17.
 */
public class TimeTickReceiver extends BroadcastReceiver {

    private int i;
    private static String logPath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/exiu/tick_log.txt";
    private static File logFile = new File(logPath);

    @Override
    public void onReceive(Context context, Intent intent) {
        MyLog.showLog("第几次执行::" + i);
        if (!logFile.getParentFile().exists()) {
            logFile.getParentFile().mkdirs();
        }
        try {
            FileOutputStream fos = new FileOutputStream(logFile, true);
            fos.write((new Date() + "==============第几次唤醒::" + i).getBytes());
            fos.write("\n".getBytes());
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        i++;
        boolean isIMPushServiceRunning = MyUtils.isServiceRunning(context, "com.im.openpush.service.IMPushService");
        if (!isIMPushServiceRunning) {
            context.startService(new Intent(context, IMPushService.class));
        } else {
            PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            if (!pm.isScreenOn()) {
                PowerManager.WakeLock wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "cpu_tag");
                if (!wakeLock.isHeld()) {
                    wakeLock.acquire();
                    wakeLock.release();
                }
            }
        }
    }
}
