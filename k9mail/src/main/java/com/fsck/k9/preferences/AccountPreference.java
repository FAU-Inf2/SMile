package com.fsck.k9.preferences;

import android.content.Context;
import android.support.v7.preference.Preference;
import android.util.AttributeSet;

import com.fsck.k9.Account;

import de.fau.cs.mad.smile.android.R;

public class AccountPreference extends Preference {
    private final Account account;

    public AccountPreference(Context context, AttributeSet attributeSet, Account account, OnPreferenceClickListener listener) {
        super(context, attributeSet);
        this.account = account;
        setPersistent(false);
        setTitle(account.getDescription());
        setOnPreferenceClickListener(listener);
    }

    public Account getAccount() {
        return account;
    }
}
