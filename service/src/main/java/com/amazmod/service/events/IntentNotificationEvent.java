package com.amazmod.service.events;

public class IntentNotificationEvent {

    private Integer sbnId;
    private String packageName;

    public IntentNotificationEvent(Integer sbnId, String packageName) {
        this.sbnId = sbnId;
        this.packageName = packageName;
    }

    public Integer getSbnId() {
        return sbnId;
    }

    public String getPackageName() {
        return packageName;
    }
}
