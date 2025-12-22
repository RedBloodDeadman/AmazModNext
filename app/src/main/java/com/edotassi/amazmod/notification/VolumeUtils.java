package com.edotassi.amazmod.notification;

import android.content.Context;
import android.media.AudioManager;

public class VolumeUtils {
    public static VolumeInfo getMusicVolume(Context context) {
        AudioManager audioManager =
                (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);

        int current = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        int max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);

        return new VolumeInfo(current, max);
    }
}
