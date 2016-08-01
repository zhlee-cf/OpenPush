package com.im.rabbitmqpush.utils;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.content.Context;

import java.util.List;

public class MyUtils {
    /**
     * 判断指定名称的服务是否运行
     *
     * @param act
     * @param serviceName
     * @return
     */
    public static boolean isServiceRunning(Context act, String serviceName) {
        ActivityManager am = (ActivityManager) act.getSystemService(Context.ACTIVITY_SERVICE);
        List<RunningServiceInfo> runningServices = am.getRunningServices(200); // 参数是服务数量的最大值，一般手机中，运行，20
        for (RunningServiceInfo runningServiceInfo : runningServices) {
            String runningServiceName = runningServiceInfo.service.getClassName();
            if (runningServiceName.equals(serviceName)) {
                return true;
            }
        }
        return false;
    }
}