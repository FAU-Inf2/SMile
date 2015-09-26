package com.fsck.k9.view;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.support.v7.widget.CardView;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.text.style.AbsoluteSizeSpan;
import android.text.style.ForegroundColorSpan;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import com.daimajia.swipe.SimpleSwipeListener;
import com.daimajia.swipe.SwipeLayout;
import com.fsck.k9.Account;
import com.fsck.k9.FontSizes;
import com.fsck.k9.K9;
import com.fsck.k9.activity.misc.ContactPictureLoader;
import com.fsck.k9.fragment.IMessageListPresenter;
import com.fsck.k9.helper.ContactPicture;
import com.fsck.k9.helper.MessageHelper;
import com.fsck.k9.mail.Address;
import com.fsck.k9.mail.Flag;
import com.fsck.k9.mail.Message;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mailstore.LocalMessage;

import java.util.Set;

import de.fau.cs.mad.smile.android.R;

import static butterknife.ButterKnife.findById;

public class MessageListItemView extends CardView {
    private FontSizes fontSizes;
    private SwipeLayout swipeLayout;
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
    private ContactPictureLoader contactsPictureLoader;
    private IMessageListPresenter presenter;

    private Drawable mAttachmentIcon;
    private Drawable mForwardedIcon;
    private Drawable mAnsweredIcon;
    private Drawable mForwardedAnsweredIcon;
    private LocalMessage message;

    public MessageListItemView(Context context, AttributeSet attrs) {
        super(context, attrs);
        fontSizes = K9.getFontSizes();
        contactsPictureLoader = ContactPicture.getContactPictureLoader(context);

        mAttachmentIcon = context.getResources().getDrawable(R.drawable.ic_email_attachment_small);
        mAnsweredIcon = context.getResources().getDrawable(R.drawable.ic_email_answered_small);
        mForwardedIcon = context.getResources().getDrawable(R.drawable.ic_email_forwarded_small);
        mForwardedAnsweredIcon = context.getResources().getDrawable(R.drawable.ic_email_forwarded_answered_small);
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
        swipeLayout = findById(this, R.id.swipe_layout);
        configureView();
    }

    private void configureView() {
        setFontSize();
        final int previewLines = K9.messageListPreviewLines();
        preview.setLines(Math.max(previewLines, 1));

        this.swipeLayout.addDrag(SwipeLayout.DragEdge.Left, findViewById(R.id.pull_out));
        this.swipeLayout.addDrag(SwipeLayout.DragEdge.Right, findViewById(R.id.delete));
        this.swipeLayout.addRevealListener(R.id.delete, new DeleteRevealListener());
        this.swipeLayout.addRevealListener(R.id.pull_out, new LeftToRightRevealListener());
        setClickable(true);
    }

    private void setFontSize() {
        fontSizes.setViewTextSize(subject, fontSizes.getMessageListSubject());
        fontSizes.setViewTextSize(date, fontSizes.getMessageListDate());
        fontSizes.setViewTextSize(preview, fontSizes.getMessageListPreview());
        fontSizes.setViewTextSize(threadCount, fontSizes.getMessageListSubject());
    }

