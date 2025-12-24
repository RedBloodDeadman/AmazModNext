package com.amazmod.service.helper;

import com.amazmod.service.events.NotificationStatus;

import java.util.ArrayList;
import java.util.List;

import amazmod.com.transport.data.MediaData;

public class NotificationStatusManager {
    private static NotificationStatusManager instance;
    private NotificationStatus currentData;
    private List<DataListener> listeners = new ArrayList<>();

    public static synchronized NotificationStatusManager getInstance() {
        if (instance == null) {
            instance = new NotificationStatusManager();
        }
        return instance;
    }

    private NotificationStatusManager() {}

    public void addListener(DataListener listener) {
        listeners.add(listener);
        if (currentData != null) {
            listener.onDataUpdated(currentData);
        }
    }

    public void removeListener(DataListener listener) {
        listeners.remove(listener);
    }

    public void updateData(NotificationStatus data) {
        currentData = data;
        for (DataListener listener : listeners) {
            listener.onDataUpdated(data);
        }
    }

    public interface DataListener {
        void onDataUpdated(NotificationStatus data);
    }
}
