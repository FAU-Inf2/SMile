package com.fsck.k9.fragment;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.fsck.k9.Account;
import com.fsck.k9.K9;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.RemindMe;
import com.fsck.k9.mailstore.LocalRemindMe;
import com.fsck.k9.mailstore.LocalStore;

import java.lang.ref.WeakReference;
import java.util.List;

public class LoadRemindMe extends AsyncTask<Void, Void, List<RemindMe>> {
    private final WeakReference<MessageListFragment> weakReference;
    private Context context;
    private final Account account;

    public LoadRemindMe(Context context, Account account, MessageListFragment fragment) {
        this.context = context;
        this.account = account;
        this.weakReference = new WeakReference<>(fragment);
    }

    @Override
    protected List<RemindMe> doInBackground(Void... params) {
        try {
            LocalStore store = LocalStore.getInstance(account, context);
            LocalRemindMe localRemindMe = new LocalRemindMe(store);
            return localRemindMe.getAllRemindMes();
        } catch (MessagingException e) {
            Log.e(K9.LOG_TAG, "Unable to retrieve FollowUps", e);
        }
        return null;
    }

    @Override
    protected void onPostExecute(List<RemindMe> remindMes) {
        super.onPostExecute(remindMes);
        MessageListFragment fragment = weakReference.get();
        if(fragment != null) {
            fragment.addRemindMe(remindMes);
        }
    }
}
