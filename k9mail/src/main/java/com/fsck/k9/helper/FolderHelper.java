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
}
