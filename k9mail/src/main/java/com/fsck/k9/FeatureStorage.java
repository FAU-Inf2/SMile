package com.fsck.k9;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.fsck.k9.activity.IMAPAppendText;
import com.fsck.k9.controller.MessagingController;
import com.fsck.k9.mail.RemindMe;
import com.fsck.k9.mail.Message;
import com.fsck.k9.mail.MessagingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fsck.k9.mailstore.LocalRemindMe;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

public class FeatureStorage {
    /* Handles saving and parsing information from our features (e.g. follow-up) to the
    * file on the server for cross-platform functionality.*/

    private Account mAccount;
    private IMAPAppendText appendText;
    private LocalRemindMe localRemindMe;
    private static ObjectMapper objectMapper;
    private static File localFile;
    private static long lastUpdate = -1; // last time when the local file was parsed
    private String absolutePath; // absolute path to local files

    public FeatureStorage(Account mAccount, Context mContext, MessagingController
            messagingController, String absolutePath) {
        this.mAccount = mAccount;
        this.appendText = new IMAPAppendText(mAccount, mContext, messagingController);
        this.absolutePath = absolutePath + "/";
        try {
            this.localRemindMe = new LocalRemindMe(mAccount.getLocalStore());
        } catch (Exception e) {}
    }

    public void pollService() {
        new UpdateAddSaveManager().execute();
    }

