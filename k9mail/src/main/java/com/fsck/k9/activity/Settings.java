package com.fsck.k9.activity;

import android.app.Activity;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatDelegate;

import com.fsck.k9.Account;
import com.fsck.k9.Preferences;
import com.fsck.k9.activity.setup.AccountPreferences;
import com.fsck.k9.activity.setup.GlobalPreferences;
import com.fsck.k9.fragment.SmilePreferenceFragment;

import de.fau.cs.mad.smile.android.R;

public class Settings extends SmileActivity implements GlobalPreferences.GlobalPreferencesCallback {
    private final static String EDIT_ACCOUNT_ACTION = "EDIT_ACCOUNT";
    private final static String ACCOUNT_EXTRA = "account";

    public static void actionPreferences(Context context) {
        Intent intent = new Intent(context, Settings.class);
        context.startActivity(intent);
    }

    public static void actionAccountPreferences(Context context, Account account) {
        Intent intent = new Intent(context, Settings.class);
        intent.setAction(EDIT_ACCOUNT_ACTION);
        intent.putExtra(ACCOUNT_EXTRA, account.getUuid());
        context.startActivity(intent);
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        final Intent intent = getIntent();
        final String action = intent.getAction();

        if(action == null) {
            GlobalPreferences fragment = GlobalPreferences.newInstance(this);
            loadPreference(fragment);
        } else if (action.equals(EDIT_ACCOUNT_ACTION)) {
            final String accountUuid = intent.getStringExtra(ACCOUNT_EXTRA);
            Account account = Preferences.getPreferences(this).getAccount(accountUuid);
            onAccountClick(account);
        }
    }

    @Override
    public void onAccountClick(Account account) {
        AccountPreferences fragment = AccountPreferences.newInstance(account);
        loadPreference(fragment);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    private void loadPreference(SmilePreferenceFragment fragment) {
        FragmentManager fm = getFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();
        ft.replace(R.id.content_frame, fragment);
        ft.addToBackStack(null);
        ft.commit();
    }
}
