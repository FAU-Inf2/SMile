package com.fsck.k9.adapter;

import android.content.Context;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.fsck.k9.Account;
import com.fsck.k9.AccountStats;
import com.fsck.k9.BaseAccount;
import com.fsck.k9.FontSizes;
import com.fsck.k9.K9;
import com.fsck.k9.activity.FolderList;
import com.fsck.k9.activity.listener.AccountClickListener;
import com.fsck.k9.controller.MessagingController;
import com.fsck.k9.helper.SizeFormatter;
import com.fsck.k9.search.LocalSearch;
import com.fsck.k9.search.SearchAccount;
import com.fsck.k9.search.SearchSpecification;
import com.fsck.k9.view.ColorChip;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import de.fau.cs.mad.smile.android.R;

public class AccountsAdapter extends ArrayAdapter<BaseAccount> {
    private final ConcurrentHashMap<String, AccountStats> accountStats;

    public AccountsAdapter(Context context, List<BaseAccount> accounts) {
        this(context, accounts, null);
    }

    public AccountsAdapter(Context context, List<BaseAccount> accounts, ConcurrentHashMap<String, AccountStats> accountStats) {
        super(context, 0, accounts);
        this.accountStats = accountStats;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final BaseAccount account = getItem(position);
        View view;

        if (convertView != null) {
            view = convertView;
        } else {
            LayoutInflater layoutInflater = LayoutInflater.from(getContext());
            view = layoutInflater.inflate(R.layout.accounts_item, parent, false);
        }

        AccountViewHolder holder = getOrCreateAccountViewHolder(view);
        String description = account.getDescription();


        AccountStats stats = getAccountStats(account);
        setMailFromStats(stats, account, holder);

        if(stats != null) {
            Integer unreadMessageCount = null;
            unreadMessageCount = stats.unreadMessageCount;
            holder.newMessageCount.setText(Integer.toString(unreadMessageCount));
            holder.newMessageCountWrapper.setVisibility(unreadMessageCount > 0 ? View.VISIBLE : View.GONE);

            holder.flaggedMessageCount.setText(Integer.toString(stats.flaggedMessageCount));
            holder.flaggedMessageCountWrapper.setVisibility(K9.messageListStars() && stats.flaggedMessageCount > 0 ? View.VISIBLE : View.GONE);

            holder.flaggedMessageCountWrapper.setOnClickListener(createFlaggedSearchListener(account));
            holder.newMessageCountWrapper.setOnClickListener(createUnreadSearchListener(account));

            holder.activeIcons.setOnClickListener(
                    new View.OnClickListener() {
                        public void onClick(View v) {
                            Toast toast = Toast.makeText(getContext().getApplicationContext(), getContext().getString(R.string.tap_hint), Toast.LENGTH_SHORT);
                            toast.show();
                        }
                    }
            );
        } else {
            holder.newMessageCountWrapper.setVisibility(View.GONE);
            holder.flaggedMessageCountWrapper.setVisibility(View.GONE);
        }

        if (description == null || description.isEmpty()) {
            description = account.getEmail();
        }

        holder.description.setText(description);

        if (account instanceof Account) {
            Account realAccount = (Account) account;
            holder.chip.setBackgroundColor(realAccount.getChipColor());
            holder.flaggedMessageCountIcon.setBackgroundDrawable(realAccount.generateColorChip(false, false, false, false, true).drawable());
            holder.newMessageCountIcon.setBackgroundDrawable(realAccount.generateColorChip(false, false, false, false, false).drawable());
        } else {
            holder.chip.setBackgroundColor(0xff999999);
            holder.newMessageCountIcon.setBackgroundDrawable(new ColorChip(0xff999999, false, ColorChip.CIRCULAR).drawable());
            holder.flaggedMessageCountIcon.setBackgroundDrawable(new ColorChip(0xff999999, false, ColorChip.STAR).drawable());
        }

        FontSizes fontSizes = K9.getFontSizes();
        fontSizes.setViewTextSize(holder.description, fontSizes.getAccountName());
        fontSizes.setViewTextSize(holder.email, fontSizes.getAccountDescription());

        if (account instanceof SearchAccount) {
            holder.folders.setVisibility(View.GONE);
        } else {
            holder.folders.setVisibility(View.VISIBLE);
            holder.folders.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    FolderList.actionHandleAccount(getContext(), (Account) account);

                }
            });
        }

        return view;
    }

    @NonNull
    private AccountViewHolder getOrCreateAccountViewHolder(View view) {
        AccountViewHolder holder = (AccountViewHolder) view.getTag();

        if (holder == null) {
            holder = new AccountViewHolder();
            holder.description = (TextView) view.findViewById(R.id.description);
            holder.email = (TextView) view.findViewById(R.id.email);
            holder.newMessageCount = (TextView) view.findViewById(R.id.new_message_count);
            holder.flaggedMessageCount = (TextView) view.findViewById(R.id.flagged_message_count);
            holder.newMessageCountWrapper = (View) view.findViewById(R.id.new_message_count_wrapper);
            holder.flaggedMessageCountWrapper = (View) view.findViewById(R.id.flagged_message_count_wrapper);
            holder.newMessageCountIcon = (View) view.findViewById(R.id.new_message_count_icon);
            holder.flaggedMessageCountIcon = (View) view.findViewById(R.id.flagged_message_count_icon);
            holder.activeIcons = (RelativeLayout) view.findViewById(R.id.active_icons);

            holder.chip = view.findViewById(R.id.chip);
            holder.folders = (ImageButton) view.findViewById(R.id.folders);
            holder.accountsItemLayout = (LinearLayout) view.findViewById(R.id.accounts_item_layout);

            view.setTag(holder);
        }

        return holder;
    }

    private AccountStats getAccountStats(BaseAccount account) {
        if(accountStats == null) {
            return null;
        }

        return accountStats.get(account.getUuid());
    }

    private void setMailFromStats(AccountStats stats, BaseAccount account, AccountViewHolder holder) {
        String description = account.getDescription();
        if(stats != null && account instanceof Account && stats.size >= 0) {
            holder.email.setText(SizeFormatter.formatSize(getContext(), stats.size));
            holder.email.setVisibility(View.VISIBLE);
        } else {
            if (account.getEmail().equals(description)) {
                holder.email.setVisibility(View.GONE);
            } else {
                holder.email.setVisibility(View.VISIBLE);
                holder.email.setText(account.getEmail());
            }
        }
    }

    private View.OnClickListener createFlaggedSearchListener(BaseAccount account) {
        final Context context = getContext();
        String searchTitle = context.getString(R.string.search_title, account.getDescription(),
                context.getString(R.string.flagged_modifier));

        LocalSearch search;
        if (account instanceof SearchAccount) {
            search = ((SearchAccount) account).getRelatedSearch().clone();
            search.setName(searchTitle);
        } else {
            search = new LocalSearch(searchTitle);
            search.addAccountUuid(account.getUuid());

            Account realAccount = (Account) account;
            realAccount.excludeSpecialFolders(search);
            realAccount.limitToDisplayableFolders(search);
        }

        search.and(SearchSpecification.SearchField.FLAGGED, "1", SearchSpecification.Attribute.EQUALS);

        return new AccountClickListener(context, search);
    }

    private View.OnClickListener createUnreadSearchListener(BaseAccount account) {
        LocalSearch search = MessagingController.createUnreadSearch(getContext().getApplicationContext(), account);
        return new AccountClickListener(getContext(), search);
    }

    class AccountViewHolder {
        public TextView description;
        public TextView email;
        public TextView newMessageCount;
        public TextView flaggedMessageCount;
        public View newMessageCountIcon;
        public View flaggedMessageCountIcon;
        public View newMessageCountWrapper;
        public View flaggedMessageCountWrapper;
        public RelativeLayout activeIcons;
        public View chip;
        public ImageButton folders;
        public LinearLayout accountsItemLayout;
    }
}