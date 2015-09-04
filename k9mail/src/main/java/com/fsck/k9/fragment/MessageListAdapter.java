package com.fsck.k9.fragment;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.support.v4.widget.CursorAdapter;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.text.style.AbsoluteSizeSpan;
import android.text.style.ForegroundColorSpan;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.QuickContactBadge;
import android.widget.TextView;

import com.daimajia.swipe.SwipeLayout;
import com.fsck.k9.Account;
import com.fsck.k9.FontSizes;
import com.fsck.k9.K9;
import com.fsck.k9.Preferences;
import com.fsck.k9.activity.misc.ContactPictureLoader;
import com.fsck.k9.helper.ContactPicture;
import com.fsck.k9.helper.MessageHelper;
import com.fsck.k9.helper.Utility;
import com.fsck.k9.mail.Address;

import de.fau.cs.mad.smile.android.R;

class MessageListAdapter extends CursorAdapter {
    private final Context context;
    private final boolean mCheckboxes;
    private final boolean mStars;
    private final boolean mThreadedList;
    private final MessageHelper mMessageHelper;
    private final int mPreviewLines;
    private final FontSizes mFontSizes;
    private final boolean mSenderAboveSubject;
    private ContactPictureLoader mContactsPictureLoader;
    private Drawable mAttachmentIcon;
    private Drawable mForwardedIcon;
    private Drawable mAnsweredIcon;
    private Drawable mForwardedAnsweredIcon;

    MessageListAdapter(Context context, boolean threadedList) {
        super(context, null, 0);

        if (K9.showContactPicture()) {
            mContactsPictureLoader = ContactPicture.getContactPictureLoader(context);
        }

        this.context = context;
        mThreadedList = threadedList;
        mCheckboxes = K9.messageListCheckboxes();
        mStars = K9.messageListStars();
        mMessageHelper = MessageHelper.getInstance(context);
        mPreviewLines = K9.messageListPreviewLines();
        mFontSizes = K9.getFontSizes();
        mSenderAboveSubject = K9.messageListSenderAboveSubject();

        mAttachmentIcon = context.getResources().getDrawable(R.drawable.ic_email_attachment_small);
        mAnsweredIcon = context.getResources().getDrawable(R.drawable.ic_email_answered_small);
        mForwardedIcon = context.getResources().getDrawable(R.drawable.ic_email_forwarded_small);
        mForwardedAnsweredIcon = context.getResources().getDrawable(R.drawable.ic_email_forwarded_answered_small);
    }

    private String recipientSigil(boolean toMe, boolean ccMe) {
        if (toMe) {
            return context.getString(R.string.messagelist_sent_to_me_sigil);
        } else if (ccMe) {
            return context.getString(R.string.messagelist_sent_cc_me_sigil);
        } else {
            return "";
        }
    }

