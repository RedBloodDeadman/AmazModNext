package com.amazmod.service.events.incoming;

import com.huami.watch.transport.DataBundle;

public class ActionIncomingCall {

    private DataBundle dataBundle;

    public ActionIncomingCall(DataBundle dataBundle) {
        this.dataBundle = dataBundle;
    }

    public DataBundle getDataBundle() {
        return dataBundle;
    }

    @Override
    public String toString() {
        return "ActionIncomingCall{" +
                "dataBundle=" + dataBundle +
                '}';
    }
}
