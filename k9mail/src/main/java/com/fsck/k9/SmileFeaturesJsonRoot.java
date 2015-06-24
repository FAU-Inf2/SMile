package com.fsck.k9;

import com.fsck.k9.mail.RemindMe;

import java.util.ArrayList;
import java.util.List;

public class SmileFeaturesJsonRoot {
    // Root element of json-script containing HashSets/Lists of POJOs.

    private List<RemindMe> allRemindMes;

    public SmileFeaturesJsonRoot() {
        this.allRemindMes = new ArrayList<RemindMe>();
    }

    public List<RemindMe> getAllRemindMes() {return allRemindMes; }

    public void setAllRemindMes(List<RemindMe> newRemindMes) {
        for(RemindMe f : newRemindMes) {
            if(f.getReference() == null)
                continue;
            try {
                f.setMessageId(f.getReference().getMessageId());
            } catch (Exception e) {}
            f.setUid(f.getReference().getUid());
            f.setReference(null);
        }
        this.allRemindMes = newRemindMes;
    }
}