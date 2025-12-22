package com.amazmod.service.helper;

import java.util.ArrayList;
import java.util.List;

import amazmod.com.transport.data.MediaData;

public class MediaDataManager {
    private static MediaDataManager instance;
    private MediaData currentData;
    private List<DataListener> listeners = new ArrayList<>();

    public static synchronized MediaDataManager getInstance() {
        if (instance == null) {
            instance = new MediaDataManager();
        }
        return instance;
    }

    private MediaDataManager() {}

    public void addListener(DataListener listener) {
        listeners.add(listener);
        if (currentData != null) {
            listener.onDataUpdated(currentData);
        }
    }

    public void removeListener(DataListener listener) {
        listeners.remove(listener);
    }

    public void updateData(MediaData data) {
        currentData = data;
        for (DataListener listener : listeners) {
            listener.onDataUpdated(data);
        }
    }

    public interface DataListener {
        void onDataUpdated(MediaData data);
    }
}
