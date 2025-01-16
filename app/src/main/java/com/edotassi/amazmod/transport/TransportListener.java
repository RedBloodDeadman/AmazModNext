package com.edotassi.amazmod.transport;

import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.view.KeyEvent;

import com.edotassi.amazmod.event.BatteryStatus;
import com.edotassi.amazmod.event.FtpOnStateChanged;
import com.edotassi.amazmod.event.NextMusic;
import com.edotassi.amazmod.event.NotificationAction;
import com.edotassi.amazmod.event.NotificationIntent;
import com.edotassi.amazmod.event.NotificationReply;
import com.edotassi.amazmod.event.OnApEnableResult;
import com.edotassi.amazmod.event.OnApStateChanged;
import com.edotassi.amazmod.event.OtherData;
import com.edotassi.amazmod.event.PrevMusic;
import com.edotassi.amazmod.event.SilenceApplication;
import com.edotassi.amazmod.event.Sleep;
import com.edotassi.amazmod.event.SyncBattery;
import com.edotassi.amazmod.event.TakePicture;
import com.edotassi.amazmod.event.ToggleMusic;
import com.edotassi.amazmod.event.VolDown;
import com.edotassi.amazmod.event.VolMute;
import com.edotassi.amazmod.event.VolUp;
import com.edotassi.amazmod.event.local.ActionToNotificationLocal;
import com.edotassi.amazmod.event.local.FtpOnStateChangedLocal;
import com.edotassi.amazmod.event.local.IntentToNotificationLocal;
import com.edotassi.amazmod.event.local.OnApEnableResultLocal;
import com.edotassi.amazmod.event.local.OnApStateChangedLocal;
import com.edotassi.amazmod.event.local.ReplyToNotificationLocal;
import com.edotassi.amazmod.event.local.SleepDataLocal;
import com.edotassi.amazmod.helpers.BatteryHelper;
import com.edotassi.amazmod.support.SilenceApplicationHelper;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.tinylog.Logger;

import amazmod.com.transport.data.BatteryData;
import amazmod.com.transport.data.SilenceApplicationData;
import amazmod.com.transport.data.SleepData;
import amazmod.com.transport.data.SyncBatteryData;

public class TransportListener {
    private Context context;

    TransportListener(Context context) {
        this.context = context;
    }

    @Subscribe(threadMode = ThreadMode.POSTING)
    public void replyToNotification(NotificationReply notificationReply) {
        ReplyToNotificationLocal replyToNotificationLocal = new ReplyToNotificationLocal(notificationReply.getNotificationReplyData());
        Logger.debug("TransportService replyToNotification: " + notificationReply.toString());
        EventBus.getDefault().post(replyToNotificationLocal);
    }

    @Subscribe(threadMode = ThreadMode.POSTING)
    public void actionToNotification(NotificationAction notificationAction) {
        ActionToNotificationLocal actionToNotificationLocal = new ActionToNotificationLocal(notificationAction.getNotificationActionData());
        Logger.debug("TransportService actionToNotification: " + notificationAction.toString());
        EventBus.getDefault().post(actionToNotificationLocal);
    }

    @Subscribe(threadMode = ThreadMode.POSTING)
    public void intentToNotification(NotificationIntent notificationIntent) {
        IntentToNotificationLocal intentToNotificationLocal = new IntentToNotificationLocal(notificationIntent.getNotificationIntentData());
        Logger.debug("TransportService intentToNotification: " + notificationIntent.toString());
        EventBus.getDefault().post(intentToNotificationLocal);
    }

    @Subscribe(threadMode = ThreadMode.POSTING)
    public void silenceApplication(SilenceApplication silenceApplication) {
        SilenceApplicationData data = silenceApplication.getSilenceApplicationData();
        Logger.debug("TransportService silenceApplication: " + data.getPackageName() + " / Minutes: " + data.getMinutes());
        SilenceApplicationHelper.silenceAppFromNotification(data.getPackageName(), Integer.valueOf(data.getMinutes()));
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    public void volUp(VolUp volUp) {
        AudioManager mAudioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);

        if (mAudioManager.isMusicActive()) {
            mAudioManager.adjustVolume(AudioManager.ADJUST_RAISE, AudioManager.FLAG_PLAY_SOUND);
        }
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    public void volDown(VolDown volDown) {
        AudioManager mAudioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);

        if (mAudioManager.isMusicActive()) {
            mAudioManager.adjustVolume(AudioManager.ADJUST_LOWER, AudioManager.FLAG_PLAY_SOUND);
        }
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    public void volMute(VolMute volMute) {
        AudioManager mAudioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);

        if (mAudioManager.isMusicActive()) {
            mAudioManager.adjustVolume(AudioManager.ADJUST_TOGGLE_MUTE, AudioManager.FLAG_PLAY_SOUND);
        }
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    public void nextMusic(NextMusic nextMusic) {
        AudioManager mAudioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);

        if (mAudioManager.isMusicActive()) {
            long eventtime = SystemClock.uptimeMillis();

            KeyEvent downEvent = new KeyEvent(eventtime, eventtime, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_NEXT, 0);
            mAudioManager.dispatchMediaKeyEvent(downEvent);

            KeyEvent upEvent = new KeyEvent(eventtime, eventtime, KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_NEXT, 0);
            mAudioManager.dispatchMediaKeyEvent(upEvent);
        }
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    public void prevMusic(PrevMusic prevMusic) {
        AudioManager mAudioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);

        if (mAudioManager.isMusicActive()) {
            long eventtime = SystemClock.uptimeMillis();

            KeyEvent downEvent = new KeyEvent(eventtime, eventtime, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PREVIOUS, 0);
            mAudioManager.dispatchMediaKeyEvent(downEvent);

            KeyEvent upEvent = new KeyEvent(eventtime, eventtime, KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_PREVIOUS, 0);
            mAudioManager.dispatchMediaKeyEvent(upEvent);
        }
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    public void toggleMusic(ToggleMusic toggleMusic) {
        AudioManager mAudioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);

        long eventtime = SystemClock.uptimeMillis();

        if (mAudioManager.isMusicActive()) {
            KeyEvent downEvent = new KeyEvent(eventtime, eventtime, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PAUSE, 0);
            mAudioManager.dispatchMediaKeyEvent(downEvent);

            KeyEvent upEvent = new KeyEvent(eventtime, eventtime, KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_PAUSE, 0);
            mAudioManager.dispatchMediaKeyEvent(upEvent);
        } else {
            KeyEvent downEvent = new KeyEvent(eventtime, eventtime, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PLAY, 0);
            mAudioManager.dispatchMediaKeyEvent(downEvent);

            KeyEvent upEvent = new KeyEvent(eventtime, eventtime, KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_PLAY, 0);
            mAudioManager.dispatchMediaKeyEvent(upEvent);
        }
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    public void takePicture(TakePicture takePicture) {
        Logger.debug("Received take picture action!");
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra("android.intent.extra.quickCapture",true);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    public void syncBattery(SyncBattery syncBattery) {
        // Convert official data to Amazmod data
        SyncBatteryData officialData = syncBattery.getBatteryData();
        BatteryData batteryData = new BatteryData();
        batteryData.setLevel(officialData.getLevel() / 100f);
        batteryData.setCharging(officialData.isCharging());
        batteryData.setUsbCharge(false);
        batteryData.setAcCharge(false);
        if (officialData.getLevel() > 98)
            batteryData.setDateLastCharge(officialData.getChargingTime());
        else
            batteryData.setDateLastCharge(0);

        //officialData.getInt("ChargingIntervalDays", -1)

        BatteryStatus batteryStatus = new BatteryStatus(batteryData.toDataBundle());

        BatteryHelper.updateBattery(batteryStatus);
        BatteryHelper.batteryAlert(batteryStatus, context);
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    public void getOtherData(OtherData otherData) {
        Logger.debug("TransportService otherData: " + otherData.toString());
//        DataBundle otherData1 = otherData.getOtherData();
//        Set<String> keySet = otherData1.keySet();
//        System.out.println(keySet);
//        System.out.println(otherData1);
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    public void ftpOnStateChanged(FtpOnStateChanged ftpOnStateChanged) {
        Logger.debug("TransportService ftpOnStateChanged: " + ftpOnStateChanged.toString());
        FtpOnStateChangedLocal ftpOnStateChangedLocal = new FtpOnStateChangedLocal(ftpOnStateChanged.getWifiFtpStateData());
        EventBus.getDefault().post(ftpOnStateChangedLocal);
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    public void OnApStateChanged(OnApStateChanged onApStateChanged) {
        Logger.debug("TransportService onApStateChanged: " + onApStateChanged.toString());
        OnApStateChangedLocal onApStateChangedLocal = new OnApStateChangedLocal(onApStateChanged.getWifiFtpStateData());
        EventBus.getDefault().post(onApStateChangedLocal);
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    public void OnApEnableResult(OnApEnableResult onApEnableResult) {
        Logger.debug("TransportService OnApEnableResult: " + onApEnableResult.toString());
        OnApEnableResultLocal onApEnableResultLocal = new OnApEnableResultLocal(onApEnableResult.getApResultData());
        EventBus.getDefault().post(onApEnableResultLocal);
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    public void OnSleep(Sleep sleep) {
        Logger.debug("TransportService OnSleep: " + sleep.toString());
        SleepDataLocal sleepData = new SleepDataLocal(sleep.getSleepData());
        EventBus.getDefault().post(sleepData);
    }
}
