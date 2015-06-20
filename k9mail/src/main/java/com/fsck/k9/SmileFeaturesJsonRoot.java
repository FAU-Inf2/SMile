package com.fsck.k9;

import com.fsck.k9.mail.FollowUp;
import java.util.ArrayList;
import java.util.List;

public class SmileFeaturesJsonRoot {
    // Root element of json-script containing HashSets/Lists of POJOs.

    private List<FollowUp> allFollowUps;

    public SmileFeaturesJsonRoot() {
        this.allFollowUps = new ArrayList<FollowUp>();
    }

    public List<FollowUp> getAllFollowUps() {return allFollowUps; }

    public void setAllFollowUps(List<FollowUp> newFollowUps) {
        for(FollowUp f : newFollowUps) {
            if(f.getReference() == null)
                continue;
            try {
                f.setMessageId(f.getReference().getMessageId());
            } catch (Exception e) {}
            f.setUid(f.getReference().getUid());
            f.setReference(null);
        }
        this.allFollowUps = newFollowUps;
    }
}