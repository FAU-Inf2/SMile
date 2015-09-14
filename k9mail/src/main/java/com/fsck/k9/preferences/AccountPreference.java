package com.fsck.k9.preferences;

import android.content.Context;
import android.support.v7.preference.Preference;

import com.fsck.k9.Account;

public class AccountPreference extends Preference {
    private final Account account;

    public AccountPreference(Context context, Account account, OnPreferenceClickListener listener) {
        super(context);
        this.account = account;
        setPersistent(false);
        setTitle(account.getDescription());
        setOnPreferenceClickListener(listener);
    }

    public Account getAccount() {
        return account;
    }
}
