package com.amazmod.service.events;

public class ReplyNotificationEvent {

    private Integer sbnId;
    private String title;
    private String message;


    public ReplyNotificationEvent(Integer sbnId, String title, String message) {
        this.sbnId = sbnId;
        this.title = title;
        this.message = message;
    }

    public Integer getSbnId() {
        return sbnId;
    }

    public String getTitle() {
        return title;
    }

    public String getMessage() {
        return message;
    }
}
