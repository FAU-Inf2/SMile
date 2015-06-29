package com.fsck.k9.activity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import android.app.ActionBar;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import com.fsck.k9.Account;
import com.fsck.k9.Account.FolderMode;
import com.fsck.k9.AccountStats;
import com.fsck.k9.BaseAccount;
import com.fsck.k9.K9;
import com.fsck.k9.Preferences;

import de.fau.cs.mad.smile.android.R;

import com.fsck.k9.activity.holder.FolderInfoHolder;
import com.fsck.k9.activity.setup.AccountSettings;
import com.fsck.k9.activity.setup.FolderSettings;
import com.fsck.k9.activity.setup.Prefs;
import com.fsck.k9.controller.MessagingController;
import com.fsck.k9.controller.MessagingListener;
import com.fsck.k9.helper.SizeFormatter;
import com.fsck.k9.mail.power.TracingPowerManager;
import com.fsck.k9.mail.power.TracingPowerManager.TracingWakeLock;
import com.fsck.k9.mail.Folder;
import com.fsck.k9.mail.Message;
import com.fsck.k9.mailstore.LocalFolder;
import com.fsck.k9.search.LocalSearch;
import com.fsck.k9.service.MailService;

import de.cketti.library.changelog.ChangeLog;

/**
 * FolderList is the primary user interface for the program. This
 * Activity shows list of the Account's folders
 */

public class FolderList extends K9ListActivity {
    private static final String EXTRA_ACCOUNT = "account";
    private static final String EXTRA_FROM_SHORTCUT = "fromShortcut";
    private static final boolean REFRESH_REMOTE = true;

    private ListView mListView;
    private FolderListAdapter mAdapter;
    private FolderListActivityListener mListener;
    private Account mAccount;
    private int mUnreadMessageCount;

    private MenuItem mRefreshMenuItem;
    private View mActionBarProgressView;
    private ActionBar mActionBar;

    private TextView mActionBarTitle;
    private TextView mActionBarSubTitle;
    private TextView mActionBarUnread;

    public static Intent actionHandleAccountIntent(Context context, Account account, boolean fromShortcut) {
        Intent intent = new Intent(context, FolderList.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra(EXTRA_ACCOUNT, account.getUuid());

        if (fromShortcut) {
            intent.putExtra(EXTRA_FROM_SHORTCUT, true);
        }

        return intent;
    }

    public static void actionHandleAccount(Context context, Account account) {
        Intent intent = actionHandleAccountIntent(context, account, false);
        context.startActivity(intent);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (UpgradeDatabases.actionUpgradeDatabases(this, getIntent())) {
            finish();
            return;
        }

        mActionBarProgressView = getLayoutInflater().inflate(R.layout.actionbar_indeterminate_progress_actionview, null);
        mActionBar = getActionBar();
        initializeActionBar();

        setContentView(R.layout.folder_list);
        mListView = getListView();
        mListView.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
        mListView.setLongClickable(true);
        mListView.setFastScrollEnabled(true);
        mListView.setScrollingCacheEnabled(false);
        mListView.setOnItemClickListener(new OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                onOpenFolder(((FolderInfoHolder) mAdapter.getItem(position)).name);
            }
        });
        registerForContextMenu(mListView);

        mListView.setSaveEnabled(true);

        onNewIntent(getIntent());
        if (isFinishing()) {
            /*
             * onNewIntent() may call finish(), but execution will still continue here.
             * We return now because we don't want to display the changelog which can
             * result in a leaked window error.
             */
            return;
        }

