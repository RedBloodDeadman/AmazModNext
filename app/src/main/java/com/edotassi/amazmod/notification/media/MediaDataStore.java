package com.edotassi.amazmod.notification.media;

import java.util.concurrent.atomic.AtomicReference;

import amazmod.com.transport.data.MediaData;

public final class MediaDataStore {

    private static final AtomicReference<MediaData> LAST_INFO =
            new AtomicReference<>();

    private MediaDataStore() {}

    public static void update(MediaData info) {
        LAST_INFO.set(info);
    }

    public static MediaData get() {
        return LAST_INFO.get();
    }

    public static boolean hasData() {
        return LAST_INFO.get() != null;
    }

    public static void clear() {
        LAST_INFO.set(null);
    }
}
