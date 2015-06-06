package com.fsck.k9;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class FollowUpMailInformation {
    //POJO with all relevant information about a follow-up-mail
    private String accountEmailAddress;
    private String mailUuid;
    private String messageId;
    private long notificationTimestamp;

    public FollowUpMailInformation() {
        //default constructor for mapper
    }

    public FollowUpMailInformation(String accountEmailAddress, String mailUuid, String messageId,
                                   long notificationTimestamp) {
        this.accountEmailAddress = accountEmailAddress;
        this.mailUuid = mailUuid;
        this.messageId = messageId;
        this.notificationTimestamp = notificationTimestamp;
    }

    //getter and setter methods
    public String getAccountEmailAddress() {
        return accountEmailAddress;
    }

    public String getMailUuid(){
        return mailUuid;
    }

    public String getMessageId(){
        return messageId;
    }

    public long getNotificationTimestamp(){
        return notificationTimestamp;
    }

    public void setAccountEmailAddress(String accountEmailAddress) {
        this.accountEmailAddress = accountEmailAddress;
    }

    public void setMailUuid(String uuid) {
        this.mailUuid = uuid;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public void setNotificationTimestamp(long notificationTimestamp) {
        this.notificationTimestamp = notificationTimestamp;
    }

    @Override
    public String toString() {
        return "FollowUpMailInformation [accountEmailAddress=" + accountEmailAddress + ", mailUuid=" +
                mailUuid + ", " + "messageId=" + messageId + ", notificationTimestamp=" +
                notificationTimestamp + "]";
    }
}

