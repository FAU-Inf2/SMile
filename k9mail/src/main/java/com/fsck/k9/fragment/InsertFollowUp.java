package com.fsck.k9.fragment;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.fsck.k9.Account;
import com.fsck.k9.K9;
import com.fsck.k9.controller.MessagingController;
import com.fsck.k9.mail.Folder;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.RemindMe;
import com.fsck.k9.mailstore.LocalFolder;
import com.fsck.k9.mailstore.LocalMessage;
import com.fsck.k9.mailstore.LocalRemindMe;
import com.fsck.k9.mailstore.LocalStore;

import java.util.ArrayList;
import java.util.Arrays;

class InsertFollowUp extends AsyncTask<RemindMe, Void, Void> {
    private Context context;
    private final Account account;

    public InsertFollowUp(Context context, Account account) {
        this.context = context;
        this.account = account;
    }

    @Override
    protected Void doInBackground(RemindMe... params) {
        for(RemindMe remindMe : params) {
            try {
                LocalStore store = LocalStore.getInstance(account, context);
                LocalRemindMe localRemindMe = new LocalRemindMe(store);

                LocalFolder folder = new LocalFolder(store, account.getRemindMeFolderName());
                folder.open(Folder.OPEN_MODE_RW);

                remindMe.setFolderId(folder.getId());

                Log.d(K9.LOG_TAG, "Inserting remindMe: " + remindMe);
                // TODO: remove messagingController
                MessagingController messagingController = MessagingController.getInstance(context);
                messagingController.moveMessages(account,
                        remindMe.getReference().getFolder().getName(),
                        new ArrayList<LocalMessage>(Arrays.asList((LocalMessage) remindMe.getReference())),
                        account.getRemindMeFolderName(), null);

                if(remindMe.getId() > 0) {
                    localRemindMe.update(remindMe);
                } else {
                    localRemindMe.add(remindMe);
                }
            } catch (MessagingException e) {
                Log.e(K9.LOG_TAG, "Unable to insert followup", e);
            }
        }
        return null;
    }
}
