package com.amazmod.service.sleep;

import android.content.Context;
import android.os.Handler;

import com.amazmod.service.MainService;
import com.amazmod.service.sleep.sensor.sensorsStore;

import org.tinylog.Logger;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.Locale;

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

    static Handler handlerEmptyData = new Handler();
    static Runnable runnableEmptyData;
    static int delayEmptyData = (int) (sleepConstants.SECS_PER_MAX_VALUE * 1000);

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
            handlerEmptyData.removeCallbacks(runnableEmptyData);
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
            handlerEmptyData.removeCallbacks(runnableEmptyData);
            sleepUtils.setSensorsState(true, context, isDoHrMonitoring);
        }
    }

    private static int latestSaveBatch = 0;
    private static void startTimerEmptyData(Context context) {
        handlerEmptyData.post(runnableEmptyData = new Runnable() {
            public void run() {
                handlerEmptyData.postDelayed(this, delayEmptyData);

                int tsMillis = (int) System.currentTimeMillis();
                if (latestSaveBatch == 0) latestSaveBatch = tsMillis; //First value
                //If latest time saving batch was >= 10s ago
                if (tsMillis - latestSaveBatch >= sleepConstants.SECS_PER_MAX_VALUE * 1000 /*To millis*/) {
                    Logger.debug(new SimpleDateFormat("hh:mm:ss", Locale.US).format(new Date()) + "- Added accelerometer empty values to batch");
                    sleepStore.addMaxData(0, 0);
                    latestSaveBatch = tsMillis;
                    sendEmptyData();
                }
            }
        });
    }

    private static void sendEmptyData() {
        //Send data if batch reached batch size
        if (sleepStore.getMaxData().size() >= sleepStore.getBatchSize()) {
            SleepData sleepData = new SleepData();
            sleepData.setAction(SleepData.actions.ACTION_DATA_UPDATE);
            sleepData.setMax_data(sleepUtils.linkedToArray(sleepStore.getMaxData()));
            sleepData.setMax_raw_data(sleepUtils.linkedToArray(sleepStore.getMaxRawData()));
            sleepStore.resetMaxData();
            Logger.debug("Sleep empty Data (accelerometer): " + sleepData);
            Logger.debug("Sending sleep empty batch to phone...");
            MainService.sendSleep(Transport.SLEEP_DATA, sleepData);
        }
    }

    public static void setTimestamp(long timestamp, Context context) {
        sleepStore.timestamp = timestamp;

        if (timestamp != 0) {
            long deltaTime = timestamp - System.currentTimeMillis();
            long millis = deltaTime;
            long minutes = (millis / 1000) / 60;
            int seconds = (int) ((millis / 1000) % 60);
            sleepUtils.postNotification("Sleep", "Pause: " + minutes + "m " + seconds + "s", context);
        } else {
            sleepUtils.postNotification("Sleep", "Resume", context);
        }
    }

    public static boolean isSuspended() {
        return isSuspended;
    }

    public static long getTimestamp() {
        return timestamp;
    }
}
