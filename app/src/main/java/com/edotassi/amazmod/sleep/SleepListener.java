package com.edotassi.amazmod.sleep;

import android.content.Context;

import com.edotassi.amazmod.event.local.SleepDataLocal;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.tinylog.Logger;

import amazmod.com.transport.data.SleepData;

public class SleepListener {
    private Context context;

    public SleepListener(Context context) {
        this.context = context;
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    public void sleepDataLocal(SleepDataLocal sleepDataLocal) {
        SleepData sleepData = sleepDataLocal.getSleepData();
        Logger.debug("sleep: Received action " + sleepData.getAction() + " from watch");
        sleepUtils.broadcast(context, sleepData);
    }
}
