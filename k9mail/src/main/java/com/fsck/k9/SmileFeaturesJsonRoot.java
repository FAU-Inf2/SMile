package com.fsck.k9;

import java.util.HashSet;

public class SmileFeaturesJsonRoot {
    // Root element of json-script containing HashSets of POJOs.

    private HashSet<FollowUpMailInformation> allFollowUpMails;

    public SmileFeaturesJsonRoot() {
        this.allFollowUpMails = new HashSet<FollowUpMailInformation>();
    }

    public HashSet<FollowUpMailInformation> getAllFollowUpMails() {
        return allFollowUpMails;
    }

    public void addFollowUpMailInformation(FollowUpMailInformation newFollowUpMail) {
        allFollowUpMails.add(newFollowUpMail);
    }

    public void setAllFollowUpMails(HashSet<FollowUpMailInformation> newFollowUpMailList) {
        this.allFollowUpMails = newFollowUpMailList;
    }

    public void mergeAllFollowUpMails(HashSet<FollowUpMailInformation> newFollowUpMailList) {
        this.allFollowUpMails.addAll(newFollowUpMailList);
    }
}