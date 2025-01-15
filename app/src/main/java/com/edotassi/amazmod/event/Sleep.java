package com.edotassi.amazmod.event;

import com.huami.watch.transport.DataBundle;

import amazmod.com.transport.data.SleepData;

public class Sleep {

    private SleepData sleepData;

    public Sleep(DataBundle dataBundle) {
        this.sleepData = SleepData.fromDataBundle(dataBundle);
    }

    public SleepData getSleepData() {
        return sleepData;
    }

    @Override
    public String toString() {
        return "Sleep{" +
                "sleepData=" + sleepData +
                '}';
    }
}
