package com.edotassi.amazmod.event;

import com.huami.watch.transport.DataBundle;

import amazmod.com.transport.data.NotificationActionData;
import amazmod.com.transport.data.NotificationIntentData;

public class NotificationIntent {

    private NotificationIntentData notificationIntentData;

    public NotificationIntent(DataBundle dataBundle) {
        notificationIntentData = NotificationIntentData.fromDataBundle(dataBundle);
        }

    public NotificationIntentData getNotificationIntentData() {
        return notificationIntentData;
    }
}