    private class UpdateAddSaveManager extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... params) {
            if (localFile == null)
                localFile = new File(absolutePath + "smilestorage.json");
            if (!localFile.exists()) {
                try {
                    lastUpdate = -1;
                    Log.d(K9.LOG_TAG, "smilestorage.json does not exist -- create file.");
                    localFile.createNewFile();
                } catch (IOException e) {
                    Log.e(K9.LOG_TAG, "Error while creating local file." + e.getMessage());
                    return null;
                }
            }

            try {
                if(objectMapper == null)
                    objectMapper = new ObjectMapper();

                // Is external version newer than the version which is saved locally?
                String newerMessageId = hasUpdate();
                if(newerMessageId != null)
                    mergeLocalExternalVersion(newerMessageId); // Merge external and local version

                // update/add/delete elements
                String newContent = createUpdatedFile();

                // save new version on server
                appendUpdatedFile(newContent);

            } catch (Exception e) {
                Log.e(K9.LOG_TAG, "Error in jackson mapper:" + e.toString() + e.getMessage());
            }

            return null;
        }

        /**
         * Checks whether file on server is newer than local version
         * @return MessageId of newest version or null if local version is the newest one
         */
        private String hasUpdate() { //external file has changed
            Log.d(K9.LOG_TAG, "Check whether there is a newer version of smilestorage.json on"+
                    " the mailserver.");

            String currentMessageId = appendText.getCurrentMessageId();
            Log.d(K9.LOG_TAG, "Latest local version is from: " + lastUpdate);
            Log.d(K9.LOG_TAG, "Current MessageId from server: " + currentMessageId);
            if (currentMessageId == null)
                return null;

            try {
                if (Long.parseLong(
                        currentMessageId.replace(appendText.MESSAGE_ID_MAGIC_STRING, "")) >
                        lastUpdate)  //newer version available
                    return currentMessageId;
            } catch (Exception e) {}
            return null;
        }

        /**
         * Merges the new version with the local one and saves it locally.
         */
        private void mergeLocalExternalVersion(String newerMessageId) {
            Log.d(K9.LOG_TAG, "External version of smilestorage.json is newer -- merge files.");

            String currentContent = appendText.getCurrentContent(newerMessageId);

            SmileFeaturesJsonRoot externalRoot;
            SmileFeaturesJsonRoot internalRoot;
            try{
                try {
                    externalRoot = objectMapper.readValue(currentContent,
                            SmileFeaturesJsonRoot.class);
                } catch (Exception e) {
                    // currentContent was not valid or was null -- nothing to merge
                    Log.d(K9.LOG_TAG, "External version was empty/invalid -- nothing to merge.");
                    lastUpdate = Long.parseLong(newerMessageId.replace(
                            appendText.MESSAGE_ID_MAGIC_STRING, ""));
                    return;
                }
                try {
                    internalRoot = objectMapper.readValue(localFile, SmileFeaturesJsonRoot.class);
                } catch (Exception e) {
                    // local file was empty -- save external version
                    Log.d(K9.LOG_TAG, "Local version was empty/invalid -- save external "
                            + "version: " + currentContent);
                    externalRoot.setAllRemindMes(mergeFollowUps(externalRoot.getAllRemindMes(),
                            null));
                    objectMapper.writeValue(localFile, externalRoot);
                    lastUpdate = Long.parseLong(newerMessageId.replace(
                            appendText.MESSAGE_ID_MAGIC_STRING, ""));
                    return;
                }

                //Merge FollowUps
                try {
                    List<RemindMe> allRemindMes = new ArrayList<RemindMe>();
                    try {
                        allRemindMes = localRemindMe.getAllFollowUps();
                        for(RemindMe f : allRemindMes) {
                            if(f.getReference() == null)
                                continue;
                            try {
                                f.setMessageId(f.getReference().getMessageId());
                            } catch (Exception e) {}
                            f.setUid(f.getReference().getUid());
                        }
                    } catch (Exception e) {
                        Log.e(K9.LOG_TAG, "Could not get all FollowUps from db: " + e.getMessage());
                    }
                    internalRoot.setAllRemindMes(mergeFollowUps(externalRoot.getAllRemindMes(),
                            allRemindMes));
                } catch (Exception e){
                    Log.e(K9.LOG_TAG, "Exception while adding allFollowUps to root: " + e.getMessage());
                }

                Log.d(K9.LOG_TAG, "New version from mergeLocalExternalVersion(): " + objectMapper.
                        writerWithDefaultPrettyPrinter().writeValueAsString(internalRoot));
                objectMapper.writeValue(localFile, internalRoot);
                lastUpdate = Long.parseLong(newerMessageId.replace(
                        appendText.MESSAGE_ID_MAGIC_STRING, ""));

                } catch (IOException e) {
                    Log.e(K9.LOG_TAG, "Error in jackson mapper while writing to file: " +
                            e.getMessage());
                }
        }

        /**
         * Adds the new FollowUpMailInformation to the file, removes expired elements and
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
                        allRemindMes = localRemindMe.getAllFollowUps();
                    } catch (Exception e) {
                        Log.e(K9.LOG_TAG, "Could not get all FollowUps from db: " + e.getMessage());
                    }
                    root.setAllRemindMes(allRemindMes);
                } catch (Exception e){
                    Log.e(K9.LOG_TAG, "Exception while adding allFollowUps to root: " + e.getMessage());
                }

                // remove expired nodes
                removePastNodes(root);

                Log.d(K9.LOG_TAG, "New version from createUpdatedFile(): " + objectMapper.
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

        private List<RemindMe> mergeFollowUps(List<RemindMe> fileRemindMes, List<RemindMe> dbRemindMes) {
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
                                //TODO: set new timer?
                                Log.d(K9.LOG_TAG, "Existing RemindMe was updated -- update in local database.");
                                localRemindMe.update(f);
                            }

                            found = true;
                            break;
                        }
                    } catch (Exception e) {}
                }
                if (found)
                    continue;
                try {
                    Log.d(K9.LOG_TAG, "New RemindMe from server version -- add to local database.");
                    String []uids = {f.getUid()};
                    Message m = mAccount.getLocalStore().getFolderById(f.getFolderId()).getMessages(uids, null).get(0);
                    f.setReference(m);
                    //TODO: set timer?
                    localRemindMe.add(f);
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
            Log.d(K9.LOG_TAG, "Sum of all FollowUps: " + remindMes.size());
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
            Log.d(K9.LOG_TAG, "Sum of all FollowUpMail after removing expired ones: " +
                    remindMes.size());
            root.setAllRemindMes(remindMes);

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
                Log.d(K9.LOG_TAG, "New content to append: " + newContent);
                long newTimestamp = appendText.appendNewContent(newContent);
                lastUpdate = newTimestamp;
            } catch (MessagingException e) {
                Log.e(K9.LOG_TAG, "Failed to append new content from FeatureStorage.");
                return;
            }
        }
    }
}