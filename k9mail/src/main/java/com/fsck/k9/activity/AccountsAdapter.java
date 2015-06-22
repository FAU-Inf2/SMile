package com.fsck.k9.activity;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.fsck.k9.Account;
import com.fsck.k9.BaseAccount;
import com.fsck.k9.FontSizes;
import com.fsck.k9.K9;

import java.util.List;

import de.fau.cs.mad.smile.android.R;

class AccountsAdapter extends ArrayAdapter<BaseAccount> {
    public AccountsAdapter(Context context, List<BaseAccount> accounts) {
        super(context, 0, accounts);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final BaseAccount account = getItem(position);

        final View view;
        if (convertView != null) {
            view = convertView;
        } else {
            LayoutInflater layoutInflater = LayoutInflater.from(getContext());
            view = layoutInflater.inflate(R.layout.accounts_item, parent, false);
            view.findViewById(R.id.active_icons).setVisibility(View.GONE);
            view.findViewById(R.id.folders).setVisibility(View.GONE);
        }

        AccountViewHolder holder = (AccountViewHolder) view.getTag();
        if (holder == null) {
            holder = new AccountViewHolder();
            holder.description = (TextView) view.findViewById(R.id.description);
            holder.email = (TextView) view.findViewById(R.id.email);
            holder.chip = view.findViewById(R.id.chip);

            view.setTag(holder);
        }

        String description = account.getDescription();
        if (account.getEmail().equals(description)) {
            holder.email.setVisibility(View.GONE);
        } else {
            holder.email.setVisibility(View.VISIBLE);
            holder.email.setText(account.getEmail());
        }

        if (description == null || description.isEmpty()) {
            description = account.getEmail();
        }

        holder.description.setText(description);

        if (account instanceof Account) {
            Account realAccount = (Account) account;
            holder.chip.setBackgroundColor(realAccount.getChipColor());
        } else {
            holder.chip.setBackgroundColor(0xff999999);
        }

        holder.chip.getBackground().setAlpha(255);

        FontSizes fontSizes = K9.getFontSizes();
        fontSizes.setViewTextSize(holder.description, fontSizes.getAccountName());
        fontSizes.setViewTextSize(holder.email, fontSizes.getAccountDescription());


        return view;
    }

    private class AccountViewHolder {
        public TextView description;
        public TextView email;
        public View chip;
    }
}
