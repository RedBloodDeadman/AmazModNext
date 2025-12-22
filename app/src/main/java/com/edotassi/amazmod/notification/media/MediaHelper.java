package com.edotassi.amazmod.notification.media;

import static com.edotassi.amazmod.notification.VolumeUtils.getMusicVolume;

import android.content.Context;
import android.util.Log;

import com.edotassi.amazmod.notification.VolumeInfo;
import com.edotassi.amazmod.watch.Watch;

import amazmod.com.transport.data.MediaData;

public class MediaHelper {

    public static void postMedia(Context context){
        if (MediaDataStore.hasData()) {
            MediaData mediaData = MediaDataStore.get();

            VolumeInfo musicVolume = getMusicVolume(context);
            mediaData.setVolume(musicVolume.getCurrent());
            mediaData.setMaxVolume(musicVolume.getMax());

            Log.d("MediaHelper", mediaData.toString());
            Watch.get().postMediaInfo(mediaData);
        }
    }
}
