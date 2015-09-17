package com.fsck.k9.adapter;

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
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.QuickContactBadge;
import android.widget.TextView;

import com.daimajia.swipe.SimpleSwipeListener;
import com.daimajia.swipe.SwipeLayout;
import com.fsck.k9.Account;
import com.fsck.k9.FontSizes;
import com.fsck.k9.K9;
import com.fsck.k9.Preferences;
import com.fsck.k9.activity.misc.ContactPictureLoader;
import com.fsck.k9.fragment.MessageActions;
import com.fsck.k9.fragment.MessageListFragment;
import com.fsck.k9.helper.ContactPicture;
import com.fsck.k9.helper.MessageHelper;
import com.fsck.k9.helper.Utility;
import com.fsck.k9.mail.Address;
import com.fsck.k9.holder.MessageViewHolder;
import com.fsck.k9.mail.Folder;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mailstore.LocalFolder;
import com.fsck.k9.mailstore.LocalMessage;
import com.fsck.k9.mailstore.LocalStore;

import de.fau.cs.mad.smile.android.R;

public class MessageListAdapter extends CursorAdapter {
    private final Context context;
    private final boolean mCheckboxes;
    private final boolean mStars;
    private final boolean mThreadedList;
    private final MessageHelper mMessageHelper;
    private final int mPreviewLines;
    private final FontSizes mFontSizes;
    private final boolean mSenderAboveSubject;
    private final MessageActions messageActionsCallback;
    private ContactPictureLoader mContactsPictureLoader;
    private Drawable mAttachmentIcon;
    private Drawable mForwardedIcon;
    private Drawable mAnsweredIcon;
    private Drawable mForwardedAnsweredIcon;


