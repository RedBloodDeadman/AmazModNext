package com.edotassi.amazmod.event.local;

import amazmod.com.transport.data.NotificationActionData;

public class ActionToNotificationLocal {

    private NotificationActionData notificationActionData;

    public ActionToNotificationLocal(NotificationActionData notificationActionData) {
        this.notificationActionData = notificationActionData;
    }

    public NotificationActionData getNotificationActionData() {
        return notificationActionData;
    }
}
