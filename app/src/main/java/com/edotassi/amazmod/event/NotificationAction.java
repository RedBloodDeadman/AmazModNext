package com.edotassi.amazmod.event;

import com.huami.watch.transport.DataBundle;

import amazmod.com.transport.data.NotificationActionData;
import amazmod.com.transport.data.NotificationReplyData;

public class NotificationAction {

    private NotificationActionData notificationActionData;

    public NotificationAction(DataBundle dataBundle) {
        notificationActionData = NotificationActionData.fromDataBundle(dataBundle);
        }

    public NotificationActionData getNotificationActionData() {
        return notificationActionData;
    }
}
