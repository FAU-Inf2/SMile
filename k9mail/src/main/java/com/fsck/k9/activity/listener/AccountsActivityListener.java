package com.fsck.k9.activity.listener;

import android.content.Context;
import android.util.Log;

import com.fsck.k9.Account;
import com.fsck.k9.AccountStats;
import com.fsck.k9.BaseAccount;
import com.fsck.k9.K9;
import com.fsck.k9.activity.AccountsHandler;
import com.fsck.k9.controller.MessagingController;

public class AccountsActivityListener extends ActivityListener {
    private final AccountsHandler mHandler;
    private final Context mContext;

    public AccountsActivityListener(Context context, AccountsHandler mHandler) {
        this.mContext = context;
        this.mHandler = mHandler;
    }

    @Override
    public void informUserOfStatus() {
        mHandler.refreshTitle();
    }

    @Override
    public void folderStatusChanged(Account account, String folderName, int unreadMessageCount) {
        try {
            AccountStats stats = account.getStats(mContext);
            if (stats == null) {
                Log.w(K9.LOG_TAG, "Unable to get account stats");
            } else {
                accountStatusChanged(account, stats);
            }
        } catch (Exception e) {
            Log.e(K9.LOG_TAG, "Unable to get account stats", e);
        }
    }

    @Override
    public void accountStatusChanged(BaseAccount account, AccountStats stats) {
        mHandler.updateAccountStatus(account, stats);
    }

    @Override
    public void accountSizeChanged(Account account, long oldSize, long newSize) {
        mHandler.accountSizeChanged(account, oldSize, newSize);
    }

    @Override
    public void synchronizeMailboxFinished(
            Account account,
            String folder,
            int totalMessagesInMailbox,
            int numNewMessages) {
        MessagingController.getInstance(mContext).getAccountStats(mContext, account, this);
        super.synchronizeMailboxFinished(account, folder, totalMessagesInMailbox, numNewMessages);
        mHandler.progress(false);
    }

    @Override
    public void synchronizeMailboxStarted(Account account, String folder) {
        super.synchronizeMailboxStarted(account, folder);
        mHandler.progress(true);
    }

    @Override
    public void synchronizeMailboxFailed(Account account, String folder,
                                         String message) {
        super.synchronizeMailboxFailed(account, folder, message);
        mHandler.progress(false);

    }
}
