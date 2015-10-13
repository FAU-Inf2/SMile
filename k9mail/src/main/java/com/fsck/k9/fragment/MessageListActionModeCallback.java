package com.fsck.k9.fragment;

import android.support.v7.view.ActionMode;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.fsck.k9.Account;
import com.fsck.k9.K9;
import com.fsck.k9.Preferences;
import com.fsck.k9.controller.MessagingController;
import com.fsck.k9.mail.Flag;
import com.fsck.k9.mailstore.LocalMessage;

import java.lang.ref.WeakReference;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import de.fau.cs.mad.smile.android.R;

public class MessageListActionModeCallback implements ActionMode.Callback {
    private MenuItem mSelectAll;
    private MenuItem mMarkAsRead;
    private MenuItem mMarkAsUnread;
    private MenuItem mFlag;
    private MenuItem mUnflag;
    private final Account mAccount;
    private final Preferences mPreferences;
    private final boolean mSingleAccountMode;
    private final WeakReference<MessageListFragment> messageListFragmentWeakReference;
    private final MessagingController mController;

    public MessageListActionModeCallback(Account mAccount, boolean mSingleAccountMode, MessageListFragment messageListFragment) {
        this.mAccount = mAccount;
        this.mPreferences = Preferences.getPreferences(K9.getApplication());
        this.mSingleAccountMode = mSingleAccountMode;
        this.messageListFragmentWeakReference = new WeakReference<>(messageListFragment);
        mController = MessagingController.getInstance(messageListFragment.getContext());
    }

    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        MenuInflater inflater = mode.getMenuInflater();
        inflater.inflate(R.menu.message_list_context, menu);

        // check capabilities
        setContextCapabilities(mAccount, menu);

        return true;
    }

    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        mSelectAll = menu.findItem(R.id.select_all);
        mMarkAsRead = menu.findItem(R.id.mark_as_read);
        mMarkAsUnread = menu.findItem(R.id.mark_as_unread);
        mFlag = menu.findItem(R.id.flag);
        mUnflag = menu.findItem(R.id.unflag);

        // we don't support cross account actions atm
        if (!mSingleAccountMode) {
            // show all
            menu.findItem(R.id.move).setVisible(true);
            menu.findItem(R.id.archive).setVisible(true);
            menu.findItem(R.id.spam).setVisible(true);
            menu.findItem(R.id.copy).setVisible(true);

            Set<String> accountUuids = getAccountUuidsForSelected();

            for (String accountUuid : accountUuids) {
                Account account = mPreferences.getAccount(accountUuid);
                if (account != null) {
                    setContextCapabilities(account, menu);
                }
            }

        }
        return true;
    }

    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
        /*
         * In the following we assume that we can't move or copy
         * mails to the same folder. Also that spam isn't available if we are
         * in the spam folder,same for archive.
         *
         * This is the case currently so safe assumption.
         */
        MessageListFragment fragment = messageListFragmentWeakReference.get();
        if(fragment == null) {
            return true;
        }

        List<LocalMessage> checkedMessages = fragment.getCheckedMessages();
        switch (item.getItemId()) {
            case R.id.delete: {
                fragment.onDelete(checkedMessages);
                fragment.setSelectedCount(0);
                break;
            }
            case R.id.mark_as_read: {
                fragment.setFlagForSelected(Flag.SEEN, true);
                break;
            }
            case R.id.mark_as_unread: {
                fragment.setFlagForSelected(Flag.SEEN, false);
                break;
            }
            case R.id.flag: {
                fragment.setFlagForSelected(Flag.FLAGGED, true);
                break;
            }
            case R.id.unflag: {
                fragment.setFlagForSelected(Flag.FLAGGED, false);
                break;
            }
            case R.id.select_all: {
                fragment.selectAll();
                break;
            }

            // only if the account supports this
            case R.id.archive: {
                fragment.onArchive(checkedMessages);
                fragment.setSelectedCount(0);
                break;
            }
            case R.id.spam: {
                fragment.onSpam(checkedMessages);
                fragment.setSelectedCount(0);
                break;
            }
            case R.id.move: {
                fragment.onMove(checkedMessages);
                fragment.setSelectedCount(0);
                break;
            }
            case R.id.copy: {
                fragment.onCopy(checkedMessages);
                fragment.setSelectedCount(0);
                break;
            }
        }

        if (fragment.getSelectedCount() == 0) {
            mode.finish();
        }

        return true;
    }

    @Override
    public void onDestroyActionMode(ActionMode mode) {
        mSelectAll = null;
        mMarkAsRead = null;
        mMarkAsUnread = null;
        mFlag = null;
        mUnflag = null;
        //setSelectionState(false);
    }

    /**
     * Get the set of account UUIDs for the selected messages.
     */
    private Set<String> getAccountUuidsForSelected() {
        MessageListFragment fragment = messageListFragmentWeakReference.get();
        if(fragment == null) {
            return null;
        }

        List<LocalMessage> checkedMessages = fragment.getCheckedMessages();
        Set<String> accountUuids = new HashSet<>();

        for(LocalMessage message : checkedMessages) {
            accountUuids.add(message.getAccount().getUuid());
        }

        return accountUuids;
    }

    /**
     * Disables menu options not supported by the account type or current "search view".
     *
     * @param account The account to query for its capabilities.
     * @param menu    The menu to adapt.
     */
    private void setContextCapabilities(Account account, Menu menu) {
        if (!mSingleAccountMode) {
            // We don't support cross-account copy/move operations right now
            menu.findItem(R.id.move).setVisible(false);
            menu.findItem(R.id.copy).setVisible(false);

            //TODO: we could support the archive and spam operations if all selected messages
            // belong to non-POP3 accounts
            menu.findItem(R.id.archive).setVisible(false);
            menu.findItem(R.id.spam).setVisible(false);

        } else {
            // hide unsupported
            if (!mController.isCopyCapable(account)) {
                menu.findItem(R.id.copy).setVisible(false);
            }

            if (!mController.isMoveCapable(account)) {
                menu.findItem(R.id.move).setVisible(false);
                menu.findItem(R.id.archive).setVisible(false);
                menu.findItem(R.id.spam).setVisible(false);
            }

            if (!account.hasArchiveFolder()) {
                menu.findItem(R.id.archive).setVisible(false);
            }

            if (!account.hasSpamFolder()) {
                menu.findItem(R.id.spam).setVisible(false);
            }
        }
    }

    public void showSelectAll(boolean show) {
        if(mSelectAll != null) {
            mSelectAll.setVisible(show);
        }
    }

    public void showMarkAsRead(boolean show) {
        if(mMarkAsRead != null) {
            mMarkAsRead.setVisible(show);
            mMarkAsUnread.setVisible(!show);
        }
    }

    public void showFlag(boolean show) {
        if(mFlag != null) {
            mFlag.setVisible(show);
            mUnflag.setVisible(!show);
        }
    }
}
