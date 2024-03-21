package com.amazmod.service.support;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

import amazmod.com.transport.data.NotificationData;
import amazmod.com.transport.util.ImageUtils;

public class NotificationInfo {

    private String notificationTitle;
    private String notificationText;
    private String notificationTime;
    private Drawable icon;
    private byte[] largeIconData;
    private String key;
    private String id;

    public NotificationInfo(){}

    public NotificationInfo(String notificationTitle, String notificationText, String notificationTime, Drawable icon, byte[] largeIconData, String key, String id) {
        this.notificationTitle = notificationTitle;
        this.notificationTime = notificationTime;
        this.notificationText = notificationText;
        this.icon = icon;
        this.largeIconData = largeIconData;
        this.key = key;
        this.id = id;
    }

    public NotificationInfo(NotificationData notificationData, String key) {
        this.notificationTitle = notificationData.getTitle();
        this.notificationText = notificationData.getText();
        this.notificationTime = notificationData.getTime();
        this.largeIconData = notificationData.getLargeIcon();

        byte[] iconData = notificationData.getIcon();

        this.icon = new BitmapDrawable(Resources.getSystem(), ImageUtils.bytes2Bitmap(iconData));

        this.key = key;

        this.id = key.substring(key.lastIndexOf("|") + 1);

    }

    public String getNotificationTitle() {
        return this.notificationTitle;
    }

    public String getNotificationTime() {
        return this.notificationTime;
    }

    public String getNotificationText() { return notificationText; }

    public Drawable getIcon() {
        return this.icon;
    }

    public byte[] getLargeIconData() {
        return largeIconData;
    }

    public String getKey() {
        return this.key;
    }

    public String getId() {
        return this.id;
    }

}