    @Override
    public View newView(final Context context, Cursor cursor, ViewGroup parent) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.message_list_item, parent, false);
        view.setId(R.layout.message_list_item);
        final boolean senderAboveSubject = K9.messageListSenderAboveSubject();

        final MessageViewHolder holder = new MessageViewHolder();
        holder.date = (TextView) view.findViewById(R.id.date);
        holder.chip = view.findViewById(R.id.chip);


        if (mPreviewLines == 0 && mContactsPictureLoader == null) {
            view.findViewById(R.id.preview).setVisibility(View.GONE);
            holder.preview = (TextView) view.findViewById(R.id.sender_compact);
            holder.flagged = (CheckBox) view.findViewById(R.id.flagged_center_right);
            view.findViewById(R.id.flagged_bottom_right).setVisibility(View.GONE);
        } else {
            view.findViewById(R.id.sender_compact).setVisibility(View.GONE);
            holder.preview = (TextView) view.findViewById(R.id.preview);
            holder.flagged = (CheckBox) view.findViewById(R.id.flagged_bottom_right);
            view.findViewById(R.id.flagged_center_right).setVisibility(View.GONE);
        }

        QuickContactBadge contactBadge =
                (QuickContactBadge) view.findViewById(R.id.contact_badge);

        if (mContactsPictureLoader != null) {
            holder.contactBadge = contactBadge;
        } else {
            contactBadge.setVisibility(View.GONE);
        }

        if (senderAboveSubject) {
            holder.from = (TextView) view.findViewById(R.id.subject);
            mFontSizes.setViewTextSize(holder.from, mFontSizes.getMessageListSender());
        } else {
            holder.subject = (TextView) view.findViewById(R.id.subject);
            mFontSizes.setViewTextSize(holder.subject, mFontSizes.getMessageListSubject());
        }

        mFontSizes.setViewTextSize(holder.date, mFontSizes.getMessageListDate());


        // 1 preview line is needed even if it is set to 0, because subject is part of the same text view
        holder.preview.setLines(Math.max(mPreviewLines, 1));
        mFontSizes.setViewTextSize(holder.preview, mFontSizes.getMessageListPreview());
        holder.threadCount = (TextView) view.findViewById(R.id.thread_count);
        mFontSizes.setViewTextSize(holder.threadCount, mFontSizes.getMessageListSubject()); // thread count is next to subject
        view.findViewById(R.id.selected_checkbox_wrapper).setVisibility((mCheckboxes) ? View.VISIBLE : View.GONE);

        holder.flagged.setVisibility(mStars ? View.VISIBLE : View.GONE);
        holder.flagged.setOnClickListener(holder);

        holder.selected = (CheckBox) view.findViewById(R.id.selected_checkbox);
        holder.selected.setOnClickListener(holder);

        view.setTag(holder);

        final SwipeLayout swipeLayout = (SwipeLayout) view;
        swipeLayout.addDrag(SwipeLayout.DragEdge.Left, view.findViewById(R.id.pull_out));
        swipeLayout.addDrag(SwipeLayout.DragEdge.Right, view.findViewById(R.id.delete));

        swipeLayout.addRevealListener(R.id.delete, new SwipeLayout.OnRevealListener() {
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
        });

        swipeLayout.addRevealListener(R.id.pull_out, new SwipeLayout.OnRevealListener() {
            private boolean img_set1 = false;
            private boolean img_set2 = false;

            @Override
            public void onReveal(View view, SwipeLayout.DragEdge dragEdge, float v, int i) {
                ImageView archive = (ImageView) swipeLayout.findViewById(R.id.pull_out_archive);
                ImageView remindMe = (ImageView) swipeLayout.findViewById(R.id.pull_out_remind_me);
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
                    view.setBackgroundColor(swipeLayout.getSolidColor());
                } else {
                    if (0.2 < v && v < 0.5) {
                        view.setBackgroundColor(Color.YELLOW);
                    } else {
                        view.setBackgroundColor(Color.GREEN);
                    }
                }
            }
        });
        swipeLayout.addSwipeListener(holder);
        return view;
    }


    @Override
    public void bindView(View view, final Context context, Cursor cursor) {
        Account account = getAccountFromCursor(cursor);

        String fromList = cursor.getString(MessageListFragment.SENDER_LIST_COLUMN);
        String toList = cursor.getString(MessageListFragment.TO_LIST_COLUMN);
        String ccList = cursor.getString(MessageListFragment.CC_LIST_COLUMN);
        Address[] fromAddrs = Address.unpack(fromList);
        Address[] toAddrs = Address.unpack(toList);
        Address[] ccAddrs = Address.unpack(ccList);

        boolean fromMe = mMessageHelper.toMe(account, fromAddrs);
        boolean toMe = mMessageHelper.toMe(account, toAddrs);
        boolean ccMe = mMessageHelper.toMe(account, ccAddrs);

        CharSequence displayName = mMessageHelper.getDisplayName(account, fromAddrs, toAddrs);
        CharSequence displayDate = DateUtils.getRelativeTimeSpanString(context, cursor.getLong(MessageListFragment.DATE_COLUMN));

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

        int threadCount = (mThreadedList) ? cursor.getInt(MessageListFragment.THREAD_COUNT_COLUMN) : 0;

        String subject = cursor.getString(MessageListFragment.SUBJECT_COLUMN);
        if (TextUtils.isEmpty(subject)) {
            subject = context.getString(R.string.general_no_subject);
        } else if (threadCount > 1) {
            // If this is a thread, strip the RE/FW from the subject.  "Be like Outlook."
            subject = Utility.stripSubject(subject);
        }

        boolean read = (cursor.getInt(MessageListFragment.READ_COLUMN) == 1);
        boolean flagged = (cursor.getInt(MessageListFragment.FLAGGED_COLUMN) == 1);
        boolean answered = (cursor.getInt(MessageListFragment.ANSWERED_COLUMN) == 1);
        boolean forwarded = (cursor.getInt(MessageListFragment.FORWARDED_COLUMN) == 1);

        boolean hasAttachments = (cursor.getInt(MessageListFragment.ATTACHMENT_COUNT_COLUMN) > 0);

        MessageViewHolder holder = (MessageViewHolder) view.getTag();

        int maybeBoldTypeface = (read) ? Typeface.NORMAL : Typeface.BOLD;

        //long uniqueId = cursor.getLong(mUniqueIdColumn);
        //boolean selected = mSelected.contains(uniqueId);


        holder.chip.setBackgroundColor(account.getChipColor());

        if (mCheckboxes) {
            //holder.selected.setChecked(selected);
        }

        if (mStars) {
            holder.flagged.setChecked(flagged);
        }

        holder.position = cursor.getPosition();

        if (holder.contactBadge != null) {
            if (counterpartyAddress != null) {
                holder.contactBadge.assignContactFromEmail(counterpartyAddress.getAddress(), true);
                /*
                 * At least in Android 2.2 a different background + padding is used when no
                 * email address is available. ListView reuses the views but QuickContactBadge
                 * doesn't reset the padding, so we do it ourselves.
                 */
                holder.contactBadge.setPadding(0, 0, 0, 0);
                mContactsPictureLoader.loadContactPicture(counterpartyAddress, holder.contactBadge);
            } else {
                holder.contactBadge.assignContactUri(null);
                holder.contactBadge.setImageResource(R.drawable.ic_contact_picture);
            }
        }

        // Background color
        //if (selected || K9.useBackgroundAsUnreadIndicator()) {
        if(K9.useBackgroundAsUnreadIndicator()) {
            int res;
            /*if (selected) {
                res = R.attr.messageListSelectedBackgroundColor;
            } else*/ if (read) {
                res = R.attr.messageListReadItemBackgroundColor;
            } else {
                res = R.attr.messageListUnreadItemBackgroundColor;
            }

            TypedValue outValue = new TypedValue();
            context.getTheme().resolveAttribute(res, outValue, true);
            view.setBackgroundColor(outValue.data);
        } else {
            view.setBackgroundColor(Color.TRANSPARENT);
        }

        /*
        if (mActiveMessage != null) {
            String uid = cursor.getString(UID_COLUMN);
            String folderName = cursor.getString(FOLDER_NAME_COLUMN);

            if (account.getUuid().equals(mActiveMessage.getAccountUuid()) &&
                    folderName.equals(mActiveMessage.getFolderName()) &&
                    uid.equals(mActiveMessage.getUid())) {
                int res = R.attr.messageListActiveItemBackgroundColor;

                TypedValue outValue = new TypedValue();
                context.getTheme().resolveAttribute(res, outValue, true);
                view.setBackgroundColor(outValue.data);
            }
        }*/

        // Thread count
        if (threadCount > 1) {
            holder.threadCount.setText(Integer.toString(threadCount));
            holder.threadCount.setVisibility(View.VISIBLE);
        } else {
            holder.threadCount.setVisibility(View.GONE);
        }

        CharSequence beforePreviewText = (mSenderAboveSubject) ? subject : displayName;

        String sigil = recipientSigil(toMe, ccMe);

        SpannableStringBuilder messageStringBuilder = new SpannableStringBuilder(sigil)
                .append(beforePreviewText);

        if (mPreviewLines > 0) {
            String preview = cursor.getString(MessageListFragment.PREVIEW_COLUMN);
            if (preview != null) {
                messageStringBuilder.append(" ").append(preview);
            }
        }

        holder.preview.setText(messageStringBuilder, TextView.BufferType.SPANNABLE);

        Spannable str = (Spannable) holder.preview.getText();

        // Create a span section for the sender, and assign the correct font size and weight
        int fontSize = (mSenderAboveSubject) ?
                mFontSizes.getMessageListSubject() :
                mFontSizes.getMessageListSender();

        AbsoluteSizeSpan span = new AbsoluteSizeSpan(fontSize, true);
        str.setSpan(span, 0, beforePreviewText.length() + sigil.length(),
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

        //TODO: make this part of the theme
        int color = (K9.getK9Theme() == K9.Theme.LIGHT) ?
                Color.rgb(105, 105, 105) :
                Color.rgb(160, 160, 160);

        // Set span (color) for preview message
        str.setSpan(new ForegroundColorSpan(color), beforePreviewText.length() + sigil.length(),
                str.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

        Drawable statusHolder = null;
        if (forwarded && answered) {
            statusHolder = mForwardedAnsweredIcon;
        } else if (answered) {
            statusHolder = mAnsweredIcon;
        } else if (forwarded) {
            statusHolder = mForwardedIcon;
        }

        if (holder.from != null) {
            holder.from.setTypeface(Typeface.create(holder.from.getTypeface(), maybeBoldTypeface));
            if (mSenderAboveSubject) {
                holder.from.setCompoundDrawablesWithIntrinsicBounds(
                        statusHolder, // left
                        null, // top
                        hasAttachments ? mAttachmentIcon : null, // right
                        null); // bottom

                holder.from.setText(displayName);
            } else {
                holder.from.setText(new SpannableStringBuilder(sigil).append(displayName));
            }
        }

        if (holder.subject != null) {
            if (!mSenderAboveSubject) {
                holder.subject.setCompoundDrawablesWithIntrinsicBounds(
                        statusHolder, // left
                        null, // top
                        hasAttachments ? mAttachmentIcon : null, // right
                        null); // bottom
            }

            holder.subject.setTypeface(Typeface.create(holder.subject.getTypeface(), maybeBoldTypeface));
            holder.subject.setText(subject);
        }

        holder.date.setText(displayDate);

    }

    protected Account getAccountFromCursor(Cursor cursor) {
        String accountUuid = cursor.getString(MessageListFragment.ACCOUNT_UUID_COLUMN);
        return Preferences.getPreferences(context).getAccount(accountUuid);
    }
}
