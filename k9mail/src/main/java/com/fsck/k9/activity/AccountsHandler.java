package com.fsck.k9.activity;

import android.os.Handler;
import android.view.Window;
import android.widget.Toast;

import com.fsck.k9.Account;
import com.fsck.k9.AccountStats;
import com.fsck.k9.BaseAccount;
import com.fsck.k9.K9;
import com.fsck.k9.helper.SizeFormatter;

import java.lang.ref.WeakReference;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import de.fau.cs.mad.smile.android.R;

public class AccountsHandler extends Handler {
    private WeakReference<Accounts> mAccounts;

    public AccountsHandler(Accounts accounts) {
        this.mAccounts = new WeakReference<>(accounts);
    }

    public void refreshTitle() {
        final Accounts accounts = mAccounts.get();
        if(accounts == null) {
            return;
        }

        this.post(new Runnable() {
            public void run() {
                accounts.setViewTitle();
            }
        });
    }

    public void dataChanged() {
        final Accounts accounts = mAccounts.get();
        if(accounts == null) {
            return;
        }

        this.post(new Runnable() {
            public void run() {
                if (accounts.getAdapter() != null) {
                    accounts.getAdapter().notifyDataSetChanged();
                }
            }
        });
    }

    public void workingAccount(final Account account, final int res) {
        final Accounts accounts = mAccounts.get();
        if(accounts == null) {
            return;
        }

        this.post(new Runnable() {
            public void run() {
                String toastText = accounts.getString(res, account.getDescription());

                Toast toast = Toast.makeText(accounts.getApplication(), toastText, Toast.LENGTH_SHORT);
                toast.show();
            }
        });
    }

    public void accountSizeChanged(final Account account, final long oldSize, final long newSize) {
        final Accounts accounts = mAccounts.get();
        if(accounts == null) {
            return;
        }

        this.post(new Runnable() {
            public void run() {
                AccountStats stats = accounts.getAccountStats().get(account.getUuid());
                if (newSize != -1 && stats != null && K9.measureAccounts()) {
                    stats.size = newSize;
                }
                String toastText = accounts.getString(R.string.account_size_changed, account.getDescription(),
                        SizeFormatter.formatSize(accounts.getApplication(), oldSize), SizeFormatter.formatSize(accounts.getApplication(), newSize));

                Toast toast = Toast.makeText(accounts.getApplication(), toastText, Toast.LENGTH_LONG);
                toast.show();
                if (accounts.getAdapter() != null) {
                    accounts.getAdapter().notifyDataSetChanged();
                }
            }
        });
    }

    public void progress(final boolean progress) {
        final Accounts accounts = mAccounts.get();
        if(accounts == null) {
            return;
        }

        // Make sure we don't try this before the menu is initialized
        // this could happen while the activity is initialized.
        if (accounts.getRefreshMenuItem() == null) {
            return;
        }

        this.post(new Runnable() {
            public void run() {
                if (progress) {
                    accounts.getRefreshMenuItem().setActionView(R.layout.actionbar_indeterminate_progress_actionview);
                } else {
                    accounts.getRefreshMenuItem().setActionView(null);
                }
            }
        });

    }

    public void progress(final int progress) {
        final Accounts accounts = mAccounts.get();
        if(accounts == null) {
            return;
        }

       this.post(new Runnable() {
           public void run() {
               accounts.getWindow().setFeatureInt(Window.FEATURE_PROGRESS, progress);
           }
       });
    }

    public void updateAccountStatus(BaseAccount account, AccountStats stats) {
        final Accounts accounts = mAccounts.get();
        if(accounts == null) {
            return;
        }

        final ConcurrentHashMap<String, AccountStats> accountStats = accounts.getAccountStats();
        AccountStats oldStats = accountStats.get(account.getUuid());

        int oldUnreadMessageCount = 0;
        if (oldStats != null) {
            oldUnreadMessageCount = oldStats.unreadMessageCount;
        }

        if (stats == null) {
            stats = new AccountStats(); // empty stats for unavailable accounts
            stats.available = false;
        }

        accountStats.put(account.getUuid(), stats);

        if (account instanceof Account) {
            int newUnreadCount = accounts.getUnreadMessageCount();
            newUnreadCount += stats.unreadMessageCount - oldUnreadMessageCount;
            accounts.setUnreadMessageCount(newUnreadCount);
        }

        dataChanged();

        ConcurrentMap<BaseAccount, String> pendingWork = accounts.getPendingWork();
        pendingWork.remove(account);

        if (pendingWork.isEmpty()) {
            progress(Window.PROGRESS_END);
            refreshTitle();
        } else {
            int adapterCount = accounts.getAdapter().getCount();
            int level = (Window.PROGRESS_END / adapterCount) * (adapterCount - pendingWork.size());
            progress(level);
        }
    }
}
