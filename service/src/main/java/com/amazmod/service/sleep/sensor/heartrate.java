package com.amazmod.service.sleep.sensor;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;

import com.amazmod.service.sleep.sleepConstants;
import com.amazmod.service.sleep.sleepService;
import com.amazmod.service.sleep.sleepStore;
import com.huami.watch.transport.DataBundle;

import org.tinylog.Logger;

import amazmod.com.transport.data.SleepData;

public class heartrate implements SensorEventListener {

    private int currentValue;
    private float[] currentArray = new float[sleepConstants.HR_VALUES];
    private SensorManager sm;
    private Thread waitThread = new waitThread();

    public void registerListener(Context context){
        Logger.debug("Registering hr sensor...");
        sm = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        register();
    }

    public void register(){
        sm.registerListener(this, sm.getDefaultSensor(Sensor.TYPE_HEART_RATE)
                , SensorManager.SENSOR_DELAY_FASTEST, 3 * 1000 * 1000);
    }

    public void unregisterListener(Context context){
        sm = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        sm.unregisterListener(this);
        waitThread.interrupt();
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        if(sleepStore.isSuspended())
            return;

        currentArray[currentValue++] = sensorEvent.values[0];

        if(currentValue == sleepConstants.HR_VALUES){
            SleepData sleepData = new SleepData();
            sleepData.setAction(SleepData.actions.ACTION_HRDATA_UPDATE);
            sleepData.setHrdata(currentArray);
            currentArray = new float[sleepConstants.HR_VALUES];
            currentValue = 0;
            sleepService.send(sleepData.toDataBundle(new DataBundle()));
            sm.unregisterListener(this);
            waitThread.start();
            Logger.debug("Sending sleep hr data to phone...");
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {}

    private static class waitThread extends Thread{
        public void run(){
            try {
                Thread.sleep(sleepConstants.HR_INTERVAL);
            } catch (InterruptedException ignored) {
                return;
            }
            sensorsStore.getHrSensor().register(); //Register sensor after sleep
        }
    }
}