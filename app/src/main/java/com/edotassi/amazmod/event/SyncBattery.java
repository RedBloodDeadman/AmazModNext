package com.edotassi.amazmod.event;

import com.huami.watch.transport.DataBundle;

import amazmod.com.transport.data.SyncBatteryData;

public class SyncBattery {

    private SyncBatteryData batteryData;

    public SyncBattery(DataBundle dataBundle) {
        batteryData = SyncBatteryData.fromDataBundle(dataBundle);
    }

    public SyncBatteryData getBatteryData() {
        return batteryData;
    }
}
