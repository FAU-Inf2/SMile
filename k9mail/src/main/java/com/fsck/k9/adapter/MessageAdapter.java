package com.fsck.k9.adapter;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.support.v7.widget.RecyclerView;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.text.style.AbsoluteSizeSpan;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import com.daimajia.swipe.SwipeLayout;
import com.fsck.k9.Account;
import com.fsck.k9.FontSizes;
import com.fsck.k9.K9;
import com.fsck.k9.activity.misc.ContactPictureLoader;
import com.fsck.k9.fragment.MessageActions;
import com.fsck.k9.helper.ContactPicture;
import com.fsck.k9.helper.MessageHelper;
import com.fsck.k9.mail.Address;
import com.fsck.k9.mail.Flag;
import com.fsck.k9.mail.Message;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mailstore.LocalMessage;
import com.fsck.k9.listener.MessageListSwipeListener;
import com.fsck.k9.view.QuickContactBadge;

import java.util.List;
import java.util.Set;

import de.fau.cs.mad.smile.android.R;

import static butterknife.ButterKnife.findById;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.MessageViewHolder> {
    public static class MessageViewHolder extends RecyclerView.ViewHolder {
        private final TextView subject;
        private final TextView preview;
        private final TextView date;
        private final View chip;
        private final TextView threadCount;
        private final CheckBox flagged;
        private final SwipeLayout swipeLayout;
        private final QuickContactBadge contactBadge;

        public MessageViewHolder(View itemView) {
            super(itemView);

            swipeLayout = (SwipeLayout) itemView;
            date = findById(itemView, R.id.date);
            chip = findById(itemView, R.id.chip);
            preview = findById(itemView, R.id.preview);
            flagged = findById(itemView, R.id.flagged_bottom_right);
            contactBadge = findById(itemView, R.id.contact_badge);
            subject = findById(itemView, R.id.subject);
            threadCount = findById(itemView, R.id.thread_count);
            itemView.findViewById(R.id.sender_compact).setVisibility(View.GONE);
            threadCount.setVisibility(View.GONE);
        }

        public final TextView getSubject() {
            return subject;
        }

        public final TextView getPreview() {
            return preview;
        }

        public final TextView getDate() {
            return date;
        }

        public final View getChip() {
            return chip;
        }

        public final TextView getThreadCount() {
            return threadCount;
        }

        public final SwipeLayout getSwipeLayout() {
            return swipeLayout;
        }

        public final CheckBox getFlagged() {
            return flagged;
        }

        public final QuickContactBadge getContactBadge() {
            return contactBadge;
        }
    }

    private final List<LocalMessage> mMessages;
    private final Context mContext;
    private final ContactPictureLoader mContactsPictureLoader;
    private final MessageHelper mMessageHelper;
    private final Drawable mAttachmentIcon;
    private final Drawable mForwardedIcon;
    private final Drawable mAnsweredIcon;
    private final Drawable mForwardedAnsweredIcon;
    private final MessageActions mMessageActionsCallback;

    public MessageAdapter(final Context context, final List<LocalMessage> messages, final MessageActions messageActionsCallback) {
        this.mContext = context;
        this.mMessages = messages;
        this.mContactsPictureLoader = ContactPicture.getContactPictureLoader(this.mContext);
        this.mMessageHelper = MessageHelper.getInstance(this.mContext);

        mAttachmentIcon = context.getResources().getDrawable(R.drawable.ic_email_attachment_small);
        mAnsweredIcon = context.getResources().getDrawable(R.drawable.ic_email_answered_small);
        mForwardedIcon = context.getResources().getDrawable(R.drawable.ic_email_forwarded_small);
        mForwardedAnsweredIcon = context.getResources().getDrawable(R.drawable.ic_email_forwarded_answered_small);
        mMessageActionsCallback = messageActionsCallback;
    }

    @Override
    public MessageViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View view = inflater.inflate(R.layout.messages_item, parent, false);

        MessageViewHolder holder = new MessageViewHolder(view);

        setupFontSize(holder);
        setupSwipe(view, holder);

        return holder;
    }

    private final void setupFontSize(final MessageViewHolder holder) {
        final FontSizes fontSizes = K9.getFontSizes();
        fontSizes.setViewTextSize(holder.getPreview(), fontSizes.getMessageListPreview());
        fontSizes.setViewTextSize(holder.getSubject(), fontSizes.getMessageListSubject());
        fontSizes.setViewTextSize(holder.getDate(), fontSizes.getMessageListDate());
        holder.getPreview().setLines(Math.max(K9.messageListPreviewLines(), 1));
    }

    private final void setupSwipe(final View view, final MessageViewHolder holder) {
        final SwipeLayout swipeLayout = holder.getSwipeLayout();
        swipeLayout.addDrag(SwipeLayout.DragEdge.Left, view.findViewById(R.id.pull_out));
        swipeLayout.addDrag(SwipeLayout.DragEdge.Right, view.findViewById(R.id.delete));
        swipeLayout.addRevealListener(R.id.delete, new DeleteMessageRevealListener(swipeLayout));
        swipeLayout.addRevealListener(R.id.pull_out, new ArchiveOrRemindMeRevealListener(swipeLayout));
    }

    @Override
    public void onBindViewHolder(MessageViewHolder holder, final int position) {
        final LocalMessage message = mMessages.get(position);
        Account account = message.getAccount();

        Address[] fromAddrs = message.getFrom();
        Address[] toAddrs = new Address[0];
        Address[] ccAddrs = new Address[0];

        try {
            ccAddrs = message.getRecipients(Message.RecipientType.CC);
            toAddrs = message.getRecipients(Message.RecipientType.TO);
        } catch (MessagingException e) {
            e.printStackTrace();
        }

        boolean fromMe = mMessageHelper.toMe(account, fromAddrs);
        boolean toMe = mMessageHelper.toMe(account, toAddrs);
        boolean ccMe = mMessageHelper.toMe(account, ccAddrs);

        CharSequence displayName = mMessageHelper.getDisplayName(account, fromAddrs, toAddrs);
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

        Set<Flag> flags = message.getFlags();

        boolean read = flags.contains(Flag.SEEN);
        boolean flagged = flags.contains(Flag.FLAGGED);
        boolean answered = flags.contains(Flag.ANSWERED);
        boolean forwarded = flags.contains(Flag.FORWARDED);
        boolean hasAttachments = message.getAttachmentCount() > 0;
        int maybeBoldTypeface = (read) ? Typeface.NORMAL : Typeface.BOLD;
        long uniqueId = message.getId();

        setupContactBadge(holder, counterpartyAddress);

        String subject = message.getSubject();

        if (TextUtils.isEmpty(subject)) {
            subject = mContext.getString(R.string.general_no_subject);
        }

        CharSequence beforePreviewText = subject;
        String sigil = recipientSigil(toMe, ccMe);

        SpannableStringBuilder messageStringBuilder = new SpannableStringBuilder(sigil)
                .append(beforePreviewText);

        if (holder.getPreview().getMaxLines() > 0) {
            String preview = message.getPreview();
            if (preview != null) {
                messageStringBuilder.append(" ").append(preview);
            }
        }

        holder.getPreview().setText(messageStringBuilder, TextView.BufferType.SPANNABLE);
        Spannable str = (Spannable) holder.preview.getText();

        // Create a span section for the sender, and assign the correct font size and weight
        int fontSize = K9.getFontSizes().getMessageListSubject();

        AbsoluteSizeSpan span = new AbsoluteSizeSpan(fontSize, true);
        str.setSpan(span, 0, beforePreviewText.length() + sigil.length(),
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

        int color = (K9.getK9Theme() == K9.Theme.LIGHT) ?
                Color.rgb(105, 105, 105) :
                Color.rgb(160, 160, 160);

        Drawable statusHolder = getStatusHolder(answered, forwarded);

        // Set span (color) for preview message
        str.setSpan(new ForegroundColorSpan(color), beforePreviewText.length() + sigil.length(),
                str.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        holder.getSubject().setTypeface(Typeface.create(holder.getSubject().getTypeface(), maybeBoldTypeface));
        holder.getSubject().setCompoundDrawablesWithIntrinsicBounds(
                statusHolder, // left
                null, // top
                hasAttachments ? mAttachmentIcon : null, // right
                null); // bottom

        holder.getSubject().setText(displayName);
        //holder.getSubject().setText(subject);
        holder.getChip().setBackgroundColor(account.getChipColor());
        holder.getDate().setText(displayDate);
        holder.getPreview().setText(message.getPreview());
        //holder.getPreview().setVisibility(View.GONE);

        holder.getSwipeLayout().addSwipeListener(new MessageListSwipeListener(this, message));
    }

    private Drawable getStatusHolder(boolean answered, boolean forwarded) {
        Drawable statusHolder = null;

        if (forwarded && answered) {
            statusHolder = mForwardedAnsweredIcon;
        } else if (answered) {
            statusHolder = mAnsweredIcon;
        } else if (forwarded) {
            statusHolder = mForwardedIcon;
        }

        return statusHolder;
    }

    private final String recipientSigil(final boolean toMe, final boolean ccMe) {
        if (toMe) {
            return mContext.getString(R.string.messagelist_sent_to_me_sigil);
        } else if (ccMe) {
            return mContext.getString(R.string.messagelist_sent_cc_me_sigil);
        } else {
            return "";
        }
    }

    private final void setupContactBadge(final MessageViewHolder holder, final Address counterpartyAddress) {
        if (holder.getContactBadge() != null) {
            if (counterpartyAddress != null) {
                holder.getContactBadge().assignContactFromEmail(counterpartyAddress.getAddress(), true);
                    /*
                     * At least in Android 2.2 a different background + padding is used when no
                     * email address is available. ListView reuses the views but QuickContactBadge
                     * doesn't reset the padding, so we do it ourselves.
                     */
                holder.getContactBadge().setPadding(0, 0, 0, 0);
                mContactsPictureLoader.loadContactPicture(counterpartyAddress, holder.contactBadge);
            } else {
                holder.getContactBadge().assignContactUri(null);
                holder.getContactBadge().setImageResource(R.drawable.ic_contact_picture);
            }
        }
    }

    @Override
    public int getItemCount() {
        return mMessages.size();
    }

    private static class DeleteMessageRevealListener implements SwipeLayout.OnRevealListener {
        private final SwipeLayout swipeLayout;

        public DeleteMessageRevealListener(SwipeLayout swipeLayout) {
            this.swipeLayout = swipeLayout;
        }

        @Override
        public void onReveal(View view, SwipeLayout.DragEdge dragEdge, float v, int i) {
            ImageView trash = (ImageView) swipeLayout.findViewById(R.id.trash);
            if (v > 0.25) {
                view.setBackgroundColor(Color.RED);
                trash.setVisibility(View.VISIBLE);
            } else {
                view.setBackgroundColor(swipeLayout.getSolidColor());
                trash.setVisibility(View.INVISIBLE);
            }
        }
    }

    private static class ArchiveOrRemindMeRevealListener implements SwipeLayout.OnRevealListener {
        private final SwipeLayout swipeLayout;
        private boolean img_set1;
        private boolean img_set2;

        public ArchiveOrRemindMeRevealListener(SwipeLayout swipeLayout) {
            this.swipeLayout = swipeLayout;
            img_set1 = false;
            img_set2 = false;
        }

        @Override
        public void onReveal(View view, SwipeLayout.DragEdge dragEdge, float v, int i) {
            if (dragEdge != SwipeLayout.DragEdge.Left) {
                return;
            }

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

            if (v <= 0.2) {
                view.setBackgroundColor(Color.LTGRAY);
            } else {
                if (0.2 < v && v < 0.5) {
                    view.setBackgroundColor(Color.YELLOW);
                } else {
                    view.setBackgroundColor(Color.GREEN);
                }
            }
        }
    }

    public void archiveMessage(LocalMessage message) {
        mMessages.remove(message);
        mMessageActionsCallback.archive(message);
    }

    public void remindMeMessage(LocalMessage message) {
        mMessages.remove(message);
        mMessageActionsCallback.remindMe(message);
    }

    public void deleteMessage(LocalMessage message) {
        mMessages.remove(message);
        mMessageActionsCallback.delete(message);
    }
}
