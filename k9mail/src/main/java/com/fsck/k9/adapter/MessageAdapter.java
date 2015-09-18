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
import com.fsck.k9.view.MessageListItemView;
import com.fsck.k9.view.QuickContactBadge;

import java.util.List;
import java.util.Set;

import de.fau.cs.mad.smile.android.R;

import static butterknife.ButterKnife.findById;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.MessageViewHolder> {
    public static class MessageViewHolder extends RecyclerView.ViewHolder {
        MessageListItemView itemView;

        public MessageViewHolder(View itemView) {
            super(itemView);
            this.itemView = (MessageListItemView)itemView;
        }

        public MessageListItemView getItemView() {
            return itemView;
        }

        public View getChip() {
            return itemView.getChip();
        }

        public CheckBox getFlagged() {
            return itemView.getFlagged();
        }

        public QuickContactBadge getContactBadge() {
            return itemView.getContactBadge();
        }

        public TextView getThreadCount() {
            return itemView.getThreadCount();
        }

        public TextView getPreview() {
            return itemView.getPreview();
        }

        public TextView getFrom() {
            return itemView.getFrom();
        }

        public TextView getSubject() {
            return itemView.getSubject();
        }

        public TextView getDate() {
            return itemView.getDate();
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
        View view = inflater.inflate(R.layout.message_list_item, parent, false);
        MessageViewHolder holder = new MessageViewHolder(view);
        return holder;
    }

    @Override
    public void onBindViewHolder(MessageViewHolder holder, final int position) {
        final LocalMessage message = mMessages.get(position);
        holder.getItemView().setMessage(message);
        /*
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
        Spannable str = (Spannable) holder.getPreview().getText();

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
        holder.getChip().setBackgroundColor(account.getChipColor());
        holder.getDate().setText(displayDate);
        holder.getPreview().setText(message.getPreview());
        */
        //holder.getSwipeLayout().addSwipeListener(new MessageListSwipeListener(this, message));
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
                mContactsPictureLoader.loadContactPicture(counterpartyAddress, holder.getContactBadge());
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
