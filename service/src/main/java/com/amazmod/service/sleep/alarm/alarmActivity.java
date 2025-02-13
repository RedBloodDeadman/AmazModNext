package com.amazmod.service.sleep.alarm;

import static amazmod.com.transport.data.SleepData.actions;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioAttributes;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.os.Vibrator;
import android.widget.Button;
import android.widget.TextView;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.amazmod.service.MainService;
import com.amazmod.service.R;
import com.amazmod.service.util.ButtonListener;
import com.amazmod.service.util.SystemProperties;
import com.huami.watch.transport.DataBundle;

import org.tinylog.Logger;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import amazmod.com.transport.Transport;
import amazmod.com.transport.data.SleepData;

public class alarmActivity extends Activity {

    public static final String INTENT_CLOSE = "com.amazmod.alarm.action.close";

    private static final AudioAttributes VIBRATION_ATTRIBUTES = new AudioAttributes.Builder().setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT).build();

    private TextView time;
    private Button snooze, dismiss;

    private ButtonListener buttonListener = new ButtonListener();
    private Handler timeHandler;
    private Vibrator vibrator;

    PowerManager.WakeLock wakeLock = null;

    LocalBroadcastManager mLocalBroadcastManager;
    BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(INTENT_CLOSE))
                stop();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alarm);
        init();
        setWakeLock();
        setupBtnListener();
        //Create broadcast receiver to finish activity when received signal
        mLocalBroadcastManager = LocalBroadcastManager.getInstance(this);
        IntentFilter mIntentFilter = new IntentFilter();
        mIntentFilter.addAction(INTENT_CLOSE);
        mLocalBroadcastManager.registerReceiver(mBroadcastReceiver, mIntentFilter);
        setupTime();
        vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        long[] VIBRATION_PATTERN = new long[]{
                getIntent().getIntExtra("DELAY", 0),
                200, 100, 200, 100, 200, 100, 0, 400};
        vibrator.vibrate(VIBRATION_PATTERN, 0 /*Until cancelled*/, VIBRATION_ATTRIBUTES);
    }

    private void setupTime() {
        timeHandler = new Handler(Looper.getMainLooper());
        timeHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                time.setText(new SimpleDateFormat("HH:mm", Locale.US).format(new Date()));
                timeHandler.postDelayed(this, 1000);
            }
        }, 10);
    }

    private void init() {
        time = findViewById(R.id.alarm_time);
        snooze = findViewById(R.id.alarm_snooze);
        dismiss = findViewById(R.id.alarm_dismiss);
        snooze.setOnClickListener(view -> showActionDialog(() -> stop(actions.ACTION_SNOOZE_FROM_WATCH)));
        dismiss.setOnClickListener(view -> showActionDialog(() -> stop(actions.ACTION_DISMISS_FROM_WATCH)));
    }

    public void onDestroy() {
        super.onDestroy();
        vibrator.cancel();
        if (timeHandler != null)
            timeHandler.removeCallbacksAndMessages(null);
        timeHandler = null;
        releaseWakeLock();
        buttonListener.stop();
    }

    private void stop(int action) {
        SleepData sleepData = new SleepData();
        sleepData.setAction(action);
        Logger.debug("Sleep Data (alarm): " + sleepData);
        MainService.sendSleep(Transport.SLEEP_DATA, sleepData);
        stop();
    }

    private void stop() {
        vibrator.cancel();
        buttonListener.stop();
        finish();
    }

    private void setWakeLock() {
        //Wake the screen
        PowerManager pm = (PowerManager) this.getSystemService(Context.POWER_SERVICE);
        wakeLock = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK
                | PowerManager.ACQUIRE_CAUSES_WAKEUP
                | PowerManager.ON_AFTER_RELEASE, "AmazMod:sleepasandroid_alarm");

        wakeLock.acquire();
    }

    private void releaseWakeLock() {
        if (wakeLock != null && wakeLock.isHeld())
            wakeLock.release();
    }

    private void showActionDialog(Runnable runOnFinish){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(getResources().getString(R.string.confirmation))
                .setPositiveButton(android.R.string.yes, (dialog, which) -> runOnFinish.run())
                .setNegativeButton(android.R.string.no, (dialog, which) -> Logger.info("Clicked no"));
        builder.show();
    }

    private void setupBtnListener(){
        Handler btnHandler = new Handler();
        buttonListener.start(this, keyEvent -> {
            if ((SystemProperties.isPace() || SystemProperties.isVerge()) && keyEvent.getCode() == ButtonListener.KEY_CENTER)
                btnHandler.post(() -> dismiss.performClick());
            if (SystemProperties.isStratos())
                switch(keyEvent.getCode()){
                    case ButtonListener.KEY_UP:
                        btnHandler.post(() -> dismiss.performClick());
                        break;
                    case ButtonListener.KEY_DOWN:
                        btnHandler.post(() -> snooze.performClick());
                        break;
                }
            if (SystemProperties.isStratos3())
                switch(keyEvent.getCode()){
                    case ButtonListener.S3_KEY_MIDDLE_UP:
                        btnHandler.post(() -> dismiss.performClick());
                        break;
                    case ButtonListener.S3_KEY_MIDDLE_DOWN:
                        btnHandler.post(() -> snooze.performClick());
                        break;
                }
        });
    }
}