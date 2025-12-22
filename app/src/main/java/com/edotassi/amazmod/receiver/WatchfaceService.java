package com.edotassi.amazmod.receiver;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;

import androidx.annotation.Nullable;

public class WatchfaceService extends Service {
    private LocalBinder localBinder = new LocalBinder();

    @Override
    public void onCreate() {
        super.onCreate();

        BatteryStatusReceiver.startBatteryReceiver(this);
        WatchfaceReceiver.startWatchfaceReceiver(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return localBinder;
    }

    public class LocalBinder extends Binder {
        public WatchfaceService getService() {
            return WatchfaceService.this;
        }
    }
}
