package com.fsck.k9.view;

import android.content.Context;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.AttributeSet;

import com.fsck.k9.fragment.MessageListHandler;
import com.fsck.k9.mail.Folder;

import de.fau.cs.mad.smile.android.R;

import static butterknife.ButterKnife.findById;

public class RefreshableMessageList extends SwipeRefreshLayout {
    MessageListView messageListView;

    public RefreshableMessageList(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        messageListView = findById(this, R.id.message_list);
        setOnRefreshListener(new OnRefreshListener() {
            @Override
            public void onRefresh() {
                setRefreshing(false);
            }
        });
    }

    public void setHandler(MessageListHandler handler) {
        //messageListView.setHandler(handler);
    }

    public void loadMessages(Folder folder) {
        //messageListView.loadMessages(folder);
    }
}
