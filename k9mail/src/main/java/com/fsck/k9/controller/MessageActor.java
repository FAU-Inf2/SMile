package com.fsck.k9.controller;

import com.fsck.k9.Account;
import com.fsck.k9.mail.Folder;
import com.fsck.k9.mail.Message;

import java.util.List;

public interface MessageActor {
    void act(final Account account, final Folder folder, final List<Message> messages);
}
