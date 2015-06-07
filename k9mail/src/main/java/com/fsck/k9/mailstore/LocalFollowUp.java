package com.fsck.k9.mailstore;

import com.fsck.k9.mail.FollowUp;
import com.fsck.k9.mail.Message;

import java.util.Date;

public class LocalFollowUp extends FollowUp {
    private final LocalStore localStore;

    public LocalFollowUp(LocalStore localStore) {
        this.localStore = localStore;
    }
}
