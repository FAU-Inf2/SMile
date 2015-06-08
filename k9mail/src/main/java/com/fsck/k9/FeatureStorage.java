package com.fsck.k9;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.fsck.k9.activity.IMAPAppendText;
import com.fsck.k9.controller.MessagingController;
import com.fsck.k9.mail.MessagingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

public class FeatureStorage {
    /* Handles saving and parsing information from our features (e.g. follow-up) to the
    * file on the server for cross-platform functionality.*/

    private Account mAccount;
    private IMAPAppendText appendText;
    private static ObjectMapper objectMapper;
    private static File localFile;
    private static long lastUpdate = -1; // last time when the local file was parsed
    private String absolutePath; // absolute path to local files
    private FollowUpMailInformation newFollowUpMail;

    public FeatureStorage(Account mAccount, Context mContext, MessagingController
            messagingController, String absolutePath) {
        this.mAccount = mAccount;
        this.appendText = new IMAPAppendText(mAccount, mContext, messagingController);
        this.absolutePath = absolutePath + "/";
    }

    /**
     * User wants a notification for a certain mail; save information in local file and on mail
     * server.
     * @param uid uid of the mail TODO: is this necessary?
     * @param messageId messageId of the mail
     * @param timestamp timestamp of time when there should be a notification (follow up)
     */
    public void saveNewFollowUpMailInformation(String uid, String messageId, Long timestamp) {
        newFollowUpMail = new FollowUpMailInformation(mAccount.getEmail(), uid, messageId,
                timestamp);
        new UpdateAddSaveManager().execute();
    }

    /**
     * User wants a notification for a multiple mails; save information in local file and on mail
     * server.
     * @param uids uids of the mails TODO: is this necessary?
     * @param messageIds messageIds of all mails
     * @param timestamp timestamp of time when there should be a notification (follow up)
     */
    public void saveNewFollowUpMailsInformation(List<String> uids, List<String> messageIds,
                                                Long timestamp) {

        //TODO: implement...
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
                    //TODO: return? abort?
                }
            }

            try {
                objectMapper = new ObjectMapper();

                Log.d(K9.LOG_TAG, "New object to add: \n" +
                        objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(
                                newFollowUpMail));


                String newerMessageId = hasUpdate();
                if(newerMessageId != null)
                    updateLocalStorage(newerMessageId);

                String newContent = createUpdatedFile();
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
        private void updateLocalStorage(String newerMessageId) {
            Log.d(K9.LOG_TAG, "External version of smilestorage.json is newer -- merge files.");

            String currentContent = appendText.getCurrentContent(newerMessageId);

            SmileFeaturesJsonRoot externalRoot;
            SmileFeaturesJsonRoot internalRoot;
            //TODO: this is merging -- but I want to install new notifications too...
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
                    objectMapper.writeValue(localFile, externalRoot);
                    lastUpdate = Long.parseLong(newerMessageId.replace(
                            appendText.MESSAGE_ID_MAGIC_STRING, ""));
                    return;
                }
                internalRoot.mergeAllFollowUpMails(externalRoot.getAllFollowUpMails());
                Log.d(K9.LOG_TAG, "New version from updateLocalStorage(): " + objectMapper.
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
                    root.addFollowUpMailInformation(newFollowUpMail);
                } catch (Exception e) {
                    //local file may be empty --> new file contains only newFollowUpMail
                    Log.e(K9.LOG_TAG, "Error while reading local file, create new root.");
                    root = new SmileFeaturesJsonRoot();
                    root.addFollowUpMailInformation(newFollowUpMail);
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

        private void removePastNodes(SmileFeaturesJsonRoot root) {
            //remove nodes where there was already an notification
            long timestamp = System.currentTimeMillis();
            //go through and remove old nodes
            HashSet<FollowUpMailInformation> hashSet = root.getAllFollowUpMails();
            Log.d(K9.LOG_TAG, "Sum of all FollowUpMailInformation: " + hashSet.size());

            for (Iterator<FollowUpMailInformation> i = hashSet.iterator(); i.hasNext();) {
                FollowUpMailInformation f = i.next();
                if(f.getNotificationTimestamp() < timestamp)
                    i.remove();
            }
            Log.d(K9.LOG_TAG, "Sum of all FollowUpMailInformation after removing expired ones: " +
                    hashSet.size());

            root.setAllFollowUpMails(hashSet);
        }

        /**
         * Appends the local file to the server as newest version.
         */
        private void appendUpdatedFile(String newContent) {
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