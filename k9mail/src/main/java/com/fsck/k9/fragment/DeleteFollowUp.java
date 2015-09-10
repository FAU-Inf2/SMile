package com.fsck.k9.fragment;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.fsck.k9.Account;
import com.fsck.k9.K9;
import com.fsck.k9.controller.MessagingController;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.RemindMe;
import com.fsck.k9.mailstore.LocalMessage;
import com.fsck.k9.mailstore.LocalRemindMe;
import com.fsck.k9.mailstore.LocalStore;

import java.util.ArrayList;
import java.util.Arrays;

class DeleteFollowUp extends AsyncTask<RemindMe, Void, Void> {
    private final Account account;
    private Context context;

    public DeleteFollowUp(Context context, Account account) {
        this.context = context;
        this.account = account;
    }

    @Override
    protected Void doInBackground(RemindMe... params) {
        try {
            LocalStore store = LocalStore.getInstance(account, context);
            LocalRemindMe localRemindMe = new LocalRemindMe(store);
            for (RemindMe remindMe : params) {
                localRemindMe.delete(remindMe);
                //move back to inbox
                MessagingController messagingController = MessagingController.getInstance(context);
                messagingController.moveMessages(account,
                        remindMe.getReference().getFolder().getName(),
                        new ArrayList<LocalMessage>(Arrays.asList((LocalMessage) remindMe.getReference())),
                        account.getInboxFolderName(), null);

            }
        } catch (MessagingException e) {
            Log.e(K9.LOG_TAG, "Unable to delete RemindMe", e);
        }
        return null;
    }
}
