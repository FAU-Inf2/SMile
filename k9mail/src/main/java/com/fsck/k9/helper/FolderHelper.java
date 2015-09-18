package com.fsck.k9.helper;

import android.content.Context;
import android.util.Log;

import com.fsck.k9.Account;
import com.fsck.k9.K9;
import com.fsck.k9.holder.FolderInfoHolder;
import com.fsck.k9.mail.Folder;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mailstore.LocalFolder;
import com.fsck.k9.mailstore.LocalStore;

import de.fau.cs.mad.smile.android.R;

public class FolderHelper {
    public static FolderInfoHolder getFolder(Context context, String folder, Account account) {
        LocalFolder localFolder = null;
        try {
            LocalStore localStore = account.getLocalStore();
            localFolder = localStore.getFolder(folder);
            return new FolderInfoHolder(context, localFolder, account);
        } catch (Exception e) {
            Log.e(K9.LOG_TAG, "getFolder(" + folder + ") goes boom: ", e);
            return null;
        } finally {
            if (localFolder != null) {
                localFolder.close();
            }
        }
    }

    public static String getFolderNameById(Account account, long folderId) {
        Folder folder = getFolderById(account, folderId);
        if (folder != null) {
            return folder.getName();
        }
        return null;
    }

    public static LocalFolder getFolderById(Account account, long folderId) {
        try {
            LocalStore localStore = account.getLocalStore();
            LocalFolder localFolder = localStore.getFolderById(folderId);
            localFolder.open(Folder.OPEN_MODE_RO);
            return localFolder;
        } catch (MessagingException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Returns the display name for a folder.
     *
     * <p>
     * This will return localized strings for special folders like the Inbox or the Trash folder.
     * </p>
     *
     * @param context
     *         A {@link Context} instance that is used to get the string resources.
     * @param account
     *         The {@link Account} the folder belongs to.
     * @param name
     *         The name of the folder for which to return the display name.
     *
     * @return The localized name for the provided folder if it's a special folder or the original
     *         folder name if it's a non-special folder.
     */
    public static String getDisplayName(Context context, Account account, String name) {
        final String displayName;
        if (name.equals(account.getSpamFolderName())) {
            displayName = String.format(
                    context.getString(R.string.special_mailbox_name_spam_fmt), name);
        } else if (name.equals(account.getArchiveFolderName())) {
            displayName = String.format(
                    context.getString(R.string.special_mailbox_name_archive_fmt), name);
        } else if (name.equals(account.getSentFolderName())) {
            displayName = String.format(
                    context.getString(R.string.special_mailbox_name_sent_fmt), name);
        } else if (name.equals(account.getTrashFolderName())) {
            displayName = String.format(
                    context.getString(R.string.special_mailbox_name_trash_fmt), name);
        } else if (name.equals(account.getDraftsFolderName())) {
            displayName = String.format(
                    context.getString(R.string.special_mailbox_name_drafts_fmt), name);
        } else if (name.equals(account.getOutboxFolderName())) {
            displayName = context.getString(R.string.special_mailbox_name_outbox);
            // FIXME: We really shouldn't do a case-insensitive comparison here
        } else if (name.equalsIgnoreCase(account.getInboxFolderName())) {
            displayName = context.getString(R.string.special_mailbox_name_inbox);
        } else {
            displayName = name;
        }

        return displayName;
    }
}