        ChangeLog cl = new ChangeLog(this);
        if (cl.isFirstRun()) {
            cl.getLogDialog().show();
        }
    }

    private void initializeActionBar() {
        mActionBar.setDisplayShowCustomEnabled(true);
        mActionBar.setCustomView(R.layout.actionbar_custom);

        View customView = mActionBar.getCustomView();
        mActionBarTitle = (TextView) customView.findViewById(R.id.actionbar_title_first);
        mActionBarSubTitle = (TextView) customView.findViewById(R.id.actionbar_title_sub);
        mActionBarUnread = (TextView) customView.findViewById(R.id.actionbar_unread_count);

        mActionBar.setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public void onNewIntent(Intent intent) {
        setIntent(intent); // onNewIntent doesn't autoset our "internal" intent

        mUnreadMessageCount = 0;
        String accountUuid = intent.getStringExtra(EXTRA_ACCOUNT);
        mAccount = Preferences.getPreferences(this).getAccount(accountUuid);

        if (mAccount == null) {
            /*
             * This can happen when a launcher shortcut is created for an
             * account, and then the account is deleted or data is wiped, and
             * then the shortcut is used.
             */
            finish();
            return;
        }

        if (intent.getBooleanExtra(EXTRA_FROM_SHORTCUT, false) &&
                !K9.FOLDER_NONE.equals(mAccount.getAutoExpandFolderName())) {
            onOpenFolder(mAccount.getAutoExpandFolderName());
            finish();
        } else {
            initializeActivityView();
        }
    }

    private void initializeActivityView() {
        mAdapter = new FolderListAdapter(this, mAccount);
        restorePreviousData();

        setListAdapter(mAdapter);
        getListView().setTextFilterEnabled(mAdapter.getFilter() != null); // should never be false but better safe then sorry
    }

    @SuppressWarnings("unchecked")
    private void restorePreviousData() {
        final Object previousData = getLastNonConfigurationInstance();

        if (previousData != null) {
            mAdapter.setFolders((ArrayList<FolderInfoHolder>) previousData);
            mAdapter.setFilterFolders(Collections.unmodifiableList(mAdapter.getFolders()));
        }
    }

    @Override
    public Object onRetainNonConfigurationInstance() {
        return (mAdapter == null) ? null : mAdapter.getFolders();
    }

    @Override
    public void onPause() {
        super.onPause();
        MessagingController.getInstance(getApplication()).removeListener(mListener);
        mListener.onPause(this);
    }

    /**
     * On resume we refresh the folder list (in the background) and we refresh the
     * messages for any folder that is currently open. This guarantees that things
     * like unread message count and read status are updated.
     */
    @Override
    public void onResume() {
        super.onResume();

        if (!mAccount.isAvailable(this)) {
            Log.i(K9.LOG_TAG, "account unavaliabale, not showing folder-list but account-list");
            Accounts.listAccounts(this);
            finish();
            return;
        }

        if (mAdapter == null) {
            initializeActivityView();
        }

        mListener = new FolderListActivityListener(mAdapter, mAccount);

        refreshTitle();

        MessagingController.getInstance(getApplication()).addListener(mListener);
        //mAccount.refresh(Preferences.getPreferences(this));
        MessagingController.getInstance(getApplication()).getAccountStats(this, mAccount, mListener);

        onRefresh(!REFRESH_REMOTE);

        MessagingController.getInstance(getApplication()).notifyAccountCancel(this, mAccount);
        mListener.onResume(this);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        //Shortcuts that work no matter what is selected
        switch (keyCode) {
            case KeyEvent.KEYCODE_Q: {
                onAccounts();
                return true;
            }

            case KeyEvent.KEYCODE_S: {
                onEditAccount();
                return true;
            }

            case KeyEvent.KEYCODE_H: {
                Toast toast = Toast.makeText(this, R.string.folder_list_help_key, Toast.LENGTH_LONG);
                toast.show();
                return true;
            }

            case KeyEvent.KEYCODE_1: {
                setDisplayMode(FolderMode.FIRST_CLASS);
                return true;
            }
            case KeyEvent.KEYCODE_2: {
                setDisplayMode(FolderMode.FIRST_AND_SECOND_CLASS);
                return true;
            }
            case KeyEvent.KEYCODE_3: {
                setDisplayMode(FolderMode.NOT_SECOND_CLASS);
                return true;
            }
            case KeyEvent.KEYCODE_4: {
                setDisplayMode(FolderMode.ALL);
                return true;
            }
        }


        return super.onKeyDown(keyCode, event);
    }

    public void setUnreadMessageCount(int unreadMessageCount) {
        this.mUnreadMessageCount = unreadMessageCount;
    }

    public void refreshTitle() {
        mActionBarTitle.setText(getString(R.string.folders_title));

        if (mUnreadMessageCount == 0) {
            mActionBarUnread.setVisibility(View.GONE);
        } else {
            mActionBarUnread.setText(Integer.toString(mUnreadMessageCount));
            mActionBarUnread.setVisibility(View.VISIBLE);
        }

        String operation = mListener.getOperation(FolderList.this);

        if (operation.length() < 1) {
            mActionBarSubTitle.setText(mAccount.getEmail());
        } else {
            mActionBarSubTitle.setText(operation);
        }

        mAdapter.notifyDataSetChanged();
    }

    public void newFolders(final List<FolderInfoHolder> newFolders) {
        mAdapter.getFolders().clear();
        mAdapter.getFolders().addAll(newFolders);
        mAdapter.setFilterFolders(mAdapter.getFolders());
        mAdapter.notifyDataSetChanged();
    }

    public void workingAccount(final int res) {
        String toastText = getString(res, mAccount.getDescription());
        Toast toast = Toast.makeText(getApplication(), toastText, Toast.LENGTH_SHORT);
        toast.show();
    }

    public void accountSizeChanged(final long oldSize, final long newSize) {
        String toastText = getString(R.string.account_size_changed, mAccount.getDescription(),
                SizeFormatter.formatSize(getApplication(), oldSize),
                SizeFormatter.formatSize(getApplication(), newSize));

        Toast toast = Toast.makeText(getApplication(), toastText, Toast.LENGTH_LONG);
        toast.show();
    }

    public void folderLoading(final String folder, final boolean loading) {
        FolderInfoHolder folderHolder = mAdapter.getFolder(folder);

        if (folderHolder != null) {
            folderHolder.loading = loading;
        }
    }

    public void progress(final boolean progress) {
        // Make sure we don't try this before the menu is initialized
        // this could happen while the activity is initialized.
        if (mRefreshMenuItem == null) {
            return;
        }

        if (progress) {
            mRefreshMenuItem.setActionView(mActionBarProgressView);
        } else {
            mRefreshMenuItem.setActionView(null);
        }
    }

    private void setDisplayMode(FolderMode newMode) {
        mAccount.setFolderDisplayMode(newMode);
        mAccount.save(Preferences.getPreferences(this));

        if (mAccount.getFolderPushMode() != FolderMode.NONE) {
            MailService.actionRestartPushers(this, null);
        }

        mAdapter.getFilter().filter(null);
        onRefresh(false);
    }

    private void onRefresh(final boolean forceRemote) {
        MessagingController.getInstance(getApplication()).listFolders(mAccount, forceRemote, mListener);
    }

    private void onEditPrefs() {
        Prefs.actionPrefs(this);
    }

    private void onEditAccount() {
        AccountSettings.actionSettings(this, mAccount);
    }

    private void onAccounts() {
        Accounts.listAccounts(this);
        finish();
    }

    private void onEmptyTrash(final Account account) {
        mAdapter.notifyDataSetChanged();
        MessagingController.getInstance(getApplication()).emptyTrash(account, null);
    }

    private void onClearFolder(Account account, String folderName) {
        // There has to be a cheaper way to get at the localFolder object than this
        LocalFolder localFolder = null;
        try {
            if (account == null || folderName == null || !account.isAvailable(FolderList.this)) {
                Log.i(K9.LOG_TAG, "not clear folder of unavailable account");
                return;
            }

            localFolder = account.getLocalStore().getFolder(folderName);
            localFolder.open(Folder.OPEN_MODE_RW);
            localFolder.clearAllMessages();
        } catch (Exception e) {
            Log.e(K9.LOG_TAG, "Exception while clearing folder", e);
        } finally {
            if (localFolder != null) {
                localFolder.close();
            }
        }

        onRefresh(!REFRESH_REMOTE);
    }

    private void sendMail(Account account) {
        MessagingController.getInstance(getApplication()).sendPendingMessages(account, mListener);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onAccounts();
                return true;

            case R.id.search:
                onSearchRequested();
                return true;

            case R.id.compose:
                MessageCompose.actionCompose(this, mAccount);
                return true;

            case R.id.check_mail:
                MessagingController.getInstance(getApplication()).checkMail(this, mAccount, true, true, mListener);
                return true;

            case R.id.send_messages:
                MessagingController.getInstance(getApplication()).sendPendingMessages(mAccount, null);
                return true;

            case R.id.list_folders:
                onRefresh(REFRESH_REMOTE);
                return true;

            case R.id.account_settings:
                onEditAccount();
                return true;

            case R.id.app_settings:
                onEditPrefs();
                return true;

            case R.id.empty_trash:
                onEmptyTrash(mAccount);
                return true;

            case R.id.compact:
                onCompact(mAccount);
                return true;

            case R.id.display_1st_class: {
                setDisplayMode(FolderMode.FIRST_CLASS);
                return true;
            }
            case R.id.display_1st_and_2nd_class: {
                setDisplayMode(FolderMode.FIRST_AND_SECOND_CLASS);
                return true;
            }
            case R.id.display_not_second_class: {
                setDisplayMode(FolderMode.NOT_SECOND_CLASS);
                return true;
            }
            case R.id.display_all: {
                setDisplayMode(FolderMode.ALL);
                return true;
            }
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public boolean onSearchRequested() {
        Bundle appData = new Bundle();
        appData.putString(MessageList.EXTRA_SEARCH_ACCOUNT, mAccount.getUuid());
        startSearch(null, false, appData, false);
        return true;
    }

    private void onOpenFolder(String folder) {
        LocalSearch search = new LocalSearch(folder);
        search.addAccountUuid(mAccount.getUuid());
        search.addAllowedFolder(folder);
        MessageList.actionDisplaySearch(this, search, false, false);
    }

    private void onCompact(Account account) {
        workingAccount(R.string.compacting_account);
        MessagingController.getInstance(getApplication()).compact(account, null);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.folder_list_option, menu);
        mRefreshMenuItem = menu.findItem(R.id.check_mail);
        configureFolderSearchView(menu);
        return true;
    }

    private void configureFolderSearchView(Menu menu) {
        final MenuItem folderMenuItem = menu.findItem(R.id.filter_folders);
        final SearchView folderSearchView = (SearchView) folderMenuItem.getActionView();
        folderSearchView.setQueryHint(getString(R.string.folder_list_filter_hint));
        folderSearchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {

            @Override
            public boolean onQueryTextSubmit(String query) {
                folderMenuItem.collapseActionView();
                mActionBarTitle.setText(getString(R.string.filter_folders_action));
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                mAdapter.getFilter().filter(newText);
                return true;
            }
        });

        folderSearchView.setOnCloseListener(new SearchView.OnCloseListener() {

            @Override
            public boolean onClose() {
                mActionBarTitle.setText(getString(R.string.folders_title));
                return false;
            }
        });
    }

    @Override
    public boolean onContextItemSelected(android.view.MenuItem item) {
        AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
        FolderInfoHolder folder = (FolderInfoHolder) mAdapter.getItem(info.position);

        switch (item.getItemId()) {
            case R.id.clear_local_folder:
                onClearFolder(mAccount, folder.name);
                break;
            case R.id.refresh_folder:
                checkMail(folder);
                break;
            case R.id.folder_settings:
                FolderSettings.actionSettings(this, mAccount, folder.name);
                break;
        }

        return super.onContextItemSelected(item);
    }

    /**
     * This class is responsible for reloading the list of local messages for a
     * given folder, notifying the adapter that the message have been loaded and
     * queueing up a remote update of the folder.
     */
    private void checkMail(FolderInfoHolder folder) {
        TracingPowerManager pm = TracingPowerManager.getPowerManager(this);
        final TracingWakeLock wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "FolderList checkMail");
        wakeLock.setReferenceCounted(false);
        wakeLock.acquire(K9.WAKE_LOCK_TIMEOUT);
        MessagingListener listener = new MessagingListener() {
            @Override
            public void synchronizeMailboxFinished(Account account, String folder, int totalMessagesInMailbox, int numNewMessages) {
                if (!account.equals(mAccount)) {
                    return;
                }
                wakeLock.release();
            }

            @Override
            public void synchronizeMailboxFailed(Account account, String folder,
                                                 String message) {
                if (!account.equals(mAccount)) {
                    return;
                }
                wakeLock.release();
            }
        };
        MessagingController.getInstance(getApplication()).synchronizeMailbox(mAccount, folder.name, listener, null);
        sendMail(mAccount);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        AdapterContextMenuInfo info = (AdapterContextMenuInfo) menuInfo;
        getMenuInflater().inflate(R.menu.folder_context, menu);

        FolderInfoHolder folder = mAdapter.getItem(info.position);
        menu.setHeaderTitle(folder.displayName);
    }

    public class FolderListActivityListener extends ActivityListener {
        private FolderListAdapter mAdapter;
        private Account mAccount;
        private Context mContext;

        public FolderListActivityListener(FolderListAdapter adapter, Account account) {
            this.mAdapter = adapter;
            this.mAccount = account;
            this.mContext = adapter.getContext();
        }

        @Override
        public void informUserOfStatus() {
            refreshTitle();
        }

        @Override
        public void accountStatusChanged(BaseAccount account, AccountStats stats) {
            if (!account.equals(mAccount)) {
                return;
            }

            if (stats == null) {
                return;
            }

            setUnreadMessageCount(stats.unreadMessageCount);
            refreshTitle();
        }

        @Override
        public void listFoldersStarted(Account account) {
            if (account.equals(mAccount)) {
                progress(true);
            }

            super.listFoldersStarted(account);
        }

        @Override
        public void listFoldersFailed(Account account, String message) {
            if (account.equals(mAccount)) {
                progress(false);
            }

            super.listFoldersFailed(account, message);
        }

        @Override
        public void listFoldersFinished(Account account) {
            if (account.equals(mAccount)) {
                progress(false);
                MessagingController.getInstance(mAdapter.getContext()).refreshListener(this);
                mAdapter.notifyDataSetChanged();
            }

            super.listFoldersFinished(account);
        }

        @Override
        public void listFolders(Account account, List<? extends Folder> folders) {
            if (account.equals(mAccount)) {
                List<FolderInfoHolder> newFolders = new LinkedList<FolderInfoHolder>();
                List<FolderInfoHolder> topFolders = new LinkedList<FolderInfoHolder>();
                FolderMode aMode = account.getFolderDisplayMode();

                for (Folder folder : folders) {
                    Folder.FolderClass fMode = folder.getDisplayClass();

                    if ((aMode == FolderMode.FIRST_CLASS && fMode != Folder.FolderClass.FIRST_CLASS)
                            || (aMode == FolderMode.FIRST_AND_SECOND_CLASS &&
                            fMode != Folder.FolderClass.FIRST_CLASS &&
                            fMode != Folder.FolderClass.SECOND_CLASS)
                            || (aMode == FolderMode.NOT_SECOND_CLASS && fMode == Folder.FolderClass.SECOND_CLASS)) {
                        continue;
                    }

                    FolderInfoHolder holder = new FolderInfoHolder(mAdapter.getContext(), folder, mAccount, -1);

                    if (folder.isInTopGroup()) {
                        topFolders.add(holder);
                    } else {
                        newFolders.add(holder);
                    }
                }

                Collections.sort(newFolders);
                Collections.sort(topFolders);
                topFolders.addAll(newFolders);
                newFolders(topFolders);
            }

            super.listFolders(account, folders);
        }

        @Override
        public void synchronizeMailboxStarted(Account account, String folder) {
            super.synchronizeMailboxStarted(account, folder);
            if (account.equals(mAccount)) {
                progress(true);
                folderLoading(folder, true);
                mAdapter.notifyDataSetChanged();
            }
        }

        @Override
        public void synchronizeMailboxFinished(Account account, String folder, int totalMessagesInMailbox, int numNewMessages) {
            super.synchronizeMailboxFinished(account, folder, totalMessagesInMailbox, numNewMessages);
            if (account.equals(mAccount)) {
                progress(false);
                folderLoading(folder, false);
                refreshFolder(account, folder);
            }
        }

        private void refreshFolder(Account account, String folderName) {
            // There has to be a cheaper way to get at the localFolder object than this
            Folder localFolder = null;
            try {
                if (account != null && folderName != null) {
                    if (!account.isAvailable(mContext)) {
                        Log.i(K9.LOG_TAG, "not refreshing folder of unavailable account");
                        return;
                    }

                    localFolder = account.getLocalStore().getFolder(folderName);
                    FolderInfoHolder folderHolder = mAdapter.getFolder(folderName);

                    if (folderHolder != null) {
                        folderHolder.populate(mContext, localFolder, mAccount, -1);
                        folderHolder.flaggedMessageCount = -1;
                        mAdapter.notifyDataSetChanged();
                    }
                }
            } catch (Exception e) {
                Log.e(K9.LOG_TAG, "Exception while populating folder", e);
            } finally {
                if (localFolder != null) {
                    localFolder.close();
                }
            }
        }

        @Override
        public void synchronizeMailboxFailed(Account account, String folder, String message) {
            super.synchronizeMailboxFailed(account, folder, message);

            if (!account.equals(mAccount)) {
                return;
            }

            progress(false);
            folderLoading(folder, false);
            FolderInfoHolder holder = mAdapter.getFolder(folder);

            if (holder != null) {
                holder.lastChecked = 0;
            }

            mAdapter.notifyDataSetChanged();
        }

        @Override
        public void setPushActive(Account account, String folderName, boolean enabled) {
            if (!account.equals(mAccount)) {
                return;
            }

            FolderInfoHolder holder = mAdapter.getFolder(folderName);

            if (holder != null) {
                holder.pushActive = enabled;
                mAdapter.notifyDataSetChanged();
            }
        }

        @Override
        public void messageDeleted(Account account, String folder, Message message) {
            synchronizeMailboxRemovedMessage(account, folder, message);
        }

        @Override
        public void emptyTrashCompleted(Account account) {
            if (account.equals(mAccount)) {
                refreshFolder(account, mAccount.getTrashFolderName());
            }
        }

        @Override
        public void folderStatusChanged(Account account, String folderName, int unreadMessageCount) {
            if (account.equals(mAccount)) {
                refreshFolder(account, folderName);
                informUserOfStatus();
            }
        }

        @Override
        public void sendPendingMessagesCompleted(Account account) {
            super.sendPendingMessagesCompleted(account);
            if (account.equals(mAccount)) {
                refreshFolder(account, mAccount.getOutboxFolderName());
            }
        }

        @Override
        public void sendPendingMessagesStarted(Account account) {
            super.sendPendingMessagesStarted(account);

            if (account.equals(mAccount)) {
                mAdapter.notifyDataSetChanged();
            }
        }

        @Override
        public void sendPendingMessagesFailed(Account account) {
            super.sendPendingMessagesFailed(account);
            if (account.equals(mAccount)) {
                refreshFolder(account, mAccount.getOutboxFolderName());
            }
        }

        @Override
        public void accountSizeChanged(Account account, long oldSize, long newSize) {
            if (account.equals(mAccount)) {
                accountSizeChanged(account, oldSize, newSize);
            }
        }
    }
}
