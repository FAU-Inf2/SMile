package com.fsck.k9.mail;


import java.util.Date;

public class FollowUp {

    private int id;
    private String title;
    private Date remindTime;
    private Message message;

    public FollowUp() {

    }

    public FollowUp(String title, Date remindTime) {
        this(title, remindTime, null);
    }

    public FollowUp(String title, Date remindTime, Message reference) {
        setTitle(title);
        setRemindTime(remindTime);
        setReference(reference);
    }

    public Date getRemindTime() {
        return remindTime;
    }

    public void setRemindTime(Date remindTime) {
        this.remindTime = remindTime;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Message getReference() {
        return message;
    }

    public void setReference(Message reference) {
        this.message = reference;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

}
