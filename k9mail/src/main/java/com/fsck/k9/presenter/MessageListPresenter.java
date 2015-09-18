package com.fsck.k9.presenter;

import com.fsck.k9.Account;
import com.fsck.k9.activity.MessageReference;
import com.fsck.k9.fragment.IMessageListPresenter;
import com.fsck.k9.mailstore.LocalMessage;
import com.fsck.k9.view.MessageListView;

public class MessageListPresenter implements IMessageListPresenter {

    @Override
    public void setView(MessageListView messageListView) {

    }

    @Override
    public void setModel(LocalMessage message) {

    }

    @Override
    public void move(LocalMessage message, String destFolder) {

    }

    @Override
    public void delete(LocalMessage message) {

    }

    @Override
    public void archive(LocalMessage message) {

    }

    @Override
    public void remindMe(LocalMessage message) {

    }

    @Override
    public void reply(LocalMessage message) {

    }

    @Override
    public void replyAll(LocalMessage message) {

    }

    @Override
    public void openMessage(MessageReference messageReference) {

    }

    @Override
    public void sort(Account.SortType sortType, boolean ascending) {

    }
}
