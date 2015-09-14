package com.fsck.k9.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;

import com.fsck.k9.Account;
import com.fsck.k9.Preferences;
import com.fsck.k9.activity.setup.AccountPreferences;
import com.fsck.k9.activity.setup.FontSizePreferences;
import com.fsck.k9.activity.setup.GlobalPreferences;
import com.fsck.k9.fragment.SmilePreferenceFragment;

import de.fau.cs.mad.smile.android.R;

public class Settings extends AppCompatActivity implements GlobalPreferences.GlobalPreferencesCallback {
    private final static String EDIT_ACCOUNT_ACTION = "EDIT_ACCOUNT";
    private static final String EDIT_FOLDER_ACTION = "EDIT_FOLDER";
    private final static String ACCOUNT_EXTRA = "account";
    private final static String FOLDER_EXTRA = "folder";

    private K9ActivityCommon mBase;

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
            loadPreference(fragment);
        } else if (EDIT_ACCOUNT_ACTION.equals(action)) {
            final String accountUuid = intent.getStringExtra(ACCOUNT_EXTRA);
            Account account = Preferences.getPreferences(this).getAccount(accountUuid);
            onAccountClick(account);
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
        loadPreference(fragment);
    }

    @Override
    public void onFontSizeSettings() {
        FontSizePreferences fragment = FontSizePreferences.newInstance();
        loadPreference(fragment);
    }

    @Override
    public void onBackPressed() {
        //FragmentManager fragmentManager = getFragmentManager();
        FragmentManager fragmentManager = getSupportFragmentManager();
        if(fragmentManager.getBackStackEntryCount() > 1) { // TODO: Workaround
            fragmentManager.popBackStack();
        } else {
            finish();
        }
        //super.onBackPressed();
    }

    private void loadPreference(SmilePreferenceFragment fragment) {
        //FragmentManager fm = getFragmentManager();
        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();
        ft.replace(R.id.content_frame, fragment);
        ft.addToBackStack(null);
        ft.commit();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

}
