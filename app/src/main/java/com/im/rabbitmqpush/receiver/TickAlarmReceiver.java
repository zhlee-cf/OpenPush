package com.im.rabbitmqpush.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Environment;
import android.os.PowerManager;

import com.im.rabbitmqpush.service.IMPushService;
import com.im.rabbitmqpush.utils.MyLog;
import com.im.rabbitmqpush.utils.MyUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;

/**
 * 收到广播时，判断MPushService是否在运行中，若不在则启动服务
 * 若IMPushService在运行 并且 屏幕是锁屏状态 则唤醒CPU30秒后释放
 */
public class TickAlarmReceiver extends BroadcastReceiver {
    private static int i;
    private static String logPath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/exiu/alarm_log.txt";
    private static File logFile = new File(logPath);

    static {
        MyLog.showLog("静态代码块儿执行");
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        MyLog.showLog("OpenIMPush收到Alarm广播");
        MyLog.showLog("第几次执行::" + i);
        i++;
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
        MyLog.showLog("到这儿没");
        boolean isIMPushServiceRunning = MyUtils.isServiceRunning(context, "com.im.openpush.service.IMPushService");
        if (!isIMPushServiceRunning) {
            context.startService(new Intent(context, IMPushService.class));
        } else {
            PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            if (!pm.isScreenOn()) {
                PowerManager.WakeLock wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "cpu_tag");
                if (!wakeLock.isHeld()) {
                    wakeLock.acquire(30);
                }
//                wakeLock.release();
            }
        }
    }
}
