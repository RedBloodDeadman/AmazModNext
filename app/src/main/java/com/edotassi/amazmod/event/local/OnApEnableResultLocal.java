package com.edotassi.amazmod.event.local;

import com.huami.watch.transport.DataBundle;

import amazmod.com.transport.data.ApResultData;

public class OnApEnableResultLocal {
    private ApResultData apResultData;

    public OnApEnableResultLocal(ApResultData apResultData) {
        this.apResultData = apResultData;
    }

    public ApResultData getApResultData() {
        return apResultData;
    }
}
