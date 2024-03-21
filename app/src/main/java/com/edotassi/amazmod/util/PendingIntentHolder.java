package com.edotassi.amazmod.util;

import android.annotation.SuppressLint;
import android.app.PendingIntent;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class PendingIntentHolder {
    private static String TAG = "PendingIntentHolder";

    public String getTAG() {
        return TAG;
    }

    private static final int CACHE_SIZE = 128;
    private static int nextCachedIntentId = 0;
    @SuppressLint("UseSparseArrays")
    private static Map<Integer, PendingIntent> cachedIntents = new HashMap<>();
    private static Map<Integer, Integer> savedNotificationIdToCachedIntent = new HashMap<>();

    public static PendingIntent read(Integer savedSbnId) {
        Integer intentId = savedNotificationIdToCachedIntent.get(savedSbnId);
        if (intentId == null) {
            return null;
        } else {
            return cachedIntents.get(intentId);
        }
    }

    public static void save(Integer savedSbnId, PendingIntent contentIntent) {
        // check if intent already exists
        Integer oldCachedIntentId = getKey(cachedIntents, contentIntent);

        if (oldCachedIntentId != null) {
            //Crashlytics.log(Log.DEBUG, TAG, "existed intent");
            savedNotificationIdToCachedIntent.put(savedSbnId, oldCachedIntentId);
        } else {
            //Crashlytics.log(Log.DEBUG, TAG, "new intent");
            Integer newCachedIntentId = nextCachedIntentId++;
            cachedIntents.put(newCachedIntentId, contentIntent);
            savedNotificationIdToCachedIntent.put(savedSbnId, newCachedIntentId);

            if (cachedIntents.size() > CACHE_SIZE) {
                //Crashlytics.log(Log.DEBUG, TAG, "clean up cache");
                Integer oldestCachedIntentId = Collections.min(cachedIntents.keySet());
                cachedIntents.remove(oldestCachedIntentId);

                Iterator<Map.Entry<Integer, Integer>> iterator = savedNotificationIdToCachedIntent.entrySet().iterator();
                Map.Entry<Integer, Integer> map;
                while (iterator.hasNext()) {
                    map = iterator.next();
                    if (map.getValue().equals(oldestCachedIntentId)) {
                        iterator.remove();
                    }
                }
            }
        }

    }

    public static <K, V> K getKey(Map<K, V> map, V value) {
        for (Map.Entry<K, V> entry : map.entrySet()) {
            try {
                if (value != null && entry != null) {
                    if (value.equals(entry.getValue())) {
                        return entry.getKey();
                    }
                }
            } catch (NullPointerException e) {
                e.printStackTrace();
            }
        }
        return null;
    }
}
