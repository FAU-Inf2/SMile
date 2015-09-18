package com.fsck.k9.view;

import com.fsck.k9.fragment.IMessageListPresenter;
import com.fsck.k9.mailstore.LocalMessage;

import java.util.List;

public interface IMessageListView {
    void setPresenter(IMessageListPresenter presenter);
    void showMessageList(List<LocalMessage> messageList);
}
