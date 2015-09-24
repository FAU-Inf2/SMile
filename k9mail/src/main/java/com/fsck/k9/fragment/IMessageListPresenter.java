package com.fsck.k9.fragment;

import com.fsck.k9.Account.SortType;
import com.fsck.k9.activity.MessageReference;
import com.fsck.k9.mailstore.LocalMessage;
import com.fsck.k9.view.IMessageListView;

public interface IMessageListPresenter {
    void setView(IMessageListView messageListView);
    void move(LocalMessage message, String destFolder);
    void delete(LocalMessage message);
    void archive(LocalMessage message);
    void remindMe(LocalMessage message);
    void reply(LocalMessage message);
    void replyAll(LocalMessage message);
    void sort(SortType sortType, Boolean ascending);
    void openMessage(MessageReference messageReference);
    boolean openNextMessage(MessageReference messageReference);
    boolean openPreviousMessage(MessageReference messageReference);
    boolean isFirst(MessageReference messageReference);
    boolean isLast(MessageReference messageReference);
}
