package com.amazmod.service.util;

import android.annotation.SuppressLint;

import java.util.concurrent.TimeUnit;

public class TimeUtils {

    @SuppressLint("DefaultLocale")
    public static String msToHumaTime(long ms) {
        long HH = TimeUnit.MILLISECONDS.toHours(ms);
        long MM = TimeUnit.MILLISECONDS.toMinutes(ms) % 60;
        long SS = TimeUnit.MILLISECONDS.toSeconds(ms) % 60;
        if (HH != 0) {
            return String.format("%02d:%02d:%02d", HH, MM, SS);
        } else {
            return String.format("%02d:%02d", MM, SS);
        }
    }
}
