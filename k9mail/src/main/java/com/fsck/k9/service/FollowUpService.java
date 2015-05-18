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

public class WiedervorlageService extends CoreService {
    class MyMessagingListener extends MessagingListener {

        @Override
        public void listFoldersFinished(Account account) {
            // TODO: check account/map results to account?
            MessagingController.getInstance(getApplication()).refreshListener(mListener);
            super.listFoldersFinished(account);
        }

        @Override
        public void listFolders(Account account, List<? extends Folder> folders) {
            List<FolderInfoHolder> newFolders = new LinkedList<FolderInfoHolder>();

            // TODO: adhere to folder class system?
            for (Folder folder : folders) {
                newFolders.add(new FolderInfoHolder(getApplication(), folder, account, -1));
            }

            mFolders.addAll(newFolders);
        }
    }

    class RetrievalListener implements MessageRetrievalListener {

        @Override
        public void messageStarted(String uid, int number, int ofTotal) {

        }

        @Override
        public void messageFinished(Message message, int number, int ofTotal) {

        }

        @Override
        public void messagesFinished(int total) {

        }
    }

    private MyMessagingListener mListener;
    private RetrievalListener mRetListener;
    private List<FolderInfoHolder> mFolders = new ArrayList<FolderInfoHolder>();

    @Override
    public void onCreate() {
        super.onCreate();

        if (K9.DEBUG) {
            Log.v(K9.LOG_TAG, "***** WiedervorlageService *****: onCreate");
        }

        this.mListener = new MyMessagingListener();
        this.mRetListener = new RetrievalListener();

        MessagingController.getInstance(getApplication()).addListener(this.mListener);
        MessagingController.getInstance(getApplication()).addListener(this.mRetListener);
    }

    @Override
    public int startService(Intent intent, int startId) {
        if (K9.DEBUG) {
            Log.i(K9.LOG_TAG, "WiedervorlageService.startService(" + intent + ", " + startId + ") alive and kicking");
        }

        Preferences prefs = Preferences.getPreferences(WiedervorlageService.this);
        //MessagingController controller = MessagingController.getInstance(getApplication());

        for(Account acc : prefs.getAccounts()) {
            try {
                LocalFolder folder = acc.getLocalStore().getFolder("Ruminant");
                folder.getMessages();
            } catch (MessagingException e) {
                e.printStackTrace();
            }
            /*
            controller.listFolders(acc, false, this.mListener);
            // TODO: whats the difference?
            controller.listFoldersSynchronous(acc,false, this.mListener);*/
        }

        return 0;
    }
}
