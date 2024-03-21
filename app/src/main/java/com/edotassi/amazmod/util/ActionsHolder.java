package com.edotassi.amazmod.util;

import android.annotation.SuppressLint;
import android.app.Notification;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class ActionsHolder {
    //Notification.Action[] notificationActions = sbn.getNotification().actions;

    private static String TAG = "ActionsHolder";

    public String getTAG() {
        return TAG;
    }

    private static final int CACHE_SIZE = 128;
    private static int nextCachedIntentId = 0;
    @SuppressLint("UseSparseArrays")
    private static Map<Integer, Notification.Action[]> cachedActions = new HashMap<Integer, Notification.Action[]>();
    private static Map<Integer, Integer> savedNotificationKeyToCachedActions = new HashMap<Integer, Integer>();

    public static Notification.Action[] read(Integer savedSbnId) {
        Integer intentId = savedNotificationKeyToCachedActions.get(savedSbnId);
        if (intentId == null) {
            return null;
        } else {
            return cachedActions.get(intentId);
        }
    }

    public static void save(Integer savedSbnId, Notification.Action[] contentActions) {
        // check if intent already exists
        Integer oldCachedIntentId = getKey(cachedActions, contentActions);

        if (oldCachedIntentId != null) {
            savedNotificationKeyToCachedActions.put(savedSbnId, oldCachedIntentId);
        } else {
            Integer newCachedIntentId = nextCachedIntentId++;
            cachedActions.put(newCachedIntentId, contentActions);
            savedNotificationKeyToCachedActions.put(savedSbnId, newCachedIntentId);

            if (cachedActions.size() > CACHE_SIZE) {
                Integer oldestCachedIntentId = Collections.min(cachedActions.keySet());
                cachedActions.remove(oldestCachedIntentId);

                Iterator<Map.Entry<Integer, Integer>> iterator = savedNotificationKeyToCachedActions.entrySet().iterator();
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
