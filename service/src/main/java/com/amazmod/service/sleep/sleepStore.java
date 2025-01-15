package com.amazmod.service.sleep;

import android.content.Context;
import android.os.Handler;

import com.amazmod.service.MainService;
import com.amazmod.service.sleep.sensor.sensorsStore;

import org.tinylog.Logger;

import java.time.Duration;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.TimerTask;

import amazmod.com.transport.Transport;
import amazmod.com.transport.data.SleepData;

public class sleepStore {
    private static long batchSize = 1;
    private static long timestamp;
    private static boolean isSuspended;
    private static boolean isTracking;
    private static boolean isDoHrMonitoring;
    private static LinkedList<Float> acc_max_data = new LinkedList<>();
    private static LinkedList<Float> acc_max_raw_data = new LinkedList<>();

    static Handler handler = new Handler();
    static Runnable runnable;
    static int delay = sleepConstants.SECS_PER_MAX_VALUE * 1000;

    public static boolean isTracking() {
        return isTracking;
    }

    public static void setTracking(boolean IsTracking, Context context, boolean doHrMonitoring) {
        if (IsTracking) {
            if (!isTracking) //Don't start sensors again if it was listening
                sleepUtils.setSensorsState(true, context, doHrMonitoring);
            isDoHrMonitoring = doHrMonitoring;
            isSuspended = false;
            timestamp = 0L;
        } else {
            sleepUtils.setSensorsState(false, context, false);
            handler.removeCallbacks(runnable);
            batchSize = 1;
        }
        isTracking = IsTracking;
    }

    public static void addMaxData(float max_data, float max_raw_data) {
        acc_max_data.add(max_data);
        acc_max_raw_data.add(max_raw_data);
    }

    public static LinkedList<Float> getMaxData() {
        return acc_max_data;
    }

    public static LinkedList<Float> getMaxRawData() {
        return acc_max_raw_data;
    }

    public static void resetMaxData() {
        acc_max_data = new LinkedList<>();
        acc_max_raw_data = new LinkedList<>();
    }

    public static void setBatchSize(long BatchSize) {
        long oldBatchSize = batchSize;
        batchSize = BatchSize;
        if (oldBatchSize != batchSize)
            sensorsStore.getAccelerometer().setBatchSize(BatchSize);
    }

    public static long getBatchSize() {
        return batchSize;
    }

    public static void setSuspended(boolean IsSuspended, Context context) {
        isSuspended = IsSuspended;
        if (IsSuspended) {
            sleepUtils.setSensorsState(false, context, false);
            startTimerEmptyData(context);
        } else {
            timestamp = 0L;
            handler.removeCallbacks(runnable);
            sleepUtils.setSensorsState(true, context, isDoHrMonitoring);
        }
    }

    private static void startTimerEmptyData(Context context) {
        handler.post(runnable = new Runnable() {
            public void run() {
                handler.postDelayed(this, delay);
                sendEmptyData();
            }
        });
    }

    private static void sendEmptyData() {
        SleepData sleepData = new SleepData();
        sleepData.setAction(SleepData.actions.ACTION_DATA_UPDATE);
        sleepData.setMax_data(new float[]{0f, 0f, 0f, 0f});
        sleepData.setMax_raw_data(new float[]{0f, 0f, 0f, 0f});
        Logger.debug("Sleep Empty Data (accelerometer): " + sleepData);
        Logger.debug("Sending sleep Empty batch to phone...");
        MainService.sendSleep(Transport.SLEEP_DATA, sleepData);
    }

    public static void setTimestamp(long timestamp, Context context) {
        sleepStore.timestamp = timestamp;

        if (timestamp != 0) {
            long deltaTime = timestamp - System.currentTimeMillis();
            long millis = deltaTime;
            long minutes = (millis / 1000) / 60;
            int seconds = (int) ((millis / 1000) % 60);
            sleepUtils.postNotification("Sleep Pause", minutes + "m " + seconds + "s", context);
        }
    }

    public static boolean isSuspended() {
        return isSuspended;
    }

    public static long getTimestamp() {
        return timestamp;
    }
}
