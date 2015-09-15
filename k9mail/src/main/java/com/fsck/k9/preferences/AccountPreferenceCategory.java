package com.fsck.k9.preferences;

import android.content.Context;
import android.support.v7.preference.PreferenceCategory;
import android.util.AttributeSet;

import com.fsck.k9.Account;
import com.fsck.k9.activity.setup.GlobalPreferences;

public class AccountPreferenceCategory extends PreferenceCategory {
    private AttributeSet attributeSet;
    private Context context;

    public AccountPreferenceCategory(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
        this.attributeSet = attrs;
    }

    public void addAccount(Account account, GlobalPreferences.OnAccountPreferenceClickListener listener) {
        addPreference(new AccountPreference(context, attributeSet, account, listener));
    }
}
