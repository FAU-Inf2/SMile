package com.fsck.k9.adapter;

import android.content.Context;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.fsck.k9.Account;
import com.fsck.k9.K9;
import com.fsck.k9.listener.FolderClickListener;
import com.fsck.k9.activity.FolderListFilter;
import com.fsck.k9.holder.FolderInfoHolder;
import com.fsck.k9.holder.FolderViewHolder;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.search.LocalSearch;
import com.fsck.k9.search.SearchSpecification;

import de.fau.cs.mad.smile.android.R;

public class FolderAdapter extends ArrayAdapter<FolderInfoHolder> {
    private Filter mFilter;
    private Account mAccount;

    public FolderAdapter(Context context, Account account) {
        super(context, R.layout.folder_list_item);
        setFilter(new FolderListFilter(this));
        this.mAccount = account;
    }

    public FolderInfoHolder getFolder(String folder) {
        FolderInfoHolder search = new FolderInfoHolder();
        search.name = folder;

        for(int i = 0; i < getCount(); i++) {
            FolderInfoHolder holder = getItem(i);
            if(holder == search)
                return holder;
        }

        return null;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (position <= getCount()) {
            return getItemView(position, convertView, parent);
        } else {
            Log.e(K9.LOG_TAG, "getView with illegal positon=" + position
                    + " called! count is only " + getCount());
            return null;
        }
    }

