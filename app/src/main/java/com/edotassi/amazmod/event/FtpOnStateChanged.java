package com.edotassi.amazmod.event;

import com.huami.watch.transport.DataBundle;

import amazmod.com.transport.data.WifiFtpNewStateData;

public class FtpOnStateChanged {
    private WifiFtpNewStateData wifiFtpNewStateData;

    public FtpOnStateChanged(DataBundle dataBundle) {
        wifiFtpNewStateData = WifiFtpNewStateData.fromDataBundle(dataBundle);
    }

    public WifiFtpNewStateData getWifiFtpStateData() {
        return wifiFtpNewStateData;
    }
}
