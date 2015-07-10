package com.fsck.k9.activity;

import android.app.FragmentManager;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import com.fsck.k9.Account;
import com.fsck.k9.Preferences;
import com.fsck.k9.fragment.MessageFragment;
import com.fsck.k9.search.LocalSearch;
import com.fsck.k9.search.SearchAccount;
import com.fsck.k9.search.SearchSpecification;

import java.util.Collection;
import java.util.List;

import de.fau.cs.mad.smile.android.R;

public class Messages extends SmileActivity {
    private static final String ACTION_SHORTCUT = "shortcut";
    private static final String EXTRA_SEARCH = "search";
    public static final String EXTRA_MESSAGE_REFERENCE = "message_reference";
    private static final String EXTRA_SPECIAL_FOLDER = "special_folder";
    public static final String EXTRA_SEARCH_ACCOUNT = "com.fsck.k9.search_account";
    public static final String EXTRA_SEARCH_FOLDER = "com.fsck.k9.search_folder";

    private LocalSearch mSearch;
    private MessageReference mMessageReference;

    public static void actionDisplaySearch(Context context,
                                           SearchSpecification search) {
        context.startActivity(intentDisplaySearch(context, search));
    }

    public static Intent intentDisplaySearch(Context context, SearchSpecification search) {
        Intent intent = new Intent(context, Messages.class);
        intent.putExtra(EXTRA_SEARCH, search);

        return intent;
    }

    public static Intent shortcutIntent(Context context, String specialFolder) {
        Intent intent = new Intent(context, Messages.class);
        intent.setAction(ACTION_SHORTCUT);
        intent.putExtra(EXTRA_SPECIAL_FOLDER, specialFolder);

        return intent;
    }

    public static Intent actionDisplayMessageIntent(Context context,
                                                         MessageReference messageReference) {
        Intent intent = new Intent(context, Messages.class);
        intent.putExtra(EXTRA_MESSAGE_REFERENCE, messageReference);
        return intent;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // TODO: according to documentation long running tasks like db upgrade should not be placed inside an activity
        if (UpgradeDatabases.actionUpgradeDatabases(this, getIntent())) {
            finish();
            return;
        }

        handleIntent(getIntent());

        FragmentManager fragmentManager = getFragmentManager();
        MessageFragment messageFragment = (MessageFragment) fragmentManager.findFragmentById(R.layout.message_list_fragment);

        if(messageFragment == null) {
            messageFragment = MessageFragment.newInstance(mSearch);
        }

        fragmentManager.beginTransaction()
                .replace(R.id.content_frame, messageFragment)
                .commit();
    }

    private final void handleIntent(final Intent intent) {
        String action = intent.getAction();

        if (Intent.ACTION_VIEW.equals(action) && intent.getData() != null) {
            decodeExtraActionView(intent);
        } else if (ACTION_SHORTCUT.equals(action)) {
            decodeExtraActionShortcut(intent);
        } else if (intent.getStringExtra(SearchManager.QUERY) != null) {
            decodeExtraActionSearch(intent);
        } else {
            // regular LocalSearch object was passed
            mSearch = intent.getParcelableExtra(EXTRA_SEARCH);
        }
    }

    private final void decodeExtraActionSearch(final Intent intent) {
        // check if this intent comes from the system search ( remote )
        if (!Intent.ACTION_SEARCH.equals(intent.getAction())) {
            return;
        }
        //Query was received from Search Dialog
        final String query = intent.getStringExtra(SearchManager.QUERY).trim();

        mSearch = new LocalSearch(getString(R.string.search_results));
        mSearch.setManualSearch(true);

        mSearch.or(new SearchSpecification.SearchCondition(SearchSpecification.SearchField.SENDER, SearchSpecification.Attribute.CONTAINS, query));
        mSearch.or(new SearchSpecification.SearchCondition(SearchSpecification.SearchField.SUBJECT, SearchSpecification.Attribute.CONTAINS, query));
        // FIXME: SqlQueryBuilder throws RTE
        //mSearch.or(new SearchCondition(SearchField.MESSAGE_CONTENTS, Attribute.CONTAINS, query));

        Bundle appData = intent.getBundleExtra(SearchManager.APP_DATA);
        if (appData != null) {
            mSearch.addAccountUuid(appData.getString(EXTRA_SEARCH_ACCOUNT));
            // searches started from a folder list activity will provide an account, but no folder
            if (appData.getString(EXTRA_SEARCH_FOLDER) != null) {
                mSearch.addAllowedFolder(appData.getString(EXTRA_SEARCH_FOLDER));
            }
        } else {
            mSearch.addAccountUuid(LocalSearch.ALL_ACCOUNTS);
        }
    }

    private final void decodeExtraActionShortcut(final Intent intent) {
        // Handle shortcut intents
        final String specialFolder = intent.getStringExtra(EXTRA_SPECIAL_FOLDER);
        if (SearchAccount.UNIFIED_INBOX.equals(specialFolder)) {
            mSearch = SearchAccount.createUnifiedInboxAccount(this).getRelatedSearch();
        } else if (SearchAccount.ALL_MESSAGES.equals(specialFolder)) {
            mSearch = SearchAccount.createAllMessagesAccount(this).getRelatedSearch();
        }
    }

    private final void decodeExtraActionView(final Intent intent) {
        final Uri uri = intent.getData();
        List<String> segmentList = uri.getPathSegments();
        String accountId = segmentList.get(0);
        Collection<Account> accounts = Preferences.getPreferences(this).getAvailableAccounts();

        for (Account account : accounts) {
            if (String.valueOf(account.getAccountNumber()).equals(accountId)) {
                String folderName = segmentList.get(1);
                String messageUid = segmentList.get(2);
                mMessageReference = new MessageReference(account.getUuid(), folderName, messageUid, null);
                break;
            }
        }
    }
}