    public MessageListAdapter(Context context, MessageActions messageActionsCallback, boolean threadedList) {
        super(context, null, 0);

        if (K9.showContactPicture()) {
            mContactsPictureLoader = ContactPicture.getContactPictureLoader(context);
        }

        this.messageActionsCallback = messageActionsCallback;
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

        final MessageViewHolder holder = new MessageViewHolder(view);
        final boolean senderAboveSubject = K9.messageListSenderAboveSubject();

        if (mPreviewLines == 0 && mContactsPictureLoader == null) {
            view.findViewById(R.id.preview).setVisibility(View.GONE);
            holder.setPreview((TextView) view.findViewById(R.id.sender_compact));
            holder.setFlagged((CheckBox) view.findViewById(R.id.flagged_center_right));
            view.findViewById(R.id.flagged_bottom_right).setVisibility(View.GONE);
        } else {
            view.findViewById(R.id.sender_compact).setVisibility(View.GONE);
            holder.setPreview((TextView) view.findViewById(R.id.preview));
            holder.setFlagged((CheckBox) view.findViewById(R.id.flagged_bottom_right));
        }

        if (mContactsPictureLoader == null) {
            holder.getContactBadge().setVisibility(View.GONE);
        }

        if (senderAboveSubject) {
            holder.setFrom((TextView) view.findViewById(R.id.subject));
            mFontSizes.setViewTextSize(holder.getFrom(), mFontSizes.getMessageListSender());
        } else {
            holder.setSubject((TextView) view.findViewById(R.id.subject));
            mFontSizes.setViewTextSize(holder.getSubject(), mFontSizes.getMessageListSubject());
        }

        mFontSizes.setViewTextSize(holder.getDate(), mFontSizes.getMessageListDate());

        // 1 preview line is needed even if it is set to 0, because subject is part of the same text view
        holder.getPreview().setLines(Math.max(mPreviewLines, 1));
        mFontSizes.setViewTextSize(holder.getPreview(), mFontSizes.getMessageListPreview());
        holder.setThreadCount((TextView) view.findViewById(R.id.thread_count));
        mFontSizes.setViewTextSize(holder.getThreadCount(), mFontSizes.getMessageListSubject()); // thread count is next to subject

        holder.getFlagged().setVisibility(mStars ? View.VISIBLE : View.GONE);
        holder.getFlagged().setOnClickListener(holder);

        view.setTag(holder);

        final SwipeLayout swipeLayout = holder.getSwipeLayout();
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

        final MessageViewHolder holder = (MessageViewHolder) view.getTag();

        int maybeBoldTypeface = (read) ? Typeface.NORMAL : Typeface.BOLD;

        //long uniqueId = cursor.getLong(mUniqueIdColumn);
        //boolean selected = mSelected.contains(uniqueId);

        holder.getChip().setBackgroundColor(account.getChipColor());

        if (mCheckboxes) {
            //holder.selected.setChecked(selected);
        }

        if (mStars) {
            holder.getFlagged().setChecked(flagged);
        }

        holder.setPosition(cursor.getPosition());

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
            holder.getThreadCount().setText(Integer.toString(threadCount));
            holder.getThreadCount().setVisibility(View.VISIBLE);
        } else {
            holder.getThreadCount().setVisibility(View.GONE);
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

        holder.getPreview().setText(messageStringBuilder, TextView.BufferType.SPANNABLE);

        Spannable str = (Spannable) holder.getPreview().getText();

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

        if (holder.getFrom() != null) {
            holder.getFrom().setTypeface(Typeface.create(holder.getFrom().getTypeface(), maybeBoldTypeface));
            if (mSenderAboveSubject) {
                holder.getFrom().setCompoundDrawablesWithIntrinsicBounds(
                        statusHolder, // left
                        null, // top
                        hasAttachments ? mAttachmentIcon : null, // right
                        null); // bottom

                holder.getFrom().setText(displayName);
            } else {
                holder.getFrom().setText(new SpannableStringBuilder(sigil).append(displayName));
            }
        }

        if (holder.getSubject() != null) {
            if (!mSenderAboveSubject) {
                holder.getSubject().setCompoundDrawablesWithIntrinsicBounds(
                        statusHolder, // left
                        null, // top
                        hasAttachments ? mAttachmentIcon : null, // right
                        null); // bottom
            }

            holder.getSubject().setTypeface(Typeface.create(holder.getSubject().getTypeface(), maybeBoldTypeface));
            holder.getSubject().setText(subject);
        }

        holder.getDate().setText(displayDate);
        holder.getSwipeLayout().addSwipeListener(new SimpleSwipeListener() {
            @Override
            public void onHandRelease(SwipeLayout layout, float xvel, float yvel) {layout.setDragDistance(0);
                ImageView archive = (ImageView) layout.findViewById(R.id.pull_out_archive);
                ImageView remindMe = (ImageView) layout.findViewById(R.id.pull_out_remind_me);
                View delete = layout.findViewById(R.id.trash);

                LocalMessage message = getMessageAtPosition(holder.getPosition());
                if (archive.isShown()) {
                    messageActionsCallback.archive(message);
                    archive.setVisibility(View.INVISIBLE);
                }

                if (remindMe.isShown()) {
                    messageActionsCallback.remindMe(message);
                    remindMe.setVisibility(View.INVISIBLE);
                }

                if (delete.isShown()) {
                    messageActionsCallback.delete(message);
                }
            }
        });
    }

    protected Account getAccountFromCursor(Cursor cursor) {
        String accountUuid = cursor.getString(MessageListFragment.ACCOUNT_UUID_COLUMN);
        return Preferences.getPreferences(context).getAccount(accountUuid);
    }

    private LocalMessage getMessageAtPosition(int adapterPosition) {
        if (adapterPosition == AdapterView.INVALID_POSITION) {
            return null;
        }

        Cursor cursor = (Cursor) getItem(adapterPosition);
        String uid = cursor.getString(MessageListFragment.UID_COLUMN);

        Account account = getAccountFromCursor(cursor);
        long folderId = cursor.getLong(MessageListFragment.FOLDER_ID_COLUMN);
        LocalFolder folder = getFolderById(account, folderId);

        try {
            return folder.getMessage(uid);
        } catch (MessagingException e) {
            throw new RuntimeException(e);
        }
    }

    private LocalFolder getFolderById(Account account, long folderId) {
        try {
            LocalStore localStore = account.getLocalStore();
            LocalFolder localFolder = localStore.getFolderById(folderId);
            localFolder.open(Folder.OPEN_MODE_RO);
            return localFolder;
        } catch (MessagingException e) {
            throw new RuntimeException(e);
        }
    }
}
