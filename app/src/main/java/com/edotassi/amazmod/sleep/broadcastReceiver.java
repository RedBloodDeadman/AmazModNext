package com.edotassi.amazmod.sleep;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;

import com.edotassi.amazmod.AmazModApplication;
import com.edotassi.amazmod.event.OtherData;
import com.edotassi.amazmod.event.Sleep;
import com.edotassi.amazmod.transport.TransportService;
import com.edotassi.amazmod.watch.Watch;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.Task;
import com.huami.watch.transport.DataBundle;
import com.huami.watch.transport.DataTransportResult;

import org.tinylog.Logger;

import java.util.Objects;

import amazmod.com.transport.Constants;
import amazmod.com.transport.Transport;
import amazmod.com.transport.data.SleepData;

import static amazmod.com.transport.data.SleepData.actions;
import static com.edotassi.amazmod.sleep.sleepUtils.*;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;

public class broadcastReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (PreferenceManager.getDefaultSharedPreferences(context).getBoolean(Constants.PREF_ENABLE_SLEEP_AS_ANDROID, false)) {
            SleepData sleepData = new SleepData();
            sleepData.setAction(-1);
            Logger.debug("Received intent with action: " + Objects.requireNonNull(intent.getAction()));
            switch (Objects.requireNonNull(intent.getAction())) {
                case "com.urbandroid.sleep.watch.CHECK_CONNECTED":
                    if (AmazModApplication.isWatchConnected() && BluetoothAdapter.getDefaultAdapter().isEnabled()) {
                        Logger.debug("Sleep check connected returning true");
                        sendIntent("com.urbandroid.sleep.watch.CONFIRM_CONNECTED", context);
                    } else Logger.debug("Sleep check connected returning false");
                    break;
                case "com.urbandroid.sleep.watch.START_TRACKING":
                    boolean doHrMonitoring = intent.getBooleanExtra("DO_HR_MONITORING", false);
                    Logger.debug("doHrMonitoring: " + doHrMonitoring);
                    sleepData.setAction(actions.ACTION_START_TRACKING);
                    sleepData.setDoHrMonitoring(doHrMonitoring);
                    break;
                case "com.urbandroid.sleep.watch.STOP_TRACKING":
                    sleepData.setAction(actions.ACTION_STOP_TRACKING);
                    break;
                case "com.urbandroid.sleep.watch.SET_PAUSE":
                    long timestamp = intent.getLongExtra("TIMESTAMP", 0L);
                    Logger.debug("timestamp: " + timestamp);
                    sleepData.setAction(actions.ACTION_SET_PAUSE);
                    sleepData.setTimestamp(timestamp);
                    break;
                case "com.urbandroid.sleep.watch.SET_SUSPENDED":
                    sleepData.setAction(actions.ACTION_SET_SUSPENDED);
                    sleepData.setSuspended(intent.getBooleanExtra("SUSPENDED", false));
                    break;
                case "com.urbandroid.sleep.watch.SET_BATCH_SIZE":
                    sleepData.setAction(actions.ACTION_SET_BATCH_SIZE);
                    sleepData.setBatchsize(intent.getLongExtra("SIZE", 12));
                    break;
                case "com.urbandroid.sleep.watch.START_ALARM":
                    sleepData.setAction(actions.ACTION_START_ALARM);
                    sleepData.setDelay(intent.getIntExtra("DELAY", 10000));
                    break;
                case "com.urbandroid.sleep.watch.STOP_ALARM":
                    sleepData.setAction(actions.ACTION_STOP_ALARM);
                    sleepData.setHour(intent.getIntExtra("HOUR", 0));
                    sleepData.setMinute(intent.getIntExtra("MINUTE", 0));
                    sleepData.setTimestamp(intent.getLongExtra("TIMESTAMP", 0));
                    break;
                case "com.urbandroid.sleep.watch.SHOW_NOTIFICATION":
                    sleepData.setAction(actions.ACTION_SHOW_NOTIFICATION);
                    sleepData.setTitle(intent.getStringExtra("TITLE"));
                    sleepData.setText(intent.getStringExtra("TEXT"));
                    break;
                case "com.urbandroid.sleep.watch.HINT":
                    Logger.debug("Sending " + intent.getIntExtra("REPEAT", -1) + " hints");
                    sleepData.setAction(actions.ACTION_HINT);
                    sleepData.setRepeat(intent.getIntExtra("REPEAT", -1));
                    break;
                default:
                    break;
            }
            if (sleepData.getAction() != -1) {
                Watch.get().sendSleepData(sleepData);
                Logger.debug("sleep: Detected intent \"" + intent.getAction() + "\", sending action " + sleepData.getAction());
            } else {
                Logger.debug("sleep: broadcastReceiver: Received unknown intent: " + intent.getAction());
            }
        }
    }
}
