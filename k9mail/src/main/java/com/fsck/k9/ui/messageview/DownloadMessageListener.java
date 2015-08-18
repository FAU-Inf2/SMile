package com.fsck.k9.ui.messageview;

import com.fsck.k9.Account;
import com.fsck.k9.controller.MessagingListener;
import com.fsck.k9.mailstore.LocalMessage;

class DownloadMessageListener extends MessagingListener {
    private final MessageViewHandler handler;

    public DownloadMessageListener(final MessageViewHandler handler) {
        this.handler = handler;
    }

    @Override
    public void loadMessageForViewFinished(Account account, String folder, String uid, final LocalMessage message) {
        this.handler.loadMessageFinished(message);
    }

    @Override
    public void loadMessageForViewFailed(Account account, String folder, String uid, final Throwable t) {
        this.handler.loadMessageFailed(t);
    }
}