    public void setMessage(final LocalMessage message) {
        this.message = message;
        final Account account = message.getAccount();
        final MessageHelper messageHelper = MessageHelper.getInstance(getContext());

        Address[] fromAddrs = message.getFrom();
        Address[] toAddrs = new Address[0];
        Address[] ccAddrs = new Address[0];

        try {
            ccAddrs = message.getRecipients(Message.RecipientType.CC);
            toAddrs = message.getRecipients(Message.RecipientType.TO);
        } catch (MessagingException e) {
            e.printStackTrace();
        }

        boolean fromMe = messageHelper.toMe(account, fromAddrs);
        boolean toMe = messageHelper.toMe(account, toAddrs);
        boolean ccMe = messageHelper.toMe(account, ccAddrs);

        CharSequence displayName = messageHelper.getDisplayName(account, fromAddrs, toAddrs);
        CharSequence displayDate = DateUtils.getRelativeTimeSpanString(message.getSentDate().getTime());

        Address counterpartyAddress = null;
        if (fromMe) {
            if (toAddrs.length > 0) {
                counterpartyAddress = toAddrs[0];
            } else if (ccAddrs.length > 0) {
                counterpartyAddress = ccAddrs[0];
            }
        } else if (fromAddrs.length > 0) {
            counterpartyAddress = fromAddrs[0];
        }


        String subject = message.getSubject();
        if (TextUtils.isEmpty(subject)) {
            subject = getContext().getString(R.string.general_no_subject);
        }

        Set<Flag> flags = message.getFlags();

        boolean read = flags.contains(Flag.SEEN);
        boolean flagged = flags.contains(Flag.FLAGGED);
        boolean answered = flags.contains(Flag.ANSWERED);
        boolean forwarded = flags.contains(Flag.FORWARDED);
        boolean hasAttachments = message.getAttachmentCount() > 0;
        int maybeBoldTypeface = (read) ? Typeface.NORMAL : Typeface.BOLD;

        getChip().setBackgroundColor(account.getChipColor());
        getFlagged().setChecked(flagged);

        if (counterpartyAddress != null) {
            getContactBadge().assignContactFromEmail(counterpartyAddress.getAddress(), true);
            /*
             * At least in Android 2.2 a different background + padding is used when no
             * email address is available. ListView reuses the views but QuickContactBadge
             * doesn't reset the padding, so we do it ourselves.
             */
            getContactBadge().setPadding(0, 0, 0, 0);
            contactsPictureLoader.loadContactPicture(counterpartyAddress, getContactBadge());
        } else {
            getContactBadge().assignContactUri(null);
            getContactBadge().setImageResource(R.drawable.ic_contact_picture);
        }

        // Background color
        int res;
        if (read) {
            res = R.attr.messageListReadItemBackgroundColor;
        } else {
            res = R.attr.messageListUnreadItemBackgroundColor;
        }

        TypedValue outValue = new TypedValue();
        getContext().getTheme().resolveAttribute(res, outValue, true);
        //setBackgroundColor(outValue.data);

        // Thread count
        /*if (threadCount > 1) {
            holder.getThreadCount().setText(Integer.toString(threadCount));
            holder.getThreadCount().setVisibility(View.VISIBLE);
        } else {
            holder.getThreadCount().setVisibility(View.GONE);
        }*/
        // TODO: no thread count right now
        getThreadCount().setVisibility(View.GONE);

        String sigil = recipientSigil(toMe, ccMe);
        SpannableStringBuilder messageStringBuilder = new SpannableStringBuilder(sigil);
        messageStringBuilder.append(displayName);
        if (K9.messageListPreviewLines() > 0) {
            String preview = message.getPreview();
            if (preview != null) {
                messageStringBuilder.append(" ").append(preview);
            }
        }

        getPreview().setText(messageStringBuilder, TextView.BufferType.SPANNABLE);

        Spannable str = (Spannable) getPreview().getText();

        // Create a span section for the sender, and assign the correct font size and weight
        int fontSize = fontSizes.getMessageListSender();

        AbsoluteSizeSpan span = new AbsoluteSizeSpan(fontSize, true);
        str.setSpan(span, 0, displayName.length() + sigil.length(),
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

        //TODO: make this part of the theme
        int color = (K9.getK9Theme() == K9.Theme.LIGHT) ?
                Color.rgb(105, 105, 105) :
                Color.rgb(160, 160, 160);

        // Set span (color) for preview message
        str.setSpan(new ForegroundColorSpan(color), displayName.length() + sigil.length(),
                str.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        Drawable statusHolder = null;
        if (forwarded && answered) {
            statusHolder = mForwardedAnsweredIcon;
        } else if (answered) {
            statusHolder = mAnsweredIcon;
        } else if (forwarded) {
            statusHolder = mForwardedIcon;
        }

        if (getFrom() != null) {
            getFrom().setTypeface(Typeface.create(getFrom().getTypeface(), maybeBoldTypeface));
            getFrom().setText(new SpannableStringBuilder(sigil).append(displayName));
        }

        if (getSubject() != null) {
            getSubject().setCompoundDrawablesWithIntrinsicBounds(
                    statusHolder, // left
                    null, // top
                    hasAttachments ? mAttachmentIcon : null, // right
                    null); // bottom


            getSubject().setTypeface(Typeface.create(getSubject().getTypeface(), maybeBoldTypeface));
            getSubject().setText(subject);
        }

        getDate().setText(displayDate);
        swipeLayout.addSwipeListener(new ExecuteSwipeListener());
    }

    private String recipientSigil(boolean toMe, boolean ccMe) {
        if (toMe) {
            return getContext().getString(R.string.messagelist_sent_to_me_sigil);
        } else if (ccMe) {
            return getContext().getString(R.string.messagelist_sent_cc_me_sigil);
        } else {
            return "";
        }
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

    public LocalMessage getMessage() {
        return message;
    }

    public SwipeLayout getSwipeLayout() {
        return swipeLayout;
    }

    public void setPresenter(IMessageListPresenter presenter) {
        this.presenter = presenter;
    }

    private static class DeleteRevealListener implements SwipeLayout.OnRevealListener {
        @Override
        public void onReveal(View view, SwipeLayout.DragEdge dragEdge, float v, int i) {
            ImageView trash = findById(view, R.id.trash);
            if (v > 0.25) {
                view.setBackgroundColor(Color.RED);
                trash.setVisibility(View.VISIBLE);
            } else {
                view.setBackgroundColor(view.getSolidColor());
                trash.setVisibility(View.INVISIBLE);
            }
        }
    }

    private static class LeftToRightRevealListener implements SwipeLayout.OnRevealListener {
        private boolean img_set1 = false;
        private boolean img_set2 = false;

        @Override
        public void onReveal(View view, SwipeLayout.DragEdge dragEdge, float v, int i) {
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

    private class ExecuteSwipeListener extends SimpleSwipeListener {

        @Override
        public void onHandRelease(SwipeLayout layout, float xvel, float yvel) {
            MessageListItemView itemView = (MessageListItemView) layout.getParent();

            /*if(messageActionsCallback == null) {
                throw new IllegalStateException("messageActionsCallback was not set");
            }*/

            layout.setDragDistance(0);
            ImageView archive = findById(layout, R.id.pull_out_archive);
            ImageView remindMe = findById(layout, R.id.pull_out_remind_me);
            View delete = layout.findViewById(R.id.trash);

            LocalMessage message = itemView.getMessage();
            if (archive.isShown()) {
                presenter.archive(message);
                archive.setVisibility(View.INVISIBLE);
            }

            if (remindMe.isShown()) {
                presenter.remindMe(message);
                remindMe.setVisibility(View.INVISIBLE);
            }

            if (delete.isShown()) {
                presenter.delete(message);
            }
        }
    }
}
