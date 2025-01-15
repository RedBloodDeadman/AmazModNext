package com.edotassi.amazmod.event.local;

import amazmod.com.transport.data.ApResultData;
import amazmod.com.transport.data.SleepData;

public class SleepDataLocal {
    private SleepData sleepData;

    public SleepDataLocal(SleepData sleepData) {
        this.sleepData = sleepData;
    }

    public SleepData getSleepData() {
        return sleepData;
    }
}
