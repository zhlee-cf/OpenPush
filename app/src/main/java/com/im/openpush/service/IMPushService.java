package com.im.openpush.service;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Environment;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.annotation.Nullable;
import android.telephony.TelephonyManager;

import com.im.openpush.R;
import com.im.openpush.activity.MainActivity;
import com.im.openpush.receiver.ScreenListener;
import com.im.openpush.receiver.TickAlarmReceiver;
import com.im.openpush.receiver.TimeTickReceiver;
import com.im.openpush.utils.MyBase64Utils;
import com.im.openpush.utils.MyLog;
import com.im.openpush.utils.ThreadUtil;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Consumer;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import com.rabbitmq.client.impl.DefaultExceptionHandler;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeoutException;

/**
 * 接收来自RabbitMQ的推送
 * 目前是持久化模式 不在线的话 推送会在再次上线时推送过去
 * <p/>
 * 交换机持久化 队列持久化 消息持久化
 * Created by lzh12 on 2016/6/7.
 */
public class IMPushService extends Service {

    private ConnectionFactory factory;
    private static final String DURABLE_EXCHANGE_NAME = "durable_3";
    // TODO 这个参数应该是唯一的 跟 username有关
    private String DURABLE_QUEUE_NAME;
    private static final boolean durable = true; //消息队列持久化
    private PowerManager.WakeLock wakeLock;
    private NotificationManager notificationManager;
    private int locked;
    private Channel channel;
    private Connection connection;
    private PendingIntent tickPendIntent;
    private TimeTickReceiver mTimeTickReceiver;

