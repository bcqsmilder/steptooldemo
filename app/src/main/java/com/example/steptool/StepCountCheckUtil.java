package com.example.steptool;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Build;

import static android.content.Context.SENSOR_SERVICE;
import static android.hardware.Sensor.TYPE_STEP_COUNTER;
import static android.hardware.Sensor.TYPE_STEP_DETECTOR;

/**
 * Create by bcq on 2020/7/10
 * Email:352719965@qq.com
 */
public class StepCountCheckUtil {
    private Context mContext;


    public boolean  isSupportStepCountSensor() {
        return mContext.getPackageManager()
                .hasSystemFeature(PackageManager.FEATURE_SENSOR_STEP_COUNTER);
    }



    /**
     * 判断该设备是否支持计歩
     *
     * @param context
     * @return
     */
    public static boolean isSupportStepCountSensor(Context context) {
        // 获取传感器管理器的实例
        SensorManager sensorManager = (SensorManager) context
                .getSystemService(SENSOR_SERVICE);
        Sensor countSensor = sensorManager.getDefaultSensor(TYPE_STEP_COUNTER);
        Sensor detectorSensor = sensorManager.getDefaultSensor(TYPE_STEP_DETECTOR);
        return countSensor != null || detectorSensor != null;
    }





}
