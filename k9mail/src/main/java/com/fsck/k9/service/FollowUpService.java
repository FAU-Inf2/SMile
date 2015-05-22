package com.fsck.k9.service;

import android.content.Intent;
import android.util.Log;
import com.fsck.k9.K9;
import com.fsck.k9.Preferences;
import com.fsck.k9.activity.FolderInfoHolder;
import com.fsck.k9.controller.MessagingController;
import com.fsck.k9.Account;
import com.fsck.k9.controller.MessagingListener;
import com.fsck.k9.mail.Folder;
import com.fsck.k9.mail.Message;
import com.fsck.k9.mail.MessageRetrievalListener;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mailstore.LocalFolder;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class FollowUpService extends CoreService {

    @Override
    public void onCreate() {
        super.onCreate();

        if (K9.DEBUG) {
            Log.v(K9.LOG_TAG, "***** WiedervorlageService *****: onCreate");
        }
    }

    @Override
    public int startService(Intent intent, int startId) {
        if (K9.DEBUG) {
            Log.i(K9.LOG_TAG, "WiedervorlageService.startService(" + intent + ", " + startId + ") alive and kicking");
        }

        Preferences prefs = Preferences.getPreferences(FollowUpService.this);

        for(Account acc : prefs.getAccounts()) {
            try {
                LocalFolder folder = acc.getLocalStore().getFolder("FollowUp");

                if(!folder.exists()) {
                    folder.create(Folder.FolderType.HOLDS_MESSAGES);
                }

                List<? extends Message> localMessages = folder.getMessages(null);

                for(Message msg : localMessages) {
                }

            } catch (MessagingException e) {
                e.printStackTrace();
            }
        }

        return 0;
    }
}
