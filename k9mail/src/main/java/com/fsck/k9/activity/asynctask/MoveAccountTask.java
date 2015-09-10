package com.fsck.k9.activity.asynctask;

import android.app.Activity;
import android.app.ProgressDialog;

import com.fsck.k9.Account;
import com.fsck.k9.Preferences;
import com.fsck.k9.activity.Accounts;

import de.fau.cs.mad.smile.android.R;

public class MoveAccountTask extends ExtendedAsyncTask<Void, Void, Void> {
    private Account mAccount;
    private boolean mUp;

    public MoveAccountTask(Activity activity, Account account, boolean up) {
        super(activity);
        mAccount = account;
        mUp = up;
    }

    @Override
    protected void showProgressDialog() {
        String message = mActivity.getString(R.string.manage_accounts_moving_message);
        mProgressDialog = ProgressDialog.show(mActivity, null, message, true);
    }

    @Override
    protected Void doInBackground(Void... args) {
        mAccount.move(Preferences.getPreferences(mContext), mUp);
        return null;
    }

    @Override
    protected void onPostExecute(Void arg) {
        Accounts activity = (Accounts) mActivity;

        // Let the activity know that the background task is complete
        activity.setNonConfigurationInstance(null);

        activity.refresh();
        removeProgressDialog();
    }
}
