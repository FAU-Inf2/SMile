package com.fsck.k9;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.support.v7.preference.PreferenceManager;
import android.util.Log;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fsck.k9.activity.IMAPAppendText;
import com.fsck.k9.controller.MessagingController;
import com.fsck.k9.controller.MessagingListener;
import com.fsck.k9.mail.Folder;
import com.fsck.k9.mail.Message;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.RemindMe;
import com.fsck.k9.mailstore.LocalFolder;
import com.fsck.k9.mailstore.LocalRemindMe;
import com.fsck.k9.mailstore.LocalStore;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class FeatureStorage {
    /* Handles saving and parsing information from our features (e.g. follow-up) to the
    * file on the server for cross-platform functionality.*/
    private static ObjectMapper objectMapper;
    private static File localFile;
    private static long lastUpdate = -1; // last time when the local file was parsed

    private final Context mContext;
    private final Account mAccount;
    private final IMAPAppendText appendText;
    private final LocalRemindMe localRemindMe;
    private final String absolutePath; // absolute path to local files
    private final MessagingController messagingController;
    private final MessagingListener listener;

    public FeatureStorage(Account account, Context context) throws MessagingException {
        this.mContext = context;
        this.absolutePath = context.getFilesDir().getAbsolutePath();
        this.messagingController = MessagingController.getInstance(context);
        this.listener = new MyMessagingListener();
        this.messagingController.addListener(listener);
        if(account != null) {
            this.mAccount = account;
            this.localRemindMe = new LocalRemindMe(account.getLocalStore());
            this.appendText = new IMAPAppendText(account, context, messagingController);
        } else {
            mAccount = null;
            appendText = null;
            localRemindMe = null;
        }
    }

    @Override
    protected void finalize() throws Throwable {
        if(this.messagingController != null) {
            this.messagingController.removeListener(listener);
        }

        super.finalize();
    }

    public void pollService() {
        new UpdateAddSaveManager().execute();
    }

    private static class MyMessagingListener extends MessagingListener {
        @Override
        public void folderStatusChanged(Account account, String folderName, int unreadMessageCount) {
            if(account.getSmileStorageFolderName().equals(folderName)) {
                if(K9.DEBUG) {
                    K9.logDebug( "folderStatusChanged in FeatureStorage");
                }
            }

            super.folderStatusChanged(account, folderName, unreadMessageCount);
        }
    }

    private class UpdateAddSaveManager extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... params) {
            if(localRemindMe == null || mAccount == null || appendText == null) {
                return null;
            }


            SharedPreferences preferences =  PreferenceManager.getDefaultSharedPreferences(mContext);
            if (preferences.contains("SmileStorageUpdateTimestamp")) {
                lastUpdate = preferences.getLong("SmileStorageUpdateTimestamp", -1);
            } else {
                setLastUpdate(lastUpdate);
            }

            if (localFile == null) {
                String filePath = FilenameUtils.concat(absolutePath, "smilestorage.json");
                localFile = new File(filePath);
            }

            if (!localFile.exists()) {
                try {
                    setLastUpdate(-1);
                    K9.logDebug( "smilestorage.json does not exist -- create file.");
                    localFile.createNewFile();
                } catch (IOException e) {
                    Log.e(K9.LOG_TAG, "Error while creating local file." + e.getMessage());
                    return null;
                }
            }

            try {
                if(objectMapper == null) {
                    objectMapper = new ObjectMapper();
                }

                synchronizeRemindMeFolder();
                // Is external version newer than the version which is saved locally?
                String newerMessageId = hasUpdate();
                if(newerMessageId == null) { //external and internal version are the same
                    if(!hasLocalChanges()) { // did local version change since last time?
                        K9.logDebug("No local changes, no update on server, do not update.");
                        return null; // nope --> do not update
                    }
                } else {
                    mergeLocalExternalVersion(newerMessageId); // Merge external and local version
                }

                // update/add/delete elements
                String newContent = createUpdatedFile();

                // save new version on server
                appendUpdatedFile(newContent);

            } catch (Exception e) {
                Log.e(K9.LOG_TAG, "Error in jackson mapper:" + e.toString() + e.getMessage());
            }

            return null;
        }

        private void setLastUpdate(long newLastUpdate) {
            lastUpdate = newLastUpdate;
            SharedPreferences preferences =  PreferenceManager.getDefaultSharedPreferences(mContext);
            SharedPreferences.Editor editor = preferences.edit();
            editor.putLong("SmileStorageUpdateTimestamp", newLastUpdate);
            editor.apply();
        }

        /**
         * Checks whether file on server is newer than local version
         * @return MessageId of newest version or null if local version is the newest one
         */
        private String hasUpdate() { //external file has changed
            K9.logDebug("Check whether there is a newer version of smilestorage.json on" +
                     " the mailserver.");

            String currentMessageId = appendText.getCurrentMessageId();
            K9.logDebug("Latest local version is from: " + lastUpdate);
            K9.logDebug("Current MessageId from server: " + currentMessageId);
            if (currentMessageId == null)
                return null;

            try {
                if (Long.parseLong(
                        currentMessageId.replace(appendText.MESSAGE_ID_MAGIC_STRING, "")) >
                        lastUpdate)  //newer version available
                    return currentMessageId;
            } catch (Exception e) {
                Log.e(K9.LOG_TAG, "error in FeatureStorage.hasUpdate", e);
            }
            return null;
        }

        private Boolean hasLocalChanges(){
            K9.logDebug("Check for new local RemindMes.");
            try {
                List<RemindMe> allRemindMes = localRemindMe.getAllRemindMes();
                for(RemindMe r : allRemindMes) {
                    if(r.getLastModified().getTime() > lastUpdate) {
                        return true;
                    }
                }
            } catch (MessagingException e) {
                K9.logDebug("Error while getting all RemindMes.");
            }

            return false;
        }

        /**
         * Merges the new version with the local one and saves it locally.
         */
        private void mergeLocalExternalVersion(String newerMessageId) {
            K9.logDebug( "External version of smilestorage.json is newer -- merge files.");

            String currentContent = appendText.getCurrentContent(newerMessageId);

            SmileFeaturesJsonRoot externalRoot;
            SmileFeaturesJsonRoot internalRoot;
            try{
                try {
                    externalRoot = objectMapper.readValue(currentContent,
                            SmileFeaturesJsonRoot.class);
                } catch (Exception e) {
                    // currentContent was not valid or was null -- nothing to merge
                    K9.logDebug("External version was empty/invalid -- nothing to merge.");
                    setLastUpdate(Long.parseLong(newerMessageId.replace(
                            appendText.MESSAGE_ID_MAGIC_STRING, "")));
                    return;
                }
                try {
                    internalRoot = objectMapper.readValue(localFile, SmileFeaturesJsonRoot.class);
                } catch (Exception e) {
                    // local file was empty -- save external version
                    K9.logDebug( "Local version was empty/invalid -- save external "
                            + "version: " + currentContent);
                    externalRoot.setAllRemindMes(mergeRemindMes(externalRoot.getAllRemindMes(),
                            null));
                    objectMapper.writeValue(localFile, externalRoot);
                    setLastUpdate(Long.parseLong(newerMessageId.replace(
                            appendText.MESSAGE_ID_MAGIC_STRING, "")));
                    return;
                }

                //Merge RemindMes
                try {
                    List<RemindMe> allRemindMes = new ArrayList<RemindMe>();
                    try {
                        allRemindMes = localRemindMe.getAllRemindMes();
                        for(RemindMe f : allRemindMes) {
                            if(f.getReference() == null)
                                continue;
                            try {
                                f.setMessageId(f.getReference().getMessageId());
                            } catch (Exception e) {}
                            f.setUid(f.getReference().getUid());
                        }
                    } catch (Exception e) {
                        Log.e(K9.LOG_TAG, "Could not get all RemindMes from db: " + e.getMessage());
                    }
                    internalRoot.setAllRemindMes(mergeRemindMes(externalRoot.getAllRemindMes(),
                            allRemindMes));
                } catch (Exception e){
                    Log.e(K9.LOG_TAG, "Exception while adding allRemindMes to root: " + e.getMessage());
                }

                K9.logDebug( "New version from mergeLocalExternalVersion(): " + objectMapper.
                        writerWithDefaultPrettyPrinter().writeValueAsString(internalRoot));
                objectMapper.writeValue(localFile, internalRoot);
                setLastUpdate(Long.parseLong(newerMessageId.replace(
                        appendText.MESSAGE_ID_MAGIC_STRING, "")));

                } catch (IOException e) {
                    Log.e(K9.LOG_TAG, "Error in jackson mapper while writing to file: " +
                            e.getMessage());
                }
        }

        /**
         * Adds the new RemindMeMailInformation to the file, removes expired elements and
         * saves it locally.
         * @return String which needs to be appended as new version on the server
         */
        private String createUpdatedFile() {
            try {
                SmileFeaturesJsonRoot root;
                try {
                    root = objectMapper.readValue(localFile, SmileFeaturesJsonRoot.class);
                } catch (Exception e) {
                    //local file may be empty
                    Log.e(K9.LOG_TAG, "Error while reading local file, create new root.");
                    root = new SmileFeaturesJsonRoot();
                }

                try {
                    List<RemindMe> allRemindMes = new ArrayList<RemindMe>();
                    try {
                        allRemindMes = localRemindMe.getAllRemindMes();
                    } catch (Exception e) {
                        Log.e(K9.LOG_TAG, "Could not get all RemindMes from db: " + e.getMessage());
                    }
                    root.setAllRemindMes(allRemindMes);
                } catch (Exception e){
                    Log.e(K9.LOG_TAG, "Exception while adding allRemindMes to root: " + e.getMessage());
                }

                // remove expired nodes
                removePastNodes(root);

                K9.logDebug( "New version from createUpdatedFile(): " + objectMapper.
                        writerWithDefaultPrettyPrinter().writeValueAsString(root));
                //save in local file
                objectMapper.writeValue(localFile, root);
                return objectMapper.writeValueAsString(root);

            } catch (IOException e) {
                Log.e(K9.LOG_TAG, "Error in jackson mapper while writing to file: " +
                        e.getMessage());
            }

            return null;
        }

        private List<RemindMe> mergeRemindMes(List<RemindMe> fileRemindMes, List<RemindMe> dbRemindMes) {
            if(dbRemindMes == null)
                dbRemindMes = new ArrayList<RemindMe>();

            for(RemindMe f : fileRemindMes) {
                boolean found = false;
                for(RemindMe dbf : dbRemindMes) {
                    try {
                        if (dbf.getMessageId().equals(f.getMessageId())) { //Check whether they are equals
                            // remindTime has changed and external version was newer
                            if(f.getRemindTime().getTime() != dbf.getRemindTime().getTime() &&
                                    dbf.getLastModified().getTime() < f.getLastModified().getTime()) {
                                localRemindMe.update(f);
                            }

                            found = true;
                            break;
                        }
                    } catch (Exception e) {
                        Log.e(K9.LOG_TAG, "failed to update RemindMe in database ", e);
                    }
                }

                if (found) {
                    continue;
                }

                try {
                    K9.logDebug( "New RemindMe from server version -- add to local database.");
                    String []uids = {f.getUid()};
                    Message m = null;
                    try {
                        m = mAccount.getLocalStore().getFolder(mAccount.getRemindMeFolderName()).getMessages(uids, null).get(0);
                    } catch (Exception e) {
                            K9.logDebug( "Failed to get message: " + e.getMessage());
                    }
                    if(m == null)
                        continue;

                    f.setReference(m);
                    localRemindMe.add(f);
                    K9.logDebug( "Added new RemindMe to database. MessageId: " + f.getMessageId()
                            + ", title: " +  f.getTitle() + ", uid: " + f.getUid());
                } catch (Exception e) {
                    Log.e(K9.LOG_TAG, "Error while adding new RemindMe to Database: " + e.getMessage());
                }
                dbRemindMes.add(f);
            }

            return dbRemindMes;
        }

        private void removePastNodes(SmileFeaturesJsonRoot root) {
            //remove nodes where there was already an notification
            long timestamp = System.currentTimeMillis();
            //go through and remove old nodes
            List<RemindMe> remindMes = root.getAllRemindMes();
            K9.logDebug( "Sum of all RemindMes: " + remindMes.size());

            for (Iterator<RemindMe> i = remindMes.iterator(); i.hasNext();) {
                RemindMe f = i.next();
                if (f.getRemindTime().before(new Date(timestamp))) {
                    try {
                        localRemindMe.delete(f);
                    } catch (Exception e) {
                        Log.e(K9.LOG_TAG, "Error while removing old RemindMe from Database.");
                    }
                    i.remove();
                }
            }

            K9.logDebug( "Sum of all RemindMeMail after removing expired ones: " +
                    remindMes.size());
            root.setAllRemindMes(remindMes);

        }

        private void synchronizeRemindMeFolder() {
            if(messagingController == null) {
                Log.e(K9.LOG_TAG, "messagingController was null -- folders will not be synchronized.");
                return;
            }

            if(!appendText.isNetworkAvailable()) { //no network connection
                Log.e(K9.LOG_TAG, "No network connection available -- will not synchronize folders.");
                return;
            }

            LocalStore store = null;

            try {
                store = LocalStore.getInstance(mAccount, mContext);
                LocalFolder folder = new LocalFolder(store, mAccount.getRemindMeFolderName());

                if (!folder.exists()) {
                    folder.create(Folder.FolderType.HOLDS_MESSAGES);
                    folder.open(LocalFolder.OPEN_MODE_RO);
                }
            } catch (MessagingException e) {
                Log.e(K9.LOG_TAG, "could not verify that a local RemindME! folder exists", e);
            }

            final CountDownLatch latch = new CountDownLatch(1);
            messagingController.synchronizeMailbox(mAccount, mAccount.getRemindMeFolderName(),
                    new MessagingListener() {
                        @Override
                        public void synchronizeMailboxFinished(Account account, String folder,
                                                               int totalMessagesInMailbox, int numNewMessages) {
                            latch.countDown();
                        }

                        @Override
                        public void synchronizeMailboxFailed(Account account, String folder,
                                                             String message) {
                            latch.countDown();
                        }
                    }, null);

            try {
                //wait for countdown -- suspend after 2s
                latch.await(2000, TimeUnit.MILLISECONDS);
            } catch (Exception e) {
                Log.e(K9.LOG_TAG, "synchronize RemindMe folder failed ", e);
            }

        }

        /**
         * Appends the local file to the server as newest version.
         */
        private void appendUpdatedFile(String newContent) {
            if (newContent == null) {
                Log.i(K9.LOG_TAG, "Will not append new content because String is null.");
                return;
            }
            try {
                K9.logDebug( "New content to append: " + newContent);
                long newTimestamp = appendText.appendNewContent(newContent);
                setLastUpdate(newTimestamp);
            } catch (MessagingException e) {
                Log.e(K9.LOG_TAG, "Failed to append new content from FeatureStorage.");
                return;
            }
        }
    }
}