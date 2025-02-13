package com.amazmod.service.events.incoming;

import com.huami.watch.transport.DataBundle;

public class SleepDataBundle {

    private DataBundle dataBundle;

    public SleepDataBundle(DataBundle dataBundle) {
        this.dataBundle = dataBundle;
    }

    public DataBundle getDataBundle() {
        return dataBundle;
    }

    @Override
    public String toString() {
        return "SleepDataBundle{" +
                "dataBundle=" + dataBundle +
                '}';
    }
}
