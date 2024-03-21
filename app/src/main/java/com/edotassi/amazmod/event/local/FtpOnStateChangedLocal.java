package com.edotassi.amazmod.event.local;

import com.huami.watch.transport.DataBundle;

import amazmod.com.transport.data.WifiFtpNewStateData;

public class FtpOnStateChangedLocal {
    private WifiFtpNewStateData wifiFtpNewStateData;

    public FtpOnStateChangedLocal(WifiFtpNewStateData wifiFtpNewStateData) {
        this.wifiFtpNewStateData = wifiFtpNewStateData;
    }

    public WifiFtpNewStateData getWifiFtpStateData() {
        return wifiFtpNewStateData;
    }
}
