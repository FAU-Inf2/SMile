package com.fsck.k9.activity;

import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

import com.fsck.k9.Account;
import com.fsck.k9.BaseAccount;
import com.fsck.k9.K9;
import com.fsck.k9.Preferences;
import com.fsck.k9.adapter.AccountsAdapter;
import com.fsck.k9.search.SearchAccount;

import java.util.ArrayList;
import java.util.List;

import de.fau.cs.mad.smile.android.R;


/**
 * Activity displaying the list of accounts.
 *
 * <p>
 * Classes extending this abstract class have to provide an {@link #onAccountSelected(BaseAccount)}
 * method to perform an action when an account is selected.
 * </p>
 */
public abstract class AccountList extends K9ListActivity implements OnItemClickListener {

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        setResult(RESULT_CANCELED);

        setContentView(R.layout.account_list);

        ListView listView = getListView();
        listView.setOnItemClickListener(this);
        listView.setItemsCanFocus(false);
    }

    /**
     * Reload list of accounts when this activity is resumed.
     */
    @Override
    public void onResume() {
        super.onResume();
        new LoadAccounts().execute();
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        BaseAccount account = (BaseAccount) parent.getItemAtPosition(position);
        onAccountSelected(account);
    }

    /**
     * Create a new {@link AccountsAdapter} instance and assign it to the {@link ListView}.
     *
     * @param realAccounts
     *         An array of accounts to display.
     */
    public void populateListView(List<Account> realAccounts) {
        List<BaseAccount> accounts = new ArrayList<BaseAccount>();

        if (displaySpecialAccounts() && !K9.isHideSpecialAccounts()) {
            BaseAccount unifiedInboxAccount = SearchAccount.createUnifiedInboxAccount(this);
            BaseAccount allMessagesAccount = SearchAccount.createAllMessagesAccount(this);

            accounts.add(unifiedInboxAccount);
            accounts.add(allMessagesAccount);
        }

        accounts.addAll(realAccounts);
        AccountsAdapter adapter = new AccountsAdapter(this, accounts);
        ListView listView = getListView();
        listView.setAdapter(adapter);
        listView.invalidate();
    }

    /**
     * Implementing decide whether or not to display special accounts in the list.
     *
     * @return {@code true}, if special accounts should be listed. {@code false}, otherwise.
     */
    protected abstract boolean displaySpecialAccounts();

    /**
     * This method will be called when an account was selected.
     *
     * @param account
     *         The account the user selected.
     */
    protected abstract void onAccountSelected(BaseAccount account);

    /**
     * Load accounts in a background thread
     */
    class LoadAccounts extends AsyncTask<Void, Void, List<Account>> {
        @Override
        protected List<Account> doInBackground(Void... params) {
            List<Account> accounts = Preferences.getPreferences(getApplicationContext()).getAccounts();
            return accounts;
        }

        @Override
        protected void onPostExecute(List<Account> accounts) {
            populateListView(accounts);
        }
    }
}
