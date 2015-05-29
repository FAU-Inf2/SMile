package com.fsck.k9.activity;

import android.os.AsyncTask;

import com.fsck.k9.Account;
import com.fsck.k9.controller.MessagingController;
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
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class IMAPAppendText extends K9Activity {
/* Use IMAP-command "append" to upload a text to the server.*/
/* TODO:
* 1. get_current_uid [works]
* *1a get newest version --> check if local uid is same as uid on server (caller has to check; not in this class)
* *1b download newest version (here [works]) and update content locally (caller)
* 2. upload new version with new uid [works]
* 3. delete old one from server. [not implemented yet]
**
* */

    private Account mAccount;
    private MimeMessage mimeMessage;
    private MessagingController messagingController;

    private long timestamp = 0;
    private final static String MESSAGE_ID_MAGIC_STRING = "SmileStorage";

    public IMAPAppendText(Account account) {
        this.mAccount = account;
    }

    /**
     * @return messageID of the newest version (messageID is MESSAGE_ID_MAGIC_STRING + timestamp)
     */
    public String getCurrentMessageID(){
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
     * @param messageID of desired message -- null if newest should be returned
     * @return Content of the newest version
     */
    public String getCurrentContent(String messageID) {
        LocalMessage current_message;
        if (messageID == null)
            current_message = getNewestMessage();
        else
            current_message = getLocalMessageByMessageId(messageID);


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
                    if (msg.getMessageId().equals(messageId)) {
                        localMessage = msg;
                        break;
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
        //TODO: Update localStore first?

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
            } else {
                //more than one, find newest
                List<? extends LocalMessage> messages = localFolder.getMessages(null, false);
                localMessage = messages.get(0);
                for (LocalMessage msg : messages) {
                    if (Long.parseLong(msg.getMessageId().replace(MESSAGE_ID_MAGIC_STRING, "")) >
                            Long.parseLong(localMessage.getMessageId().replace(MESSAGE_ID_MAGIC_STRING, ""))) {
                        localMessage = msg;
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

    private static String streamToString(InputStream stream) throws Exception {
        return IOUtils.toString(stream, "ISO-8859-1");
    }

    /**
     * @param new_content containing the new content to be stored
     */
    public void appendNewContent(String new_content) throws MessagingException {
        /* same like appendNewMimeMessage() but with a String as parameter containing content;
        sets new messageID. */

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
        new_content += " -- messageID is " + newMimeMessage.getMessageId() + "."; //just for testing

        Body b = new TextBody(new_content);
        newMimeMessage.setBody(b);

        appendNewMimeMessage(newMimeMessage);
    }

    /**
     * @param mimeMessage is the full mimeMessage to be stored
     */
    public void appendNewMimeMessage(MimeMessage mimeMessage) {
        /*same like appendNewContent() but with full MimeMessage containing content and new messageID. */

        if(messagingController == null)
            messagingController = MessagingController.getInstance(getApplication());

        this.mimeMessage = mimeMessage;
        saveMessage();
    }

    /**
     * @param folder is the name of the new folder in which Smile should store its files
     */
    public void setNewFolder(String folder) {
        //sets new folder in which the content has to be stored
        mAccount.setSmileStorageFolderName(folder);
    }

    private void saveMessage() {
        new SaveMessageTask().execute();
    }

    /* see MessageCompose */
    private class SaveMessageTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... params) {
            Message smileStoreMessage = messagingController.saveSmileStorageMessage(mAccount, mimeMessage);
            return null;
        }
    }

}