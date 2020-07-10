package com.example.steptool;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;
import android.view.View;
import android.widget.TextView;



import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {

    TextView is_support_tv,setp;
    private TimerTask mTimerTask;
    private Timer timer;
    private Messenger mMessager;
    Handler mHandler=new Handler(){
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                //这里用来获取到Service发来的数据
                case ConstantData.MSG_FROM_SERVER :
//                如果是今天则更新数据
//                     记录运动步数
                     Bundle bundle = (Bundle) msg.getData();
                     int steps=bundle.getInt("steps");
                     String reply=bundle.getString("reply");
                    Log.i("============",reply);
                    //设置的步数
                    setp.setText(steps+"步数");
                    //计算总公里数

                break;
            }
        }
    };
    private Messenger mGetReplyMessenger =new  Messenger(mHandler);
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        is_support_tv=findViewById(R.id.is_support_tv);
        setp=findViewById(R.id.setp);
        initData();
    }

    private void initData() {

        /**
         * 这里判断当前设备是否支持计步
         */

        if (StepCountCheckUtil.isSupportStepCountSensor(this)) {

            is_support_tv.setVisibility( View.GONE);
            setupService();
        }else{
            setp.setText("0 步");
        }
    }

ServiceConnection mServiceConnection=new ServiceConnection() {
    @Override
    public void onServiceConnected(ComponentName componentName, final IBinder iBinder) {
        mTimerTask=new TimerTask() {
            @Override
            public void run() {
                try {
                mMessager= new Messenger(iBinder);
                Message msg=Message.obtain(null,ConstantData.MSG_FROM_CLIENT);
                    Bundle data = new Bundle();
                    data.putString("msg", "I am from the client.");
                    msg.setData(data);
                msg.replyTo = mGetReplyMessenger;
                    mMessager.send(msg);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        };

        timer = new Timer();
        timer.schedule(mTimerTask,0,500);
    }

    @Override
    public void onServiceDisconnected(ComponentName componentName) {

    }
};


    /**
     * 开启计步服务
     */
    private void setupService() {
        Intent intent =new  Intent(this, StepService.class);
      bindService(intent,mServiceConnection, Context.BIND_AUTO_CREATE);
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.O)
            startForegroundService(intent);
        else
            startService(intent);

        Log.i("=============", "开启服务");
    }
}