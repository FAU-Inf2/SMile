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

public class MessageListView extends RecyclerView {
    public MessageListView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        final RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(getContext());
        setLayoutManager(layoutManager);
    }
}
