package com.amazmod.service.sleep.sensor;

import static java.lang.Math.sqrt;
import static java.lang.StrictMath.abs;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;

import com.amazmod.service.MainService;
import com.amazmod.service.sleep.sleepConstants;
import com.amazmod.service.sleep.sleepStore;
import com.amazmod.service.sleep.sleepUtils;
import com.huami.watch.transport.DataBundle;

import org.tinylog.Logger;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import amazmod.com.transport.Transport;
import amazmod.com.transport.data.SleepData;

public class accelerometer implements SensorEventListener {
    private static final long maxReportLatencyUs = 195_000_000;

    private static float current_max_data;
    private static float current_max_raw_data;
    private static float lastX;
    private static float lastY;
    private static float lastZ;
    private static int latestSaveBatch = 0;

    private SensorManager sm;
    private Handler flushHandler;
    private int flushInterval;

    public void registerListener(Context context) {
        Logger.debug("Registering accelerometer listener...");
        sm = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        //Batching disabled because it doesn't work on any amazfit
        sm.registerListener(this, sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                sleepConstants.SAMPLING_PERIOD_US);
                //sleepConstants.SAMPLING_PERIOD_US, (int) maxReportLatencyUs);
        setupHandler();
    }

    public void setupHandler() {
        /*if (Looper.myLooper() == null) Looper.prepare();
        flushHandler = new Handler(Looper.getMainLooper());
        flushHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if(!sleepStore.isTracking()) return;
                if(!sleepStore.isSuspended()) flush();
                flushHandler.postDelayed(this, flushInterval);
            }
        }, 10);*/
    }

    public void unregisterListener(Context context) {
        sm.unregisterListener(this);
        //flushHandler.removeCallbacksAndMessages(null);
    }

    public void setBatchSize(long size) {
        /*if (size > sleepConstants.MAX_BATCH_SIZE) size = sleepConstants.MAX_BATCH_SIZE;
        flushInterval = (int) size * sleepConstants.SECS_PER_MAX_VALUE * 1000;
        flush();*/
    }

    public void flush() {
        if (sm != null) sm.flush(this);
        Logger.debug("Flushing accelerometer sensor...");
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        if (sleepStore.isSuspended()) return;
        float x = sensorEvent.values[0];
        float y = sensorEvent.values[1];
        float z = sensorEvent.values[2];

        //SAA processing
        float max = abs(x - lastX) + abs(y - lastY) + abs(z - lastZ);
        if (max > current_max_data) current_max_data = max;
        float max_raw = (float) sqrt((x * x) + (y * y) + (z * z));
        if (max_raw > current_max_raw_data) current_max_raw_data = max_raw;
        lastX = x;
        lastY = y;
        lastZ = z;

        int tsMillis = (int) (sensorEvent.timestamp / 1_000_000L);

        if (latestSaveBatch == 0) latestSaveBatch = tsMillis; //First value
        //If latest time saving batch was >= 10s ago
        if (tsMillis - latestSaveBatch >= sleepConstants.SECS_PER_MAX_VALUE * 1000 /*To millis*/) {
            Logger.debug(new SimpleDateFormat("hh:mm:ss", Locale.US).format(new Date()) + "- Added accelerometer values to batch");
            sleepStore.addMaxData(current_max_data, current_max_raw_data);
            current_max_data = 0;
            current_max_raw_data = 0;
            latestSaveBatch = tsMillis;
            checkAndSendBatch();
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {
    }

    private void checkAndSendBatch() {
        //Send data if batch reached batch size
        if (sleepStore.getMaxData().size() >= sleepStore.getBatchSize()) {
            SleepData sleepData = new SleepData();
            sleepData.setAction(SleepData.actions.ACTION_DATA_UPDATE);
            sleepData.setMax_data(sleepUtils.linkedToArray(sleepStore.getMaxData()));
            sleepData.setMax_raw_data(sleepUtils.linkedToArray(sleepStore.getMaxRawData()));
            sleepStore.resetMaxData();
            Logger.debug("Sleep Data (accelerometer): " + sleepData);
            Logger.debug("Sending sleep batch to phone...");
            MainService.sendSleep(Transport.SLEEP_DATA, sleepData);
        }
    }
}