    public View getItemView(int itemPosition, View convertView, ViewGroup parent) {
        final FolderInfoHolder folder = getItem(itemPosition);
        View view;

        if (convertView != null) {
            view = convertView;
        } else {
            LayoutInflater layoutInflater = LayoutInflater.from(getContext());
            view = layoutInflater.inflate(R.layout.folder_list_item, parent, false);
        }

        final FolderViewHolder holder = getOrCreateFolderInfoHolder(folder, view);

        if (folder == null) {
            return view;
        }

        final String folderStatus = getFolderStatus(folder);
        holder.folderName.setText(folder.displayName);

        if (folderStatus != null) {
            holder.folderStatus.setText(folderStatus);
            holder.folderStatus.setVisibility(View.VISIBLE);
        } else {
            holder.folderStatus.setVisibility(View.GONE);
        }

        if (folder.unreadMessageCount == -1) {
            folder.unreadMessageCount = 0;
            try {
                folder.unreadMessageCount = folder.folder.getUnreadMessageCount();
            } catch (MessagingException e) {
                Log.e(K9.LOG_TAG, "Unable to get unreadMessageCount for " + mAccount.getDescription() + ":"
                        + folder.name);
            }
        }

        if (folder.unreadMessageCount > 0) {
            holder.newMessageCount.setText(Integer.toString(folder.unreadMessageCount));
            holder.newMessageCountWrapper.setOnClickListener(
                    createUnreadSearch(mAccount, folder));
            holder.newMessageCountWrapper.setVisibility(View.VISIBLE);
            holder.newMessageCountIcon.setBackground(
                    mAccount.generateColorChip(false, false, false, false, false).drawable());
        } else {
            holder.newMessageCountWrapper.setVisibility(View.GONE);
        }

        if (folder.flaggedMessageCount == -1) {
            folder.flaggedMessageCount = 0;
            try {
                folder.flaggedMessageCount = folder.folder.getFlaggedMessageCount();
            } catch (MessagingException e) {
                Log.e(K9.LOG_TAG, "Unable to get flaggedMessageCount for " + mAccount.getDescription() + ":"
                        + folder.name);
            }
        }

        if (K9.messageListStars() && folder.flaggedMessageCount > 0) {
            holder.flaggedMessageCount.setText(Integer.toString(folder.flaggedMessageCount));
            holder.flaggedMessageCountWrapper.setOnClickListener(
                    createFlaggedSearch(mAccount, folder));
            holder.flaggedMessageCountWrapper.setVisibility(View.VISIBLE);
            holder.flaggedMessageCountIcon.setBackground(
                    mAccount.generateColorChip(false, false, false, false, true).drawable());
        } else {
            holder.flaggedMessageCountWrapper.setVisibility(View.GONE);
        }

        holder.activeIcons.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Toast toast = Toast.makeText(getContext(), getContext().getString(R.string.tap_hint), Toast.LENGTH_SHORT);
                toast.show();
            }
        });

        holder.chip.setBackgroundColor(mAccount.getChipColor());
        K9.getFontSizes().setViewTextSize(holder.folderName, K9.getFontSizes().getFolderName());

        if (K9.wrapFolderNames()) {
            holder.folderName.setEllipsize(null);
            holder.folderName.setSingleLine(false);
        } else {
            holder.folderName.setEllipsize(TextUtils.TruncateAt.START);
            holder.folderName.setSingleLine(true);
        }

        K9.getFontSizes().setViewTextSize(holder.folderStatus, K9.getFontSizes().getFolderStatus());

        return view;
    }

    private final FolderViewHolder getOrCreateFolderInfoHolder(final FolderInfoHolder folder, final View view) {
        FolderViewHolder holder = (FolderViewHolder) view.getTag();

        if (holder == null) {
            holder = new FolderViewHolder();
            holder.folderName = (TextView) view.findViewById(R.id.folder_name);
            holder.newMessageCount = (TextView) view.findViewById(R.id.new_message_count);
            holder.flaggedMessageCount = (TextView) view.findViewById(R.id.flagged_message_count);
            holder.newMessageCountWrapper = view.findViewById(R.id.new_message_count_wrapper);
            holder.flaggedMessageCountWrapper = view.findViewById(R.id.flagged_message_count_wrapper);
            holder.newMessageCountIcon = view.findViewById(R.id.new_message_count_icon);
            holder.flaggedMessageCountIcon = view.findViewById(R.id.flagged_message_count_icon);

            holder.folderStatus = (TextView) view.findViewById(R.id.folder_status);
            holder.activeIcons = (RelativeLayout) view.findViewById(R.id.active_icons);
            holder.chip = view.findViewById(R.id.chip);
            holder.folderListItemLayout = (LinearLayout) view.findViewById(R.id.folder_list_item_layout);
            holder.rawFolderName = folder.name;

            view.setTag(holder);
        }

        return holder;
    }

    private final String getFolderStatus(final FolderInfoHolder folder) {
        if (folder.loading) {
            return getContext().getString(R.string.status_loading);
        }

        if (folder.status != null) {
            return folder.status;
        }

        if (folder.lastChecked != 0) {
            long now = System.currentTimeMillis();
            int flags = DateUtils.FORMAT_SHOW_TIME | DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_YEAR;
            CharSequence formattedDate;

            if (Math.abs(now - folder.lastChecked) > DateUtils.WEEK_IN_MILLIS) {
                formattedDate = getContext().getString(R.string.preposition_for_date,
                        DateUtils.formatDateTime(getContext(), folder.lastChecked, flags));
            } else {
                formattedDate = DateUtils.getRelativeTimeSpanString(folder.lastChecked,
                        now, DateUtils.MINUTE_IN_MILLIS, flags);
            }

            if (folder.pushActive) {
                return getContext().getString(R.string.last_refresh_time_format_with_push, formattedDate);
            } else {
                return getContext().getString(R.string.last_refresh_time_format, formattedDate);
            }
        }

        return null;
    }

    private final View.OnClickListener createFlaggedSearch(final Account account, final FolderInfoHolder folder) {
        String searchTitle = getContext().getString(R.string.search_title,
                getContext().getString(R.string.message_list_title, account.getDescription(),
                        folder.displayName),
                getContext().getString(R.string.flagged_modifier));

        LocalSearch search = new LocalSearch(searchTitle);
        search.and(SearchSpecification.SearchField.FLAGGED, "1", SearchSpecification.Attribute.EQUALS);

        search.addAllowedFolder(folder.name);
        search.addAccountUuid(account.getUuid());

        return new FolderClickListener(getContext(), search);
    }

    private final View.OnClickListener createUnreadSearch(final Account account, final FolderInfoHolder folder) {
        String searchTitle = getContext().getString(R.string.search_title,
                getContext().getString(R.string.message_list_title, account.getDescription(),
                        folder.displayName),
                getContext().getString(R.string.unread_modifier));

        LocalSearch search = new LocalSearch(searchTitle);
        search.and(SearchSpecification.SearchField.READ, "1", SearchSpecification.Attribute.NOT_EQUALS);

        search.addAllowedFolder(folder.name);
        search.addAccountUuid(account.getUuid());

        return new FolderClickListener(getContext(), search);
    }

    @Override
    public final boolean hasStableIds() {
        return true;
    }

    public final void setFilter(final Filter filter) {
        this.mFilter = filter;
    }

    @Override
    public final Filter getFilter() {
        return this.mFilter;
    }
}
