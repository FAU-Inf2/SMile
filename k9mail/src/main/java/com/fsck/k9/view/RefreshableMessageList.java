package com.fsck.k9.view;

import android.content.Context;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.AttributeSet;
import android.view.MotionEvent;

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
    }

    public MessageListView getMessageListView() {
        return messageListView;
    }
}
