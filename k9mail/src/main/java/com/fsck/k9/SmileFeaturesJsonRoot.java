package com.fsck.k9;

import com.fsck.k9.mail.FollowUp;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class SmileFeaturesJsonRoot {
    // Root element of json-script containing HashSets/Lists of POJOs.

    private HashSet<FollowUpMailInformation> allFollowUpMails;
    private List<FollowUp> allFollowUps;

    public SmileFeaturesJsonRoot() {
        this.allFollowUpMails = new HashSet<FollowUpMailInformation>();
        this.allFollowUps = new ArrayList<FollowUp>();
    }

    public HashSet<FollowUpMailInformation> getAllFollowUpMails() {
        return allFollowUpMails;
    }

    public List<FollowUp> getAllFollowUps() {return allFollowUps; }

    public void addFollowUpMailInformation(FollowUpMailInformation newFollowUpMail) {
        allFollowUpMails.add(newFollowUpMail);
    }

    public void setAllFollowUpMails(HashSet<FollowUpMailInformation> newFollowUpMailList) {
        this.allFollowUpMails = newFollowUpMailList;
    }

    public void setAllFollowUps(List<FollowUp> newFollowUps) {
        for(FollowUp f : newFollowUps) {
            f.setReference(null);
        }
        this.allFollowUps = newFollowUps;
    }

    public void mergeAllFollowUpMails(HashSet<FollowUpMailInformation> newFollowUpMailList) {
        this.allFollowUpMails.addAll(newFollowUpMailList);
    }
}