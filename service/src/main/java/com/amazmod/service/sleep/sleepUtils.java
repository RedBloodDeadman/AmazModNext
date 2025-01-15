package com.amazmod.service.sleep;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Vibrator;

import com.amazmod.service.R;
import com.amazmod.service.notifications.NotificationService;
import com.amazmod.service.sleep.sensor.sensorsStore;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.Locale;

import amazmod.com.transport.data.NotificationData;
import amazmod.com.transport.util.ImageUtils;

public class sleepUtils {
    public static float[] linkedToArray(LinkedList<Float> list) {
        Object[] objectArray = list.toArray();
        int length = objectArray.length;
        float[] finalArray = new float[length];
        for (int i = 0; i < length; i++) {
            finalArray[i] = (float) objectArray[i];
        }
        return finalArray;
    }

    public static void startTracking(Context context, boolean doHrMonitoring) {
        sleepStore.setTracking(true, context, doHrMonitoring);
    }

    public static void stopTracking(Context context) {
        sleepStore.setTracking(false, context, false);
    }

    public static void postNotification(String title, String text, Context context) {
        NotificationService notificationService = new NotificationService(context);
        NotificationData notificationData = new NotificationData();
        notificationData.setId(sleepConstants.NOTIFICATION_ID);
        notificationData.setKey(sleepConstants.NOTIFICATION_KEY);
        notificationData.setTitle(title);
        notificationData.setText(text);
        notificationData.setTime(DateFormat.getTimeInstance(DateFormat.SHORT, Locale.getDefault()).format(Calendar.getInstance().getTime()));
        notificationData.setForceCustom(false);
        notificationData.setHideButtons(true);
        notificationData.setHideReplies(true);

        // Get and set icon
        Drawable drawable = context.getResources().getDrawable(R.drawable.ic_sleepasandroid);
        notificationData.setIcon(ImageUtils.bitmap2bytes(ImageUtils.drawableToBitmap(drawable), ImageUtils.smallIconQuality));

        notificationService.post(notificationData);
    }

    public static void startHint(int repeat, Context context) {
        Vibrator v = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        long[] pattern = new long[repeat * 2 + 1];//new long[]{0, 50, 1000};
        pattern[0] = 0;
        if (repeat > 0) {
            for (int i = 1; i < repeat * 2 + 1; i++)
                pattern[i] = i % 2 == 0 ? 50 : 1000;
        }
        v.vibrate(pattern, -1);
    }

    public static void setSensorsState(boolean enabled, Context context, boolean doHrMonitoring) {
        /*boolean hrEnabled = PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(Constants.PREF_ENABLE_SAA_HEARTRATE, false);*/
        if (enabled) {
            sensorsStore.getAccelerometer().registerListener(context);
            if (doHrMonitoring)
                sensorsStore.getHrSensor().registerListener(context);
        } else {
            sensorsStore.getAccelerometer().unregisterListener(context);
            sensorsStore.getHrSensor().unregisterListener(context);
        }
    }
}
