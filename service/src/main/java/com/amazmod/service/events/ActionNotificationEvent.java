package com.amazmod.service.events;

public class ActionNotificationEvent {

    private Integer sbnId;
    private String title;

    public ActionNotificationEvent(Integer sbnId, String title) {
        this.sbnId = sbnId;
        this.title = title;
    }

    public Integer getSbnId() {
        return sbnId;
    }

    public String getTitle() {
        return title;
    }
}
