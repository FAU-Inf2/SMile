package com.fsck.k9.holder;

import android.view.View;
import android.widget.CheckBox;
import android.widget.QuickContactBadge;
import android.widget.TextView;

import com.daimajia.swipe.SwipeLayout;

import de.fau.cs.mad.smile.android.R;

public class MessageViewHolder implements View.OnClickListener {
    private TextView subject;
    private TextView preview;
    private TextView from;
    private TextView time;
    private TextView date;
    private View chip;
    private TextView threadCount;
    private CheckBox flagged;
    private CheckBox selected;
    private SwipeLayout swipeLayout;
    private int position = -1;
    private QuickContactBadge contactBadge;
    private float fraction;

    public MessageViewHolder(View rootView) {
        swipeLayout = (SwipeLayout) rootView;
        date = (TextView) rootView.findViewById(R.id.date);
        chip = rootView.findViewById(R.id.chip);
        contactBadge = (QuickContactBadge) rootView.findViewById(R.id.contact_badge);
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

    public TextView getSubject() {
        return subject;
    }

    public void setSubject(TextView subject) {
        this.subject = subject;
    }

    public TextView getPreview() {
        return preview;
    }

    public void setPreview(TextView preview) {
        this.preview = preview;
    }

    public TextView getFrom() {
        return from;
    }

    public void setFrom(TextView from) {
        this.from = from;
    }

    public TextView getTime() {
        return time;
    }

    public void setTime(TextView time) {
        this.time = time;
    }

    public TextView getDate() {
        return date;
    }

    public void setDate(TextView date) {
        this.date = date;
    }

    public View getChip() {
        return chip;
    }

    public void setChip(View chip) {
        this.chip = chip;
    }

    public TextView getThreadCount() {
        return threadCount;
    }

    public void setThreadCount(TextView threadCount) {
        this.threadCount = threadCount;
    }

    public CheckBox getFlagged() {
        return flagged;
    }

    public void setFlagged(CheckBox flagged) {
        this.flagged = flagged;
    }

    public CheckBox getSelected() {
        return selected;
    }

    public void setSelected(CheckBox selected) {
        this.selected = selected;
    }

    public SwipeLayout getSwipeLayout() {
        return swipeLayout;
    }

    public void setSwipeLayout(SwipeLayout swipeLayout) {
        this.swipeLayout = swipeLayout;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    public QuickContactBadge getContactBadge() {
        return contactBadge;
    }

    public void setContactBadge(QuickContactBadge contactBadge) {
        this.contactBadge = contactBadge;
    }

    public float getFraction() {
        return fraction;
    }

    public void setFraction(float fraction) {
        this.fraction = fraction;
    }
}
