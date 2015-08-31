package com.fsck.k9.controller;

public class Command implements Comparable<Command> {
    private Runnable runnable;
    private MessagingListener listener;
    private String description;
    private boolean isForeground;
    private int sequence;

    public Command(Runnable runnable, MessagingListener listener, String description, boolean isForeground, int sequence) {
        this.runnable = runnable;
        this.listener = listener;
        this.description = description;
        this.isForeground = isForeground;
        this.sequence = sequence;
    }

    @Override
    public int compareTo(Command other) {
        if (other.isForeground() && !isForeground()) {
            return 1;
        } else if (!other.isForeground() && isForeground()) {
            return -1;
        } else {
            return (getSequence() - other.getSequence());
        }
    }

    public Runnable getRunnable() {
        return runnable;
    }

    public void setRunnable(Runnable runnable) {
        this.runnable = runnable;
    }

    public MessagingListener getListener() {
        return listener;
    }

    public void setListener(MessagingListener listener) {
        this.listener = listener;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isForeground() {
        return isForeground;
    }

    public int getSequence() {
        return sequence;
    }
}
