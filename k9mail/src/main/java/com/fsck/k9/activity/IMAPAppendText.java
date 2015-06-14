package com.fsck.k9.activity;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import com.fsck.k9.Account;
import com.fsck.k9.K9;
import com.fsck.k9.controller.MessagingController;
import com.fsck.k9.controller.MessagingListener;
import com.fsck.k9.mail.Body;
import com.fsck.k9.mail.FetchProfile;
import com.fsck.k9.mail.Folder;
import com.fsck.k9.mail.Message;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.internet.MimeMessage;
import com.fsck.k9.mail.internet.MimeUtility;
import com.fsck.k9.mail.internet.TextBody;
import com.fsck.k9.mailstore.LocalFolder;
import com.fsck.k9.mailstore.LocalMessage;
import com.fsck.k9.mailstore.LocalStore;

import org.apache.commons.io.IOUtils;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class IMAPAppendText{
/* This class handles the storage on the server of the internal data structures which should be
* accessible from different platform.
*
* It uses the IMAP-command 'append' to append a new version.
*
* Use 'getCurrentMessageId()' to check whether the version that you're editing is the newest one.
* If this is not the case, use 'getCurrentContent()' to receive the newest version.
*
* Use 'appendNewContent()' to upload a new version.
* If 'deleteOldContent' is set, all older versions will be deleted from the server, when you upload
* a new version.
*
* */

    private Account mAccount;
    private MimeMessage mimeMessage;
    private MessagingController messagingController;
    private Context mContext;

    private long timestamp = 0;
    private boolean deleteOldContent = true;
    public final static String MESSAGE_ID_MAGIC_STRING = "SmileStorage";

    public IMAPAppendText(Account account, Context context, MessagingController messagingController) {
        this.mAccount = account;
        this.mContext = context;
        this.messagingController = messagingController;
    }

    /**
     *
     * @param deleteOldContent if set, all old versions will be deleted from the folder
     */
    public void setDeleteOldContent(boolean deleteOldContent){
        this.deleteOldContent = deleteOldContent;
    }

    /**
     * @param folder is the name of the new folder in which Smile should store its files
     */
    public void setNewFolder(String folder) {
        //sets new folder in which the content has to be stored
        mAccount.setSmileStorageFolderName(folder);
    }

    /**
     * @return messageID of the newest version (messageID is MESSAGE_ID_MAGIC_STRING + timestamp)
     */
    public String getCurrentMessageId(){
        Message newestMessage = getNewestMessage();
        if (newestMessage == null)
            return null;
        else {
            try {
                return newestMessage.getMessageId();
            } catch (Exception e) {
                return null;
            }
        }
    }

    /**
     * @param messageId of desired message -- null if newest should be returned
     * @return Content of the newest version
     */
    public String getCurrentContent(String messageId) {
        LocalMessage current_message;
        if (messageId == null)
            current_message = getNewestMessage();
        else
            current_message = getLocalMessageByMessageId(messageId);


        if(current_message == null)
            return null;
        else {
            try {
                return streamToString(MimeUtility.decodeBody(current_message.getBody()));
            } catch (Exception e) {
                return null;
            }
        }
    }

    private LocalMessage getLocalMessageByMessageId(String messageId) {
        try{
            LocalStore localStore = mAccount.getLocalStore();
            LocalFolder localFolder = localStore.getFolder(mAccount.getSmileStorageFolderName());
            localFolder.open(Folder.OPEN_MODE_RW);
            LocalMessage localMessage;

            int nMessages = localFolder.getMessageCount();
            if (nMessages == 0) {
                // no messages stored
                return null;
            } else if(nMessages == 1){
                List<? extends LocalMessage> messages = localFolder.getMessages(null, false);
                localMessage = messages.get(0);
                if (!localMessage.getMessageId().equals(messageId))
                    return null;
            } else {
                //more than one, find message id
                List<? extends LocalMessage> messages = localFolder.getMessages(null, false);
                localMessage = null;
                for (LocalMessage msg : messages) {
                    try {
                        if (msg.getMessageId().equals(messageId)) {
                            localMessage = msg;
                            break;
                        }
                    } catch (Exception e) {
                        //getMessageId may return Null if no MessageId is set -- ignore message
                        continue;
                    }
                }
            }
            FetchProfile fp = new FetchProfile();
            fp.add(FetchProfile.Item.ENVELOPE);
            fp.add(FetchProfile.Item.BODY);
            localFolder.fetch(Collections.singletonList(localMessage), fp, null);
            localFolder.close();

            return localMessage;

        } catch (Exception e) {
            return null;
        }
    }

    private LocalMessage getNewestMessage() {
        //first synchronize server and local storage
        synchronizeFolder();

        try{
            LocalStore localStore = mAccount.getLocalStore();
            LocalFolder localFolder = localStore.getFolder(mAccount.getSmileStorageFolderName());
            localFolder.open(Folder.OPEN_MODE_RW);
            LocalMessage localMessage;
            int nMessages = localFolder.getMessageCount();
            if (nMessages == 0) {
                // no messages stored
                return null;
            } else if(nMessages == 1){
                List<? extends LocalMessage> messages = localFolder.getMessages(null, false);
                localMessage = messages.get(0);
                if(!localMessage.getMessageId().startsWith(MESSAGE_ID_MAGIC_STRING))
                    return null;
            } else {
                //more than one, find newest
                List<? extends LocalMessage> messages = localFolder.getMessages(null, false);
                localMessage = messages.get(0);
                for (LocalMessage msg : messages) {
                    try {
                        if(!msg.getMessageId().startsWith(MESSAGE_ID_MAGIC_STRING))
                            continue;
                        if (Long.parseLong(msg.getMessageId().replace(
                                MESSAGE_ID_MAGIC_STRING, "")) >
                                Long.parseLong(localMessage.getMessageId().replace(
                                        MESSAGE_ID_MAGIC_STRING, ""))) {
                            localMessage = msg;
                        }
                    } catch (Exception e) {
                        continue;
                    }

                }
            }
            FetchProfile fp = new FetchProfile();
            fp.add(FetchProfile.Item.ENVELOPE);
            fp.add(FetchProfile.Item.BODY);
            localFolder.fetch(Collections.singletonList(localMessage), fp, null);
            localFolder.close();
            return localMessage;

        } catch (Exception e) {
            return null;
        }
    }

    private void synchronizeFolder(){
        if(messagingController == null) {
            Log.e(K9.LOG_TAG, "messagingController was null -- folders will not be synchronized.");
            return;
        }

        if(!isNetworkAvailable()) { //no network connection
            Log.e(K9.LOG_TAG, "No network connection available -- will not synchronize folders.");
            return;
        }
        final CountDownLatch latch = new CountDownLatch(1);
        messagingController.synchronizeMailbox(mAccount, mAccount.getSmileStorageFolderName(),
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
            //wait for countdown -- suspend after 5s
            latch.await(5000, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
        }
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    private static String streamToString(InputStream stream) throws Exception {
        return IOUtils.toString(stream, "ISO-8859-1");
    }

    /**
     * @param new_content containing the new content to be stored
     * @return timestamp of new message
     */
    public long appendNewContent(String new_content) throws MessagingException {
        /* same like appendNewMimeMessage() but with a String as parameter containing content;
        sets new messageID. */

        if(new_content == null)
            return -1;

        MimeMessage newMimeMessage = new MimeMessage();

        //create messageID (magic string + timestamp)
        timestamp = System.currentTimeMillis();
        String messageId = MESSAGE_ID_MAGIC_STRING + String.valueOf(timestamp);

        //other stuff
        newMimeMessage.setHeader("MIME-Version", "1.0");
        newMimeMessage.setSentDate(new Date(timestamp), false);
        newMimeMessage.setSubject("Internal from Smile");
        newMimeMessage.setEncoding("quoted-printable");

        //set messageID and body
        newMimeMessage.setMessageId(messageId);

        Body b = new TextBody(new_content);
        newMimeMessage.setBody(b);

        appendNewMimeMessage(newMimeMessage);
        return timestamp;
    }

    /**
     * @param mimeMessage is the full mimeMessage to be stored
     */
    public void appendNewMimeMessage(MimeMessage mimeMessage) {
        /*same like appendNewContent() but with full MimeMessage containing content and new messageID. */
        if(messagingController == null) {
            Log.e(K9.LOG_TAG, "messagingController was null -- no message will be appended.");
            return;
        }

        this.mimeMessage = mimeMessage;
        //append message
        messagingController.saveSmileStorageMessage(mAccount, mimeMessage);
        //synchronize folder
        messagingController.synchronizeMailbox(mAccount, mAccount.getSmileStorageFolderName(), null, null);
        //delete old messages
        if(deleteOldContent)
            deleteOldMessages();
    }

    private void deleteOldMessages() {
        try {
            LocalStore localStore = mAccount.getLocalStore();
            LocalFolder localFolder = localStore.getFolder(mAccount.getSmileStorageFolderName());
            localFolder.open(Folder.OPEN_MODE_RW);
            List<LocalMessage> messages = localFolder.getMessages(null, false);
            List<LocalMessage> deleteMessagesList = new ArrayList<LocalMessage>();

            int nMessages = localFolder.getMessageCount();
            if (nMessages == 0) {
                // no messages stored, normally this should not happen
                return;
            } else if(nMessages == 1){
                try {
                    if (messages.get(0).getMessageId().equals(
                            MESSAGE_ID_MAGIC_STRING + String.valueOf(timestamp)))
                        return;
                    else if (Long.parseLong(messages.get(0).getMessageId().replace(
                            MESSAGE_ID_MAGIC_STRING, "")) < timestamp) {
                        //delete older message
                        deleteMessagesList.add(messages.get(0));
                    } else {
                        // one message stored, but it is not ours -- ignore message
                        return;
                    }
                } catch (Exception e) {
                        //getMessageId may return Null if no MessageID is set -- ignore message
                        return;
                }
            } else {
                //more than one, delete all older messages
                for (LocalMessage msg : messages) {
                    try {
                        if (Long.parseLong(msg.getMessageId().replace(
                                MESSAGE_ID_MAGIC_STRING, "")) < timestamp) {
                            //delete all older messages
                            deleteMessagesList.add(msg);
                        }
                    } catch (Exception e) {
                        // getMessageId may return Null if no MessageId is set -- ignore message
                        continue;
                    }
                }
            }
            //move old messages to trash
            messagingController.deleteMessages(deleteMessagesList, null);
            //synchronize folder (SmileStorage)
            synchronizeFolder();
            //close local folder
            localFolder.close();
            //remove deleted messages from Trash folder
            messagingController.deleteFromTrash(mAccount, null);
        } catch (MessagingException e) {
        }
    }
}