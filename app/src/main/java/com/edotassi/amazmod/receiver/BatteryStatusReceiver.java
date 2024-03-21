package com.edotassi.amazmod.receiver;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;

import androidx.annotation.NonNull;

import com.edotassi.amazmod.AmazModApplication;
import com.edotassi.amazmod.event.BatteryStatus;
import com.edotassi.amazmod.event.OtherData;
import com.edotassi.amazmod.event.SyncBattery;
import com.edotassi.amazmod.helpers.BatteryHelper;
import com.edotassi.amazmod.transport.TransportService;
import com.edotassi.amazmod.watch.Watch;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.Task;
import com.huami.watch.transport.DataBundle;
import com.pixplicity.easyprefs.library.Prefs;

import org.tinylog.Logger;

import amazmod.com.transport.Constants;
import amazmod.com.transport.data.BatteryData;
import amazmod.com.transport.data.SyncBatteryData;

public class BatteryStatusReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(final Context context, Intent intent) {

        final String action = intent.getAction();
        Logger.trace("BatteryStatusReceiver onReceive action: {}", action);

        if (action == null) {
            if (!Watch.isInitialized())
                Watch.init(context);

            // Send battery request and wait for answer (request to Amazmod service)

            Watch.get().getBatteryStatus().continueWith(new Continuation<BatteryStatus, Object>() {
                @Override
                public Object then(@NonNull Task<BatteryStatus> task) {
                    Logger.trace("BatteryStatusReceiver onReceive getBatteryStatus");
                    if (task.isSuccessful()) {
                        BatteryStatus batteryStatus = task.getResult();
                        if (batteryStatus != null) {
                            BatteryHelper.updateBattery(batteryStatus);
                            BatteryHelper.batteryAlert(batteryStatus, context);
                        } else
                            Logger.error("null batteryStatus!");
                    } else {
                        Logger.error(task.getException(), "failed reading battery status");
                    }
                    return null;
                }
            });

            // Send battery request and wait for answer (official API)
//            Watch.get().sendSimpleData(SYNC_BATTERY, SYNC_BATTERY, TransportService.TRANSPORT_COMPANION).continueWith(new Continuation<OtherData, Object>() {
//                @Override
//                public Object then(@NonNull Task<OtherData> task) {
//                    if (task.isSuccessful()) {
//                        // Successful reply from official API
//                        Logger.debug("Successful get official battery info");
//                        OtherData returnedData = task.getResult();
//                        if (returnedData == null)
//                            throw new NullPointerException("Returned data are null");
//
//                        // Convert official data to Amazmod data
//                        DataBundle otherData = returnedData.getOtherData();
//                        BatteryData batteryData = new BatteryData();
//                        batteryData.setLevel( otherData.getInt("BatteryLevel")/100f );
//                        batteryData.setCharging( otherData.getBoolean("BatteryIsCharging") );
//                        batteryData.setUsbCharge(false);
//                        batteryData.setAcCharge(false);
//                        if (otherData.getInt("BatteryLevel") > 98)
//                            batteryData.setDateLastCharge(otherData.getLong("ChargingTime", Long.MIN_VALUE));
//                        else
//                            batteryData.setDateLastCharge(0);
//
//                        //otherData.getInt("ChargingIntervalDays", -1)
//
//                        BatteryStatus batteryStatus = new BatteryStatus( batteryData.toDataBundle() );
//
//                        BatteryHelper.updateBattery( batteryStatus );
//                        BatteryHelper.batteryAlert(batteryStatus, context);
//                    }else{
//                        Logger.debug("Could not get official battery info");
//                    }
//                    return null;
//                }
//            });
        } else {
            startBatteryReceiver(context);
        }


    }

    public static void startBatteryReceiver(Context context) {

        final boolean isEnabled = Prefs.getBoolean(Constants.PREF_BATTERY_CHART, Constants.PREF_DEFAULT_BATTERY_CHART);

        Logger.trace("BatteryStatusReceiver startBatteryReceiver isEnabled: {}", isEnabled);

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent alarmBatteryIntent = new Intent(context, BatteryStatusReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, alarmBatteryIntent, 0);

        if (isEnabled) {
            Logger.trace("BatteryStatusReceiver enabling receiver");

            int syncInterval = Integer.parseInt(Prefs.getString(Constants.PREF_BATTERY_BACKGROUND_SYNC_INTERVAL, "60"));
            AmazModApplication.timeLastSync = Prefs.getLong(Constants.PREF_TIME_LAST_SYNC, 0L);

            long delay = ((long) syncInterval * 60000L) - SystemClock.elapsedRealtime() - AmazModApplication.timeLastSync;

            Logger.info("BatteryStatusReceiver times: " + SystemClock.elapsedRealtime() + " / " + AmazModApplication.timeLastSync);

            if (delay < 60000)
                delay = 60000; // 1 min delay because it may be triggered when you open the app

            try {
                if (alarmManager != null)
                    alarmManager.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + delay,
                            (long) syncInterval * 60000L, pendingIntent);
            } catch (NullPointerException e) {
                Logger.error(e, "BatteryStatusReceiver setRepeating exception: " + e.toString());
            }

        } else {
            Logger.trace("BatteryStatusReceiver disabling receiver");
            if (alarmManager != null)
                alarmManager.cancel(pendingIntent);
        }
    }
}