    private static String logPath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/exiu/push_log.txt";
    private static File logFile = new File(logPath);
    private SimpleDateFormat ft;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        MyLog.showLog("IMPushService---onCreate");
        initData();
        // 开启前台进程  不在状态栏添加图标
        startForeground(0, null);
        // 初始化连接工厂
        setupConnectionFactory();
        // 订阅
        subscribePush();
        // 开启计时器
        setTickAlarm();
        // 监听系统时间改变广播
//        setTimeTickReceiver();
        // 锁屏保持CPU运行
//        keepCPUAlive();
    }

    /**
     * 监听系统时间改变广播
     * 此广播每分钟发一次
     */
    private void setTimeTickReceiver() {
        mTimeTickReceiver = new TimeTickReceiver();
        IntentFilter filter = new IntentFilter(Intent.ACTION_TIME_TICK);
        registerReceiver(mTimeTickReceiver, filter);
    }

    /**
     * 初始化数据
     */
    private void initData() {
        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        TelephonyManager telephonyManager = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
        String deviceId = telephonyManager.getDeviceId();
        MyLog.showLog("deviceId::" + deviceId);
        DURABLE_QUEUE_NAME = MyBase64Utils.encodeToString(deviceId) + "#OpenIM";
        ft = new SimpleDateFormat("HH:mm:ss");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    /**
     * 开个计时器类似的  唤醒服务
     */
    protected void setTickAlarm() {
        AlarmManager alarmMgr = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(this, TickAlarmReceiver.class);
        int requestCode = 0;
        tickPendIntent = PendingIntent.getBroadcast(this,
                requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        //小米2s的MIUI操作系统，目前最短广播间隔为5分钟，少于5分钟的alarm会等到5分钟再触发
        long triggerAtTime = System.currentTimeMillis();
        MyLog.showLog("triggerAtTime::" + triggerAtTime);
        int interval = 60 * 1000;
        alarmMgr.setRepeating(AlarmManager.RTC_WAKEUP, triggerAtTime, interval, tickPendIntent);
    }

    /**
     * 取消计时器
     */
    protected void cancelTickAlarm() {
        AlarmManager alarmMgr = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        alarmMgr.cancel(tickPendIntent);
    }


    /**
     * 初始化连接工厂类
     */
    private void setupConnectionFactory() {
        factory = new ConnectionFactory();
//        String uri = "amqp://push.openim.top";
//        try {
        // 设置允许自动重连
        factory.setAutomaticRecoveryEnabled(true);

        // 设置连接超时时间
        factory.setConnectionTimeout(60 * 1000);
        // 设置重连时间间隔 默认就是5秒
        factory.setNetworkRecoveryInterval(5 * 1000);
        // 默认10000
        factory.setHandshakeTimeout(5000);
        // 默认60
        factory.setRequestedHeartbeat(30);

        factory.setExceptionHandler(new DefaultExceptionHandler() {
            @Override
            public void handleConnectionRecoveryException(Connection conn, Throwable exception) {
                MyLog.showLog("重新连接异常::" + exception.getMessage());
                super.handleConnectionRecoveryException(conn, exception);
            }
        });

        // 设置服务器Uri
        factory.setHost("push.openim.top");
//            factory.setUri(uri);
//        } catch (KeyManagementException | NoSuchAlgorithmException | URISyntaxException e1) {
//            e1.printStackTrace();
//        }
    }

    /**
     * 订阅
     */
    private void subscribePush() {
        ThreadUtil.runOnBackThread(new Runnable() {
            @Override
            public void run() {
                try {
                    connection = factory.newConnection();

                    channel = connection.createChannel();

                    channel.exchangeDeclare(DURABLE_EXCHANGE_NAME, "fanout", durable);

                    MyLog.showLog(DURABLE_QUEUE_NAME + "============");
                    channel.queueDeclare(DURABLE_QUEUE_NAME, durable, false, false, null);
                    channel.queueBind(DURABLE_QUEUE_NAME, DURABLE_EXCHANGE_NAME, "");
                    Consumer consumer = new DefaultConsumer(channel) {
                        @Override
                        public void handleDelivery(String consumerTag, Envelope envelope,
                                                   AMQP.BasicProperties properties, byte[] body) throws IOException {
                            String message = new String(body, "UTF-8");
                            newMsgNotify(message);

                            writeToLog(message);

                            MyLog.showLog("收到RabbitMQ推送::" + message);
                            // 手动回执
                            channel.basicAck(envelope.getDeliveryTag(), false);
                        }
                    };
                    channel.basicConsume(DURABLE_QUEUE_NAME, false, consumer);
                } catch (IOException | TimeoutException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    /**
     * 把收到的推送写到文件中
     *
     * @param message
     */
    private void writeToLog(String message) {
        if (!logFile.getParentFile().exists()) {
            logFile.getParentFile().mkdirs();
        }
        try {
            FileOutputStream fos = new FileOutputStream(logFile, true);
            fos.write((message + "**收到时间==" + ft.format(new Date())).getBytes());
            fos.write("\n".getBytes());
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 新消息通知
     */
    private void newMsgNotify(String messageBody) {
        CharSequence tickerText = "RabbitMQ新通知！";
        // 收到单人消息时，亮屏
        acquireWakeLock();
        Intent intent = new Intent(this, MainActivity.class);
        // 必须添加
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent contentIntent = PendingIntent.getActivity(this, 77, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        Notification notification = new Notification.Builder(this)
                .setContentTitle("RabbitMQ")
                .setContentText(messageBody)
                .setContentIntent(contentIntent)
                .setTicker(tickerText)
                .setSmallIcon(R.mipmap.ic_launcher)
                .build();
        // 设置默认声音
        notification.defaults |= Notification.DEFAULT_SOUND;
        // 设定震动(需加VIBRATE权限)
        notification.defaults |= Notification.DEFAULT_VIBRATE;
        notification.vibrate = new long[]{0, 100, 200, 300};
        // 设置LED闪烁
        notification.defaults |= Notification.DEFAULT_LIGHTS;
        notification.ledARGB = 0xff00ff00;
        notification.ledOnMS = 300;
        notification.ledOffMS = 1000;
        notification.flags |= Notification.FLAG_SHOW_LIGHTS;

        // 点击通知后 通知栏消失
        notification.flags = Notification.FLAG_AUTO_CANCEL;
        notificationManager.notify(5555, notification);
    }

    /**
     * 方法 点亮屏幕 要加权限 <uses-permission
     * android:name="android.permission.WAKE_LOCK"></uses-permission>
     */
    private void acquireWakeLock() {
        if (wakeLock == null) {
            PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
            wakeLock = powerManager.newWakeLock(PowerManager.ACQUIRE_CAUSES_WAKEUP | PowerManager.SCREEN_DIM_WAKE_LOCK, "lzh");
        }
        wakeLock.acquire();
        wakeLock.release();
    }

    /**
     * 方法 在锁屏时 保持CPU运行
     */
    private void keepCPUAlive() {
        //获取电源锁，保证cpu运行
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        final PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "pw_tag");
        ScreenListener l = new ScreenListener(this);
        l.begin(new ScreenListener.ScreenStateListener() {
            @Override
            public void onUserPresent() {
            }

            @Override
            public void onScreenOn() {
                if (wl != null && locked == 1) {
                    wl.release();
                    locked = 0;
                    MyLog.showLog("亮屏");
                }
            }

            @Override
            public void onScreenOff() {
                if (wl != null && locked == 0) {
                    wl.acquire();
                    locked = 1;
                    MyLog.showLog("锁屏");
                }
            }
        });
    }

    @Override
    public void onDestroy() {
        // 退出服务时 关闭Channel，断开链接(不然还会收到消息)
        if (channel != null) {
            try {
                channel.close();
            } catch (IOException | TimeoutException e) {
                e.printStackTrace();
            }
        }
        if (connection != null) {
            try {
                connection.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (mTimeTickReceiver != null) {
            unregisterReceiver(mTimeTickReceiver);
        }
        cancelTickAlarm();
        stopForeground(true);
        MyLog.showLog("服务被杀死---onDestroy");
        super.onDestroy();
    }
}
