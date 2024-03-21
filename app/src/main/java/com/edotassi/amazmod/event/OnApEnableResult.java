package com.edotassi.amazmod.event;

import com.huami.watch.transport.DataBundle;

import amazmod.com.transport.data.ApResultData;
import amazmod.com.transport.data.WifiFtpNewStateData;

public class OnApEnableResult {
    private ApResultData apResultData;

    public OnApEnableResult(DataBundle dataBundle) {
        apResultData = ApResultData.fromDataBundle(dataBundle);
    }

    public ApResultData getApResultData() {
        return apResultData;
    }
}
