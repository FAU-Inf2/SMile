package com.fsck.k9.controller;

import com.fsck.k9.Account;

public class Memory {
    private Account account;
    private String folderName;
    private MemorizingState syncingState = null;
    private MemorizingState sendingState = null;
    private MemorizingState pushingState = null;
    private MemorizingState processingState = null;
    private String failureMessage = null;

    private int syncingTotalMessagesInMailbox;
    private int syncingNumNewMessages;

    private int folderCompleted = 0;
    private int folderTotal = 0;
    private String processingCommandTitle = null;

    public Memory(Account nAccount, String nFolderName) {
        account = nAccount;
        folderName = nFolderName;
    }

    String getKey() {
        return MessagingController.getMemoryKey(account, folderName);
    }

    public Account getAccount() {
        return account;
    }

    public void setAccount(Account account) {
        this.account = account;
    }

    public String getFolderName() {
        return folderName;
    }

    public void setFolderName(String folderName) {
        this.folderName = folderName;
    }

    public MemorizingState getSyncingState() {
        return syncingState;
    }

    public void setSyncingState(MemorizingState syncingState) {
        this.syncingState = syncingState;
    }

    public MemorizingState getSendingState() {
        return sendingState;
    }

    public void setSendingState(MemorizingState sendingState) {
        this.sendingState = sendingState;
    }

    public MemorizingState getPushingState() {
        return pushingState;
    }

    public void setPushingState(MemorizingState pushingState) {
        this.pushingState = pushingState;
    }

    public MemorizingState getProcessingState() {
        return processingState;
    }

    public void setProcessingState(MemorizingState processingState) {
        this.processingState = processingState;
    }

    public String getFailureMessage() {
        return failureMessage;
    }

    public void setFailureMessage(String failureMessage) {
        this.failureMessage = failureMessage;
    }

    public int getSyncingTotalMessagesInMailbox() {
        return syncingTotalMessagesInMailbox;
    }

    public void setSyncingTotalMessagesInMailbox(int syncingTotalMessagesInMailbox) {
        this.syncingTotalMessagesInMailbox = syncingTotalMessagesInMailbox;
    }

    public int getSyncingNumNewMessages() {
        return syncingNumNewMessages;
    }

    public void setSyncingNumNewMessages(int syncingNumNewMessages) {
        this.syncingNumNewMessages = syncingNumNewMessages;
    }

    public int getFolderCompleted() {
        return folderCompleted;
    }

    public void setFolderCompleted(int folderCompleted) {
        this.folderCompleted = folderCompleted;
    }

    public int getFolderTotal() {
        return folderTotal;
    }

    public void setFolderTotal(int folderTotal) {
        this.folderTotal = folderTotal;
    }

    public String getProcessingCommandTitle() {
        return processingCommandTitle;
    }

    public void setProcessingCommandTitle(String processingCommandTitle) {
        this.processingCommandTitle = processingCommandTitle;
    }

    public enum MemorizingState {
        STARTED,
        FINISHED,
        FAILED
    }
}
