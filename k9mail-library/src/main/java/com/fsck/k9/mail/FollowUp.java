package com.fsck.k9.mail;


import java.util.Date;

public class FollowUp {

    private int id;
    private String title;
    private Date remindTime;
    private Message message;
    private long folderId;

    public FollowUp() {

    }

    public FollowUp(String title, Date remindTime) {
        this(title, remindTime, null, -1);
    }

    public FollowUp(String title, Date remindTime, Message reference) {
        this(title, remindTime, reference, -1);
    }

    public FollowUp(String title, Date remindTime, long folderId) {
        this(title, remindTime, null, folderId);
    }

    public FollowUp(String title, Date remindTime, Message reference, long folderId) {
        setTitle(title);
        setRemindTime(remindTime);
        setReference(reference);
        setFolderId(folderId);
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

    public long getFolderId() {
        return folderId;
    }

    public void setFolderId(long folderId) {
        this.folderId = folderId;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("Id: ");
        builder.append(getId());
        builder.append(", FolderId: ");
        builder.append(getFolderId());
        builder.append(", MessageId: ");
        builder.append(getReference().getId());
        builder.append(", MessageUID: ");
        builder.append(getReference().getUid());
        builder.append(", RemindTime: ");
        builder.append(getRemindTime());

        return builder.toString();
    }
}
