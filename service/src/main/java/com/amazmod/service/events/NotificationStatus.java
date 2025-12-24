package com.amazmod.service.events;

public class NotificationStatus {
    private String action;
    private String key;

    public NotificationStatus(String action, String key) {
        this.action = action;
        this.key = key;
    }

    public String getAction() {
        return action;
    }

    public String getKey() {
        return key;
    }
}
