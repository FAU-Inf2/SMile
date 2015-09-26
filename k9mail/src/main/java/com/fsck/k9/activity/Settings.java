package com.fsck.k9.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.NavUtils;
import android.support.v4.app.TaskStackBuilder;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.support.v7.preference.PreferenceFragmentCompat.OnPreferenceStartScreenCallback;
import android.support.v7.preference.PreferenceScreen;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;

import com.fsck.k9.Account;
import com.fsck.k9.Preferences;
import com.fsck.k9.activity.setup.AccountPreferences;
import com.fsck.k9.activity.setup.FontSizePreferences;
import com.fsck.k9.activity.setup.GlobalPreferences;
import com.fsck.k9.activity.setup.GlobalPreferences.GlobalPreferencesCallback;
import com.fsck.k9.fragment.SmilePreferenceFragment;
import com.fsck.k9.activity.setup.AccountPreferences.AccountPreferenceFragmentCallback;

import de.fau.cs.mad.smile.android.R;

public class Settings extends AppCompatActivity
        implements GlobalPreferencesCallback, OnPreferenceStartScreenCallback, AccountPreferenceFragmentCallback {
    private final static String EDIT_ACCOUNT_ACTION = "EDIT_ACCOUNT";
    private static final String EDIT_FOLDER_ACTION = "EDIT_FOLDER";
    private final static String ACCOUNT_EXTRA = "account";
    private final static String FOLDER_EXTRA = "folder";

    private K9ActivityCommon mBase;
    private AccountPreferences currentFragment;

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

    public static void actionFolderPreferences(Context context, Account account, String folderName) {
        Intent intent = new Intent(context, Settings.class);
        intent.setAction(EDIT_FOLDER_ACTION);
        intent.putExtra(ACCOUNT_EXTRA, account.getUuid());
        intent.putExtra(FOLDER_EXTRA, folderName);
        context.startActivity(intent);
    }

    @Override
    public void onCreate(Bundle icicle) {
        mBase = K9ActivityCommon.newInstance(this);
        super.onCreate(icicle);

        setContentView(R.layout.settings);
        Toolbar toolbar = (Toolbar)findViewById(R.id.toolbar);

        if(toolbar != null) {
            toolbar.setTitle(R.string.preferences_title);
            setSupportActionBar(toolbar);
        }

        ActionBar actionBar = getSupportActionBar();

        if(actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        final Intent intent = getIntent();
        final String action = intent.getAction();

        if(action == null) {
            GlobalPreferences fragment = GlobalPreferences.newInstance(this);
            loadPreference(fragment, false);
        } else if (EDIT_ACCOUNT_ACTION.equals(action)) {
            final String accountUuid = intent.getStringExtra(ACCOUNT_EXTRA);
            Account account = Preferences.getPreferences(this).getAccount(accountUuid);
            AccountPreferences fragment = AccountPreferences.newInstance(account);
            loadPreference(fragment, false);
        } else if (EDIT_FOLDER_ACTION.equals(action)) {
            final String accountUuid = intent.getStringExtra(ACCOUNT_EXTRA);
            Account account = Preferences.getPreferences(this).getAccount(accountUuid);
            final String folderName = intent.getStringExtra(FOLDER_EXTRA);
            //onFolderClick(account);
        }
    }

    @Override
    public void onAccountClick(Account account) {
        AccountPreferences fragment = AccountPreferences.newInstance(account);
        loadPreference(fragment, true);
    }

    @Override
    public void onFontSizeSettings() {
        FontSizePreferences fragment = FontSizePreferences.newInstance();
        loadPreference(fragment, true);
    }

    private void loadPreference(SmilePreferenceFragment fragment, boolean addToBackStack) {
        final String tag = fragment.getClass().getSimpleName();
        loadPreference(fragment, addToBackStack, tag);
    }

    private void loadPreference(SmilePreferenceFragment fragment, boolean addToBackStack, final String tag) {
        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();
        ft.replace(R.id.content_frame, fragment, tag);

        if(addToBackStack) {
            ft.addToBackStack(tag);
        }

        ft.commit();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            Intent upIntent = NavUtils.getParentActivityIntent(this);
            NavUtils.navigateUpTo(this, upIntent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onPreferenceStartScreen(PreferenceFragmentCompat preferenceFragmentCompat, PreferenceScreen preferenceScreen) {
        if(preferenceFragmentCompat instanceof SmilePreferenceFragment) {
            SmilePreferenceFragment smilePreferenceFragment = (SmilePreferenceFragment)preferenceFragmentCompat;
            SmilePreferenceFragment subScreen = smilePreferenceFragment.openPreferenceScreen();
            Bundle args = subScreen.getArguments();
            if(args == null) {
                args = new Bundle();
            }

            args.putString(PreferenceFragmentCompat.ARG_PREFERENCE_ROOT, preferenceScreen.getKey());
            subScreen.setArguments(args);
            loadPreference(subScreen, true, preferenceScreen.getKey());
            return true;
        }

        return false;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(currentFragment != null) {
            currentFragment.onActivityResult(requestCode, resultCode, data);
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void registerFragment(AccountPreferences fragment) {
        currentFragment = fragment;
    }
}
