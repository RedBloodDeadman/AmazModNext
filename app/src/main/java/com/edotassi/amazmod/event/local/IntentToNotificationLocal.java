package com.edotassi.amazmod.event.local;

import amazmod.com.transport.data.NotificationIntentData;

public class IntentToNotificationLocal {

    private NotificationIntentData notificationIntentData;

    public IntentToNotificationLocal(NotificationIntentData notificationIntentData) {
        this.notificationIntentData = notificationIntentData;
    }

    public NotificationIntentData getNotificationIntentData() {
        return notificationIntentData;
    }
}
