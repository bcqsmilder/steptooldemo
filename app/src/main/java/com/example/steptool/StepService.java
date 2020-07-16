package com.example.steptool;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.BitmapFactory;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Create by bcq on 2020/7/9
 * Email:352719965@qq.com
 */
public class StepService extends Service implements SensorEventListener {
    private MessengerHandler mMessengerHandler = new MessengerHandler();
    //发送消息，用来和Service之间传递步数
    private Messenger messenger = new Messenger(mMessengerHandler);
    //广播接收
    private BroadcastReceiver mInfoReceiver;
    //计步传感器类型 0-counter 1-detector
    private int stepSensor = -1;
    //当前步数
    private int currentStep = 0;

    //是否有当天的记录
    private Boolean hasRecord = false;
    //未记录之前的步数
    private int hasStepCount = 0;
    //下次记录之前的步数
    private int previousStepCount = 0;


    //传感器
    private SensorManager sensorManager;


    private Notification.Builder builder = null;

    private NotificationManager notificationManager = null;
    private Intent nfIntent= null;


    @Override
    public void onCreate() {
        super.onCreate();
        initBroadcastReceiver();
        new Thread() {
            @Override
            public void run() {
                super.run();
                Log.i("=============", "开启子线程");
                getStepDetector();
            }
        }.start();
    }


    /**
     * 获取传感器实例
     */
    private void getStepDetector() {
        if (sensorManager != null) {
            sensorManager = null;
        }
        // 获取传感器管理器的实例
        sensorManager = (SensorManager) this.getSystemService(Context.SENSOR_SERVICE);
        Log.i("=============", "判断版本号");
        //android4.4以后可以使用计步传感器
        if (Build.VERSION.SDK_INT >= 19) {
            addCountStepListener();
            Log.i("=============", "版本号大于19");
        }
    }







