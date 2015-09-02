package com.fsck.k9.fragment;

import com.fsck.k9.Account;
import com.fsck.k9.activity.MessageReference;
import com.fsck.k9.mailstore.LocalMessage;

public interface MessageListFragmentListener {
    void enableActionBarProgress(boolean enable);

    void setMessageListProgress(int level);

    void showThread(Account account, String folderName, long rootId);

    void showSMS(Account account, String FolderName, long rootId, MessageReference messageReference);

    void showMoreFromSameSender(String senderAddress);

    void onResendMessage(LocalMessage message);

    void onForward(LocalMessage message);

    void onReply(LocalMessage message);

    void onReplyAll(LocalMessage message);

    void openMessage(MessageReference messageReference);

    void setMessageListTitle(String title);

    void setMessageListSubTitle(String subTitle);

    void setUnreadCount(int unread);

    void onCompose(Account account);

    boolean startSearch(Account account, String folderName);

    void remoteSearchStarted();

    void goBack();

    void updateMenu();
}
