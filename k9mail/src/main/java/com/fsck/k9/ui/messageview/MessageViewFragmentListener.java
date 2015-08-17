package com.fsck.k9.ui.messageview;

import com.fsck.k9.crypto.PgpData;
import com.fsck.k9.mailstore.LocalMessage;
import com.fsck.k9.view.MessageHeader;

public interface MessageViewFragmentListener {
    public void onForward(LocalMessage mMessage, PgpData mPgpData);

    public void disableDeleteAction();

    public void onReplyAll(LocalMessage mMessage, PgpData mPgpData);

    public void onReply(LocalMessage mMessage, PgpData mPgpData);

    public void displayMessageSubject(String title);

    public void setProgress(boolean b);

    public void showNextMessageOrReturn();

    public void messageHeaderViewAvailable(MessageHeader messageHeaderView);

    public void updateMenu();
}
