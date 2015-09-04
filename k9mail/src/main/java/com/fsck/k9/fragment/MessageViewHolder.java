package com.fsck.k9.fragment;

import android.view.View;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.QuickContactBadge;
import android.widget.TextView;

import com.daimajia.swipe.SimpleSwipeListener;
import com.daimajia.swipe.SwipeLayout;

import de.fau.cs.mad.smile.android.R;

class MessageViewHolder extends SimpleSwipeListener implements View.OnClickListener {
    public TextView subject;
    public TextView preview;
    public TextView from;
    public TextView time;
    public TextView date;
    public View chip;
    public TextView threadCount;
    public CheckBox flagged;
    public CheckBox selected;
    public SwipeLayout swipeLayout;
    public int position = -1;
    public QuickContactBadge contactBadge;
    public float fraction;
    private final MessageListHandler handler;

    public MessageViewHolder() {
        this.handler = null;
    }

    @Override
    public void onClick(View view) {
        if (position != -1) {

            switch (view.getId()) {
                case R.id.selected_checkbox:
                    //handler.toggleMessageSelectWithAdapterPosition(position);
                    break;
                /*case R.id.delete:
                    onDelete(getMessageAtPosition(position));
                    break;*/
                case R.id.flagged_bottom_right:
                case R.id.flagged_center_right:
                    //handler.toggleMessageFlagWithAdapterPosition(position);
                    break;
            }
        }
    }

    @Override
    public void onHandRelease(SwipeLayout layout, float xvel, float yvel) {
        if (position == -1) {
            return;
        }
        layout.setDragDistance(0);
        ImageView archive = (ImageView) layout.findViewById(R.id.pull_out_archive);
        ImageView remindMe = (ImageView) layout.findViewById(R.id.pull_out_remind_me);
        View delete = layout.findViewById(R.id.trash);
        if (archive.isShown()) {
            //handler.onArchive(getMessageAtPosition(position));
            archive.setVisibility(View.INVISIBLE);
        }
        if (remindMe.isShown()) {
            //handler.onRemindMe(getMessageAtPosition(position));
            remindMe.setVisibility(View.INVISIBLE);
        }
        if (delete.isShown()) {
            //handler.onDelete(getMessageAtPosition(position));
        }
    }
}
