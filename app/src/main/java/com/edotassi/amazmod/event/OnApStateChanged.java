package com.edotassi.amazmod.event;

import com.huami.watch.transport.DataBundle;

import amazmod.com.transport.data.WifiFtpNewStateData;

public class OnApStateChanged {
    private WifiFtpNewStateData wifiFtpNewStateData;

    public OnApStateChanged(DataBundle dataBundle) {
        wifiFtpNewStateData = WifiFtpNewStateData.fromDataBundle(dataBundle);
    }

    public WifiFtpNewStateData getWifiFtpStateData() {
        return wifiFtpNewStateData;
    }
}