    /**
     * 添加传感器监听
     */
    private void addCountStepListener() {
        Sensor countSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
        Sensor detectorSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR);
        Log.i("=============", "添加传感器");
        if (countSensor != null) {
            stepSensor = 0;
            Log.i("=============", " 添加传感器stepSensor = 0");
            sensorManager.registerListener(this, countSensor, SensorManager.SENSOR_DELAY_NORMAL);
        } else if (detectorSensor != null) {
            stepSensor = 1;
            Log.i("=============", " 添加传感器stepSensor = 1");
            sensorManager.registerListener(this,detectorSensor, SensorManager.SENSOR_DELAY_NORMAL
            );
        }
    }


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return messenger.getBinder();
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

            /**
             * 此处设将Service为前台，不然当APP结束以后很容易被GC给干掉，
             * 这也就是大多数音乐播放器会在状态栏设置一个原理大都是相通的
             */
            notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);


            //----------------  针对8.0 新增代码 --------------------------------------
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                builder = new Notification.Builder(this.getApplicationContext(), ConstantData.CHANNEL_ID);
                NotificationChannel notificationChannel =new
                        NotificationChannel(
                                ConstantData.CHANNEL_ID,
                                ConstantData.CHANNEL_NAME,
                                NotificationManager.IMPORTANCE_MIN
                        );
                notificationChannel.enableLights(false);//如果使用中的设备支持通知灯，则说明此通知通道是否应显示灯
                notificationChannel.setShowBadge(false);//是否显示角标
                notificationChannel.setLockscreenVisibility(Notification.VISIBILITY_SECRET);
                NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                manager.createNotificationChannel(notificationChannel);
                builder.setChannelId(ConstantData.CHANNEL_ID);
            } else {
                builder = new Notification.Builder(this.getApplicationContext());
            }

            /**
             * 设置点击通知栏打开的界面，此处需要注意了，
             * 如果你的计步界面不在主界面，则需要判断app是否已经启动，
             * 再来确定跳转页面，这里面太多坑
             */
            nfIntent =new  Intent(this, MainActivity.class);
            setStepBuilder();
            // 参数一：唯一的通知标识；参数二：通知消息。
            startForeground(ConstantData.NOTIFY_ID, builder.build());// 开始前台服务
            return START_STICKY;
        }

    /**
     * 由传感器记录当前用户运动步数，注意：该传感器只在4.4及以后才有，并且该传感器记录的数据是从设备开机以后不断累加，
     * 只有当用户关机以后，该数据才会清空，所以需要做数据保护
     */
    @Override
    public void onSensorChanged(SensorEvent event) {
        Log.i("=============", "传感器改变");
        if (stepSensor == 0) {
            int tempStep = (int) event.values[0];
            Log.i("=============", "传感器改变0");
            if (!hasRecord) {
                hasRecord = true;
                hasStepCount = tempStep;
            } else {
                int thisStepCount = tempStep - hasStepCount;
                currentStep += thisStepCount - previousStepCount;
                previousStepCount = thisStepCount;
                Log.i("=============", "传感器改变0currentStep"+currentStep);
            }
            saveStepData();
        } else if (stepSensor == 1) {
            Log.i("=============", "传感器改变1");
            if (event.values[0] == 1.0) {
                currentStep++;
                Log.i("=============", "传感器改变1currentStep"+currentStep);
                saveStepData();
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }


    /**
     * 初始化广播
     */
    private void initBroadcastReceiver() {
        IntentFilter filter = new IntentFilter();
        // 屏幕灭屏广播
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        //关机广播
        filter.addAction(Intent.ACTION_SHUTDOWN);
        // 屏幕解锁广播
        filter.addAction(Intent.ACTION_USER_PRESENT);
        // 当长按电源键弹出“关机”对话或者锁屏时系统会发出这个广播
        // example：有时候会用到系统对话框，权限可能很高，会覆盖在锁屏界面或者“关机”对话框之上，
        // 所以监听这个广播，当收到时就隐藏自己的对话，如点击pad右下角部分弹出的对话框
        filter.addAction(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
        //监听日期变化
        filter.addAction(Intent.ACTION_DATE_CHANGED);
        filter.addAction(Intent.ACTION_TIME_CHANGED);
        filter.addAction(Intent.ACTION_TIME_TICK);

        mInfoReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                switch (intent.getAction()) {
                    // 屏幕灭屏广播
                    case Intent.ACTION_SCREEN_OFF:
                        saveStepData();
                        break;
//关机广播，保存好当前数据
                    case Intent.ACTION_SHUTDOWN:
                        saveStepData();
                        break;
                    // 屏幕解锁广播
                    case Intent.ACTION_USER_PRESENT:
                        saveStepData();
                        break;
// 当长按电源键弹出“关机”对话或者锁屏时系统会发出这个广播
                    // example：有时候会用到系统对话框，权限可能很高，会覆盖在锁屏界面或者“关机”对话框之上，
                    // 所以监听这个广播，当收到时就隐藏自己的对话，如点击pad右下角部分弹出的对话框
                    case Intent.ACTION_CLOSE_SYSTEM_DIALOGS:
                        saveStepData();
                        break;
                    //监听日期变化
                    case Intent.ACTION_DATE_CHANGED:
                    case Intent.ACTION_TIME_CHANGED:
                    case Intent.ACTION_TIME_TICK:
                        saveStepData();
                        break;
                }
            }
        };
        //注册广播
        registerReceiver(mInfoReceiver, filter);
    }


    /**
     * 自定义handler
     */
    class MessengerHandler extends Handler {
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case ConstantData.MSG_FROM_CLIENT:
                    try {
                        Log.i("=============", "收到客户端消息");
                        Log.i("=============", msg.getData().getString("msg"));
                        Messenger messenger = msg.replyTo;
                        Message replyMsg = Message.obtain(null, ConstantData.MSG_FROM_SERVER);
                        Bundle bundle = new Bundle();
                        bundle.putInt("steps", currentStep);
                        bundle.putString("reply", "I am from the server");
                        replyMsg.setData(bundle);
                        messenger.send(replyMsg);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    break;
            }
        }
    }


    /**
     * 保存当天的数据到数据库中，并去刷新通知栏
     */
    private void saveStepData() {

        StepEntity entity = null;
        //为空则说明还没有该天的数据，有则说明已经开始当天的计步了
        if (entity == null) {
            //没有则新建一条数据
            entity = new StepEntity();

            entity.setSteps("" + currentStep);

        } else {
            //有则更新当前的数据
            entity.setSteps("" + currentStep);
        }
        setStepBuilder();

    }
    private void  setStepBuilder() {
        PendingIntent pendingIntent=PendingIntent.getActivity(this,0,nfIntent,0);

        builder.setContentIntent(pendingIntent);// 设置PendingIntent
        builder .setLargeIcon(BitmapFactory.decodeResource(this.getResources(),R.mipmap.ic_launcher));

        builder.setContentTitle("今日步数" + currentStep + "步");
        builder.setSmallIcon(R.mipmap.ic_launcher);
        builder.setContentText("加油，要记得勤加运动哦");
        // 获取构建好的Notification
        Notification stepNotification = builder.build();
        //调用更新
        notificationManager.notify(ConstantData.NOTIFY_ID, stepNotification);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        //主界面中需要手动调用stop方法service才会结束
        stopForeground(true);
        unregisterReceiver(mInfoReceiver);
    }

}
