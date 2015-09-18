package com.fsck.k9.view;

import android.content.Context;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import com.fsck.k9.K9;
import com.fsck.k9.adapter.MessageAdapter;
import com.fsck.k9.fragment.IMessageListPresenter;
import com.fsck.k9.fragment.MessageListHandler;
import com.fsck.k9.fragment.RecyclerItemClickListener;
import com.fsck.k9.mailstore.LocalMessage;

import java.util.ArrayList;
import java.util.List;

public class MessageListView extends RecyclerView implements IMessageListView {
    private final List<LocalMessage> messages;
    private MessageListHandler handler;
    private IMessageListPresenter presenter;

    public MessageListView(Context context, AttributeSet attrs) {
        super(context, attrs);
        messages = new ArrayList<>();
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        final MessageAdapter messageAdapter = new MessageAdapter(messages);
        final RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(getContext());
        setLayoutManager(layoutManager);
        setAdapter(messageAdapter);
        addOnItemTouchListener(
                new RecyclerItemClickListener(getContext(), new RecyclerItemClickListener.OnItemClickListener() {
                    @Override
                    public void onItemClick(View view, int position) {
                        LocalMessage message = messages.get(position);
                        Log.d(K9.LOG_TAG, message.toString());
                        if(handler != null) {
                            handler.openMessage(message.makeMessageReference());
                        }
                    }
                })
        );
    }

    public void setHandler(MessageListHandler handler) {
        this.handler = handler;
    }

    @Override
    public void setPresenter(IMessageListPresenter presenter) {
        this.presenter = presenter;
    }

    @Override
    public void showMessageList(List<LocalMessage> messageList) {
        messages.clear();
        messages.addAll(messageList);
    }
}
