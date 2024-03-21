package com.edotassi.amazmod.event.local;

import com.huami.watch.transport.DataBundle;

import amazmod.com.transport.data.WifiFtpNewStateData;

public class OnApStateChangedLocal {
    private WifiFtpNewStateData wifiFtpNewStateData;

    public OnApStateChangedLocal(WifiFtpNewStateData wifiFtpNewStateData) {
        this.wifiFtpNewStateData = wifiFtpNewStateData;
    }

    public WifiFtpNewStateData getWifiFtpStateData() {
        return wifiFtpNewStateData;
    }
}
