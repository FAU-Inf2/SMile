package com.fsck.k9.asynctask;

import android.content.Context;
import android.os.AsyncTask;

import com.fsck.k9.Account;
import com.fsck.k9.Preferences;

import java.util.List;

/**
 * Load accounts in a background thread
 */
public class LoadAccountTask extends AsyncTask<Void, Void, List<Account>> {
    private final Context mContext;
    private final LoadAccountsCallback callback;

    public LoadAccountTask(Context context, LoadAccountsCallback callback) {
        this.mContext = context;
        this.callback = callback;
    }

    @Override
    protected List<Account> doInBackground(Void... params) {
        List<Account> accounts = Preferences.getPreferences(mContext).getAccounts();
        return accounts;
    }

    @Override
    protected void onPostExecute(List<Account> accounts) {
        if(callback == null) {
            return;
        }

        callback.onAccountsLoaded(accounts);
    }

    public interface LoadAccountsCallback {
        void onAccountsLoaded(final List<Account> accounts);
    }
}
