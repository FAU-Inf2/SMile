package com.fsck.k9.view;

import android.content.Context;
import android.graphics.Color;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import com.daimajia.swipe.SwipeLayout;
import com.fsck.k9.FontSizes;
import com.fsck.k9.K9;
import com.fsck.k9.mailstore.LocalMessage;

import de.fau.cs.mad.smile.android.R;

import static butterknife.ButterKnife.findById;

public class MessageListItemView extends SwipeLayout {
    private FontSizes fontSizes;
    private TextView subject;
    private TextView preview;
    private TextView from;
    private TextView time;
    private TextView date;
    private View chip;
    private TextView threadCount;
    private CheckBox flagged;
    private QuickContactBadge contactBadge;
    private int position; // TODO: remove this once cursor is no longer used

    public MessageListItemView(Context context, AttributeSet attrs) {
        super(context, attrs);
        fontSizes = K9.getFontSizes();
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        subject = findById(this, R.id.subject);
        preview = findById(this, R.id.preview);
        from = findById(this, R.id.from);
        time = findById(this, R.id.time);
        date = findById(this, R.id.date);
        chip = findById(this, R.id.chip);
        threadCount = findById(this, R.id.thread_count);
        flagged = findById(this, R.id.flagged);
        contactBadge = findById(this, R.id.contact_badge);
        configureView();
    }

    private void configureView() {
        setFontSize();
        final int previewLines = K9.messageListPreviewLines();
        preview.setLines(Math.max(previewLines, 1));
        flagged.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
            }
        });

        this.addDrag(SwipeLayout.DragEdge.Left, findViewById(R.id.pull_out));
        this.addDrag(SwipeLayout.DragEdge.Right, findViewById(R.id.delete));
        this.addRevealListener(R.id.delete, new DeleteRevealListener());
        this.addRevealListener(R.id.pull_out, new LeftToRightRevealListener());
    }

    private void setFontSize() {
        fontSizes.setViewTextSize(subject, fontSizes.getMessageListSubject());
        fontSizes.setViewTextSize(date, fontSizes.getMessageListDate());
        fontSizes.setViewTextSize(preview, fontSizes.getMessageListPreview());
        fontSizes.setViewTextSize(threadCount, fontSizes.getMessageListSubject());
    }

    public View getChip() {
        return chip;
    }

    public CheckBox getFlagged() {
        return flagged;
    }

    public QuickContactBadge getContactBadge() {
        return contactBadge;
    }

    public TextView getThreadCount() {
        return threadCount;
    }

    public TextView getPreview() {
        return preview;
    }

    public TextView getFrom() {
        return from;
    }

    public TextView getSubject() {
        return subject;
    }

    public TextView getDate() {
        return date;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    private static class DeleteRevealListener implements OnRevealListener {
        @Override
        public void onReveal(View view, DragEdge dragEdge, float v, int i) {
            ImageView trash = (ImageView) view.findViewById(R.id.trash);
            if (v > 0.25) {
                view.setBackgroundColor(Color.RED);
                trash.setVisibility(View.VISIBLE);
            } else {
                view.setBackgroundColor(view.getSolidColor());
                trash.setVisibility(View.INVISIBLE);
            }
        }
    }

    private static class LeftToRightRevealListener implements OnRevealListener {
        private boolean img_set1 = false;
        private boolean img_set2 = false;

        @Override
        public void onReveal(View view, DragEdge dragEdge, float v, int i) {
            ImageView archive = findById(view, R.id.pull_out_archive);
            ImageView remindMe = findById(view, R.id.pull_out_remind_me);

            if (v <= 0.2) {
                img_set1 = img_set2 = false;
                archive.setVisibility(View.INVISIBLE);
                remindMe.setVisibility(View.INVISIBLE);
            }

            if (v > 0.2 && !img_set1) {
                img_set1 = true;
                img_set2 = false;
                archive.setVisibility(View.INVISIBLE);
                remindMe.setVisibility(View.VISIBLE);
            }

            if (v > 0.5 && !img_set2) {
                img_set1 = false;
                img_set2 = true;
                remindMe.setVisibility(View.INVISIBLE);
                archive.setVisibility(View.VISIBLE);
            }

            final Context context = view.getContext();
            final int remindmeOrange = ContextCompat.getColor(context, R.color.remindme_orange);

            if (v <= 0.2) {
                view.setBackgroundColor(view.getSolidColor());
            } else {
                if (0.2 < v && v < 0.5) {
                    view.setBackgroundColor(remindmeOrange);
                } else {
                    view.setBackgroundColor(Color.GREEN);
                }
            }
        }
    }
}
