package com.fsck.k9.listener;

import android.view.View;
import android.widget.ImageView;

import com.daimajia.swipe.SimpleSwipeListener;
import com.daimajia.swipe.SwipeLayout;
import com.fsck.k9.adapter.MessageAdapter;
import com.fsck.k9.mailstore.LocalMessage;

import de.fau.cs.mad.smile.android.R;

public class MessageListSwipeListener extends SimpleSwipeListener {
    private final MessageAdapter adapter;
    private final LocalMessage message;

    public MessageListSwipeListener(MessageAdapter adapter, LocalMessage message) {
        this.adapter = adapter;
        this.message = message;
    }

    @Override
    public void onHandRelease(SwipeLayout layout, float xvel, float yvel) {
        layout.setDragDistance(0);
        ImageView archive = (ImageView) layout.findViewById(R.id.pull_out_archive);
        ImageView remindMe = (ImageView) layout.findViewById(R.id.pull_out_remind_me);
        View delete = layout.findViewById(R.id.trash);

        if (archive.isShown()) {
            adapter.archiveMessage(message);
            archive.setVisibility(View.INVISIBLE);
        }

        if (remindMe.isShown()) {
            adapter.remindMeMessage(message);
            remindMe.setVisibility(View.INVISIBLE);
        }

        if (delete.isShown()) {
            adapter.deleteMessage(message);
        }
    }
}
