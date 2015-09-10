package com.fsck.k9.fragment;

import com.fsck.k9.activity.MessageReference;
import com.fsck.k9.mailstore.LocalMessage;

public interface MessageActions {
    void move(LocalMessage message, String destFolder);
    void delete(LocalMessage message);
    void archive(LocalMessage message);
    void remindMe(LocalMessage message);
    void reply(LocalMessage message);
    void replyAll(LocalMessage message);
    void openMessage(MessageReference messageReference);
}
