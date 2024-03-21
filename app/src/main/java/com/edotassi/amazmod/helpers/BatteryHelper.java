package com.edotassi.amazmod.helpers;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.SystemClock;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.edotassi.amazmod.AmazModApplication;
import com.edotassi.amazmod.R;
import com.edotassi.amazmod.db.model.BatteryStatusEntity;
import com.edotassi.amazmod.db.model.BatteryStatusEntity_Table;
import com.edotassi.amazmod.event.BatteryStatus;
import com.edotassi.amazmod.ui.MainActivity;
import com.pixplicity.easyprefs.library.Prefs;
import com.raizlabs.android.dbflow.config.FlowManager;
import com.raizlabs.android.dbflow.sql.language.SQLite;

import org.tinylog.Logger;

import amazmod.com.transport.Constants;
import amazmod.com.transport.data.BatteryData;

public class BatteryHelper {
    private static final char ALERT_LOW = 'L';
    private static final char ALERT_FULL = 'F';
    public static void updateBattery(BatteryStatus batteryStatus) {
        Logger.trace("BatteryStatusReceiver updateBattery");

        BatteryData batteryData = batteryStatus.getBatteryData();
        Logger.debug("[Battery] Incoming battery data: [level: {}, charging: {}, usb: {}, ac: {}, dataLastCharge: {}]",
                batteryData.getLevel(), batteryData.isCharging(), batteryData.isUsbCharge(), batteryData.isAcCharge(), batteryData.getDateLastCharge());

        long date = System.currentTimeMillis();

        BatteryStatusEntity batteryStatusEntity = new BatteryStatusEntity();
        batteryStatusEntity.setAcCharge(batteryData.isAcCharge());
        batteryStatusEntity.setCharging(batteryData.isCharging());
        batteryStatusEntity.setDate(date);
        batteryStatusEntity.setLevel(batteryData.getLevel());
        batteryStatusEntity.setDateLastCharge(batteryData.getDateLastCharge());

        //Logger.debug("TransportService batteryStatus: " + batteryStatus.toString());

        // Save data
        try {
            BatteryStatusEntity storeBatteryStatusEntity = SQLite
                    .select()
                    .from(BatteryStatusEntity.class)
                    .where(BatteryStatusEntity_Table.date.is(date))
                    .querySingle();

            if (storeBatteryStatusEntity == null)
                FlowManager.getModelAdapter(BatteryStatusEntity.class).insert(batteryStatusEntity);

        } catch (Exception ex) {
            //TODO add crashlitics
            Logger.error(ex, "[Battery] Crash while storing data, exception: {}", ex.getMessage());
        }
        // Save time of last sync
        Prefs.putLong(Constants.PREF_TIME_LAST_SYNC, SystemClock.elapsedRealtime());
    }

    public static void batteryAlert(BatteryStatus batteryStatus, Context context) {
        Logger.trace("BatteryStatusReceiver batteryAlert");
        // User options/data
        int watchBatteryAlert = Integer.parseInt(Prefs.getString(Constants.PREF_BATTERY_WATCH_ALERT,
                Constants.PREF_DEFAULT_BATTERY_WATCH_ALERT));
        boolean batteryFullAlert = Prefs.getBoolean(Constants.PREF_BATTERY_FULL_ALERT, true);
        boolean alreadyBatteryNotified = Prefs.getBoolean(Constants.PREF_BATTERY_WATCH_ALREADY_ALERTED,
                false);
        boolean alreadyChargingNotified = Prefs.getBoolean(Constants.PREF_BATTERY_WATCH_CHARGED,
                false);

        BatteryData batteryData = batteryStatus.getBatteryData();
        int battery = Math.round(batteryData.getLevel()*100);
        boolean charging = batteryData.isCharging();

        Logger.debug("[Battery Alert] Check watch battery: [level: {}%, charging: {}, limit: {}%]", battery, charging, watchBatteryAlert);

        // Check if low battery
        if (watchBatteryAlert > 0 && watchBatteryAlert > battery && !charging && !alreadyBatteryNotified) {
            Logger.debug("[Battery Alert] low watch battery...");
            // Send notification
            sendNotification(context, ALERT_LOW, watchBatteryAlert);
            Prefs.putBoolean(Constants.PREF_BATTERY_WATCH_ALREADY_ALERTED, true);
        }
        // Re-set notification
        if (watchBatteryAlert > 0 && watchBatteryAlert < battery && alreadyBatteryNotified)
            Prefs.putBoolean(Constants.PREF_BATTERY_WATCH_ALREADY_ALERTED, false);

        if (batteryFullAlert && battery > 99 && charging && !alreadyChargingNotified) {
            Logger.debug("[Battery Alert] watch fully charged...");
            // Fully charged notification
            sendNotification(context, ALERT_FULL, 100);
            Prefs.putBoolean(Constants.PREF_BATTERY_WATCH_CHARGED, true);
        }
        // Re-set notification
        if (battery < 99 && alreadyChargingNotified)
            Prefs.putBoolean(Constants.PREF_BATTERY_WATCH_CHARGED, false);
    }

    private static void sendNotification(Context context, char alertTye, int level) {
        Logger.trace("BatteryStatusReceiver sendNotification type: {}", alertTye);

        Resources res = context.getResources();
        Configuration conf = res.getConfiguration();
        conf.locale = AmazModApplication.defaultLocale;
        res.updateConfiguration(conf, context.getResources().getDisplayMetrics());

        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra("source", "notification");
        PendingIntent pendingIntent = PendingIntent.getActivity(context, (int)System.currentTimeMillis(),
                intent, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, Constants.TAG)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        switch (alertTye) {
            case ALERT_LOW:
                builder.setSmallIcon(R.drawable.ic_battery_alert_red_24dp)
                        .setContentTitle(context.getString(R.string.notification_low_battery))
                        .setContentText(context.getString(R.string.notification_low_battery_description,level + "%"));
                break;
            case ALERT_FULL:
                builder.setSmallIcon(R.drawable.ic_battery_charging_full_green_24dp)
                        .setContentTitle(context.getString(R.string.notification_watch_charged))
                        .setContentText(context.getString(R.string.notification_watch_charged_description));
                break;
            default:
                Logger.error("BatteryStatusReceiver sendNotification unknown alertType!");
        }
        notificationManager.notify(0, builder.build());
    }
}
