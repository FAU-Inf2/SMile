package com.fsck.k9.fragment;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatDelegate;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import com.fsck.k9.Account;
import com.fsck.k9.Account.SortType;
import com.fsck.k9.K9;
import com.fsck.k9.Preferences;
import com.fsck.k9.activity.ChooseFolder;
import com.fsck.k9.activity.MessageCompose;
import com.fsck.k9.activity.MessageReference;
import com.fsck.k9.activity.RemindMeList;
import com.fsck.k9.adapter.MessageAdapter;
import com.fsck.k9.helper.FolderHelper;
import com.fsck.k9.holder.FolderInfoHolder;
import com.fsck.k9.listener.ActivityListener;
import com.fsck.k9.cache.EmailProviderCache;
import com.fsck.k9.controller.MessagingController;
import com.fsck.k9.fragment.ConfirmationDialogFragment.ConfirmationDialogFragmentListener;
import com.fsck.k9.helper.MessageHelper;
import com.fsck.k9.helper.NotificationHelper;
import com.fsck.k9.helper.Utility;
import com.fsck.k9.mail.Address;
import com.fsck.k9.mail.Flag;
import com.fsck.k9.mail.Folder;
import com.fsck.k9.mail.Message;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mailstore.LocalFolder;
import com.fsck.k9.mailstore.LocalMessage;
import com.fsck.k9.presenter.MessageListPresenter;
import com.fsck.k9.search.LocalSearch;
import com.fsck.k9.search.SearchSpecification;
import com.fsck.k9.view.IMessageListView;
import com.fsck.k9.view.MessageListItemView;
import com.fsck.k9.view.MessageListView;
import com.fsck.k9.view.RefreshableMessageList;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.Future;

import de.fau.cs.mad.smile.android.R;

import static butterknife.ButterKnife.findById;

public class MessageListFragment extends Fragment
        implements AdapterView.OnItemClickListener,
        ConfirmationDialogFragmentListener,
        IMessageListPresenter, IMessageListView {
    protected static final String ARG_SEARCH = "searchObject";
    protected static final String ARG_THREADED_LIST = "threadedList";
    protected static final String ARG_IS_THREAD_DISPLAY = "isThreadedDisplay";

    private static final int ACTIVITY_CHOOSE_FOLDER_MOVE = 1;
    private static final int ACTIVITY_CHOOSE_FOLDER_COPY = 2;
    private static final String STATE_SELECTED_MESSAGES = "selectedMessages";
    private static final String STATE_ACTIVE_MESSAGE = "activeMessage";
    private static final String STATE_REMOTE_SEARCH_PERFORMED = "remoteSearchPerformed";
    private static final String STATE_MESSAGE_LIST = "listState";

    protected final ActivityListener mListener = new MessageListActivityListener();
    public List<Message> mExtraSearchResults;
    protected View mFooterView;
    protected FolderInfoHolder mCurrentFolder;
    protected MessagingController mController;
    protected Account mAccount;

    /**
     * Stores the name of the folder that we want to open as soon as possible after load.
     */
    protected String mFolderName;
    protected LocalSearch mSearch = null;
    protected int mSelectedCount = 0;
    protected MessageListFragmentListener mFragmentListener;
    protected boolean mThreadedList;
    // package visible so handler can access it
    Parcelable mSavedListState;
    //ListView mListView;
    /* package visibility for faster inner class access */
    MessageHelper mMessageHelper;
    private RefreshableMessageList mPullToRefreshView;
    private MessageListView messageListView;
    private NotificationHelper notificationHelper;
    private String[] mAccountUuids;
    private int mUnreadMessageCount = 0;
    private boolean mRemoteSearchPerformed = false;
    private Future<?> mRemoteSearchFuture = null;
    private String mTitle;
    private boolean mSingleAccountMode;
    private boolean mSingleFolderMode;
    private boolean mAllAccounts;
    private MessageListHandler mHandler = new MessageListHandler(this);
    private SortType mSortType = SortType.SORT_DATE;
    private boolean mSortAscending = true;
    private boolean mSortDateAscending = false;
    private Set<Long> mSelected = new HashSet<>();
    private ActionMode mActionMode;
    private Boolean mHasConnectivity;
    private List<LocalMessage> messages;
    /**
     * Relevant messages for the current context when we have to remember the chosen messages
     * between user interactions (e.g. selecting a folder for move operation).
     */
    private List<LocalMessage> mActiveMessages;
    private ActionModeCallback mActionModeCallback;
    private boolean mIsThreadDisplay;
    private Context mContext;
    private Preferences mPreferences;
    private MessageReference mActiveMessage;

    /**
     * {@code true} after {@link #onCreate(Bundle)} was executed. Used in {@link #updateTitle()} to
     * make sure we don't access member variables before initialization is complete.
     */
    private boolean mInitialized = false;
    private LocalBroadcastManager mLocalBroadcastManager;
    private BroadcastReceiver mCacheBroadcastReceiver;
    private IntentFilter mCacheIntentFilter;

    /**
     * Stores the unique ID of the message the context menu was opened for.
     * <p/>
     * We have to save this because the message list might change between the time the menu was
     * opened and when the user clicks on a menu item. When this happens the 'adapter position' that
     * is accessible via the {@code ContextMenu} object might correspond to another list item and we
     * would end up using/modifying the wrong message.
     * <p/>
     * The value of this field is {@code 0} when no context menu is currently open.
     */
    private long mContextMenuUniqueId = 0;
    private IMessageListPresenter presenter;

    public static MessageListFragment newInstance(LocalSearch search, boolean isThreadDisplay, boolean threadedList) {
        MessageListFragment fragment = new MessageListFragment();
        Bundle args = new Bundle();
        args.putParcelable(ARG_SEARCH, search);
        args.putBoolean(ARG_IS_THREAD_DISPLAY, isThreadDisplay);
        args.putBoolean(ARG_THREADED_LIST, threadedList);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        mContext = activity.getApplicationContext();

        try {
            mFragmentListener = (MessageListFragmentListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.getClass() +
                    " must implement MessageListFragmentListener");
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        messages = new ArrayList<>();

        Context appContext = getActivity().getApplicationContext();
        mPreferences = Preferences.getPreferences(appContext);
        mController = MessagingController.getInstance(appContext);
        notificationHelper = NotificationHelper.getInstance(appContext);
        setHasOptionsMenu(true);

        restoreInstanceState(savedInstanceState);
        decodeArguments();
        createCacheBroadcastReceiver(appContext);
        setPresenter(new MessageListPresenter(getContext(), mAccount, (LocalFolder) mCurrentFolder.folder, mHandler));

        mInitialized = true;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.message_list_fragment, container, false);

        mPullToRefreshView = findById(view, R.id.swipeRefreshLayout);
        messageListView = mPullToRefreshView.getMessageListView();
        View.OnClickListener clickListener = new MessageItemViewOnClickListener();
        //messageListView.addOnItemTouchListener(new MessageItemViewOnTouchListener(mContext, clickListener));

        final MessageAdapter messageAdapter = new MessageAdapter(messages, clickListener);

        messageListView.setAdapter(messageAdapter);
        FloatingActionButton actionButton = findById(view, R.id.fab);
        actionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MessageCompose.actionCompose(getActivity(), mAccount);
            }
        });

        setView(this);
        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mMessageHelper = MessageHelper.getInstance(getActivity());
        initializeMessageList();

        // This needs to be done before initializing the cursor loader below
        initializeSortSettings();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        saveListState(outState);

        outState.putBoolean(STATE_REMOTE_SEARCH_PERFORMED, mRemoteSearchPerformed);
        outState.putParcelable(STATE_ACTIVE_MESSAGE, mActiveMessage);
    }

    /**
     * Restore the state of a previous {@link MessageListFragment} instance.
     *
     * @see #onSaveInstanceState(Bundle)
     */
    private void restoreInstanceState(Bundle savedInstanceState) {
        if (savedInstanceState == null) {
            return;
        }

        restoreSelectedMessages(savedInstanceState);

        mRemoteSearchPerformed = savedInstanceState.getBoolean(STATE_REMOTE_SEARCH_PERFORMED);
        mSavedListState = savedInstanceState.getParcelable(STATE_MESSAGE_LIST);
        mActiveMessage = savedInstanceState.getParcelable(STATE_ACTIVE_MESSAGE);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if (view == mFooterView) {
            if (mCurrentFolder != null && !mSearch.isManualSearch()) {

                mController.loadMoreMessages(mAccount, mFolderName, null);

            } else if (mCurrentFolder != null && isRemoteSearch() &&
                    mExtraSearchResults != null && mExtraSearchResults.size() > 0) {

                int numResults = mExtraSearchResults.size();
                int limit = mAccount.getRemoteSearchNumResults();

                List<Message> toProcess = mExtraSearchResults;

                if (limit > 0 && numResults > limit) {
                    toProcess = toProcess.subList(0, limit);
                    mExtraSearchResults = mExtraSearchResults.subList(limit,
                            mExtraSearchResults.size());
                } else {
                    mExtraSearchResults = null;
                }

                mController.loadSearchResults(mAccount, mCurrentFolder.name, toProcess, mListener);
            }

            return;
        }

        Cursor cursor = (Cursor) parent.getItemAtPosition(position);
        if (cursor == null) {
            return;
        }

        Log.d(K9.LOG_TAG, "showing message at position: " + position);
        // This item represents a message; just display the message.
        //openMessageAtPosition(listViewToAdapterPosition(position));
    }

    /**
     * Write the unique IDs of selected messages to a {@link Bundle}.
     */
    private void saveSelectedMessages(Bundle outState) {
        long[] selected = new long[mSelected.size()];
        int i = 0;
        for (Long id : mSelected) {
            selected[i++] = id;
        }
        outState.putLongArray(STATE_SELECTED_MESSAGES, selected);
    }

    public static String getSenderAddressFromCursor(LocalMessage message) {
        Address[] fromAddrs = message.getFrom();
        return (fromAddrs.length > 0) ? fromAddrs[0].getAddress() : null;
    }

    void folderLoading(String folder, boolean loading) {
        if (mCurrentFolder != null && mCurrentFolder.name.equals(folder)) {
            mCurrentFolder.loading = loading;
        }
    }

    public void updateTitle() {
        if (!mInitialized) {
            return;
        }

        setWindowTitle();
        if (!mSearch.isManualSearch()) {
            setWindowProgress();
        }
    }

    private void setWindowProgress() {
        int level = Window.PROGRESS_END;

        if (mCurrentFolder != null && mCurrentFolder.loading && mListener.getFolderTotal() > 0) {
            int divisor = mListener.getFolderTotal();
            if (divisor != 0) {
                level = (Window.PROGRESS_END / divisor) * (mListener.getFolderCompleted());
                if (level > Window.PROGRESS_END) {
                    level = Window.PROGRESS_END;
                }
            }
        }

        mFragmentListener.setMessageListProgress(level);
    }

    private void setWindowTitle() {
        // regular folder content display
        if (!isManualSearch() && mSingleFolderMode) {
            Activity activity = getActivity();
            String displayName = FolderHelper.getDisplayName(activity, mAccount,
                    mFolderName);

            mFragmentListener.setMessageListTitle(displayName);

            String operation = mListener.getOperation(activity);
            if (operation.length() < 1) {
                mFragmentListener.setMessageListSubTitle(mAccount.getEmail());
            } else {
                mFragmentListener.setMessageListSubTitle(operation);
            }
        } else {
            // query result display.  This may be for a search folder as opposed to a user-initiated search.
            if (mTitle != null) {
                // This was a search folder; the search folder has overridden our title.
                mFragmentListener.setMessageListTitle(mTitle);
            } else {
                // This is a search result; set it to the default search result line.
                mFragmentListener.setMessageListTitle(getString(R.string.search_results));
            }

            mFragmentListener.setMessageListSubTitle(null);
        }

        // set unread count
        if (mUnreadMessageCount <= 0) {
            mFragmentListener.setUnreadCount(0);
        } else {
            if (!mSingleFolderMode && mTitle == null) {
                // The unread message count is easily confused
                // with total number of messages in the search result, so let's hide it.
                mFragmentListener.setUnreadCount(0);
            } else {
                mFragmentListener.setUnreadCount(mUnreadMessageCount);
            }
        }
    }

    void progress(final boolean progress) {
        mFragmentListener.enableActionBarProgress(progress);
        if (mPullToRefreshView != null && !progress) {
            //mPullToRefreshView.onRefreshComplete();
        }
    }

    /**
     * Restore selected messages from a {@link Bundle}.
     */
    private void restoreSelectedMessages(Bundle savedInstanceState) {
        long[] selected = savedInstanceState.getLongArray(STATE_SELECTED_MESSAGES);
        for (long id : selected) {
            mSelected.add(Long.valueOf(id));
        }
    }

    private void saveListState(Bundle outState) {
        if (mSavedListState != null) {
            // The previously saved state was never restored, so just use that.
            outState.putParcelable(STATE_MESSAGE_LIST, mSavedListState);
        }/* else if (mListView != null) {
            outState.putParcelable(STATE_MESSAGE_LIST, mListView.onSaveInstanceState());
        }*/
    }

    private void initializeSortSettings() {
        if (mSingleAccountMode) {
            mSortType = mAccount.getSortType();
            mSortAscending = mAccount.isSortAscending(mSortType);
            mSortDateAscending = mAccount.isSortAscending(SortType.SORT_DATE);
        } else {
            mSortType = K9.getSortType();
            mSortAscending = K9.isSortAscending(mSortType);
            mSortDateAscending = K9.isSortAscending(SortType.SORT_DATE);
        }
    }

    private void decodeArguments() {
        Bundle args = getArguments();

        enableThreadedList(args.getBoolean(ARG_THREADED_LIST, false));
        mIsThreadDisplay = args.getBoolean(ARG_IS_THREAD_DISPLAY, false);
        mSearch = args.getParcelable(ARG_SEARCH);
        mTitle = mSearch.getName();

        String[] accountUuids = mSearch.getAccountUuids();

        mSingleAccountMode = false;
        if (accountUuids.length == 1 && !mSearch.searchAllAccounts()) {
            mSingleAccountMode = true;
            mAccount = mPreferences.getAccount(accountUuids[0]);
        }

        mSingleFolderMode = false;
        if (mSingleAccountMode && (mSearch.getFolderNames().size() == 1)) {
            mSingleFolderMode = true;
            mFolderName = mSearch.getFolderNames().get(0);
            mCurrentFolder = FolderHelper.getFolder(mContext, mFolderName, mAccount);
        }

        mAllAccounts = false;
        if (mSingleAccountMode) {
            mAccountUuids = new String[]{mAccount.getUuid()};
        } else {
            if (accountUuids.length == 1 &&
                    accountUuids[0].equals(SearchSpecification.ALL_ACCOUNTS)) {
                mAllAccounts = true;

                List<Account> accounts = mPreferences.getAccounts();

                mAccountUuids = new String[accounts.size()];
                for (int i = 0, len = accounts.size(); i < len; i++) {
                    mAccountUuids[i] = accounts.get(i).getUuid();
                }

                if (mAccountUuids.length == 1) {
                    mSingleAccountMode = true;
                    mAccount = accounts.get(0);
                }
            } else {
                mAccountUuids = accountUuids;
            }
        }

        mActionModeCallback = new ActionModeCallback(mAccount, mSingleAccountMode);
    }

    private void initializeMessageList() {
        /*mAdapter = new MessageListAdapter(getActivity(), this, mThreadedList);

        if (mFolderName != null) {
            mCurrentFolder = FolderHelper.getFolder(mContext, mFolderName, mAccount);
        }

        if (mSingleFolderMode) {
            mListView.addFooterView(getFooterView(mListView));
            updateFooterView();
        }

        mListView.setAdapter(mAdapter);*/
    }

    private void createCacheBroadcastReceiver(Context appContext) {
        mLocalBroadcastManager = LocalBroadcastManager.getInstance(appContext);

        mCacheBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                //mAdapter.notifyDataSetChanged();
            }
        };

        mCacheIntentFilter = new IntentFilter(EmailProviderCache.ACTION_CACHE_UPDATED);
    }

    @Override
    public void onPause() {
        super.onPause();

        mLocalBroadcastManager.unregisterReceiver(mCacheBroadcastReceiver);
        mListener.onPause(getActivity());
        //mController.removeListener(mListener);
    }

    /**
     * On resume we refresh messages for the folder that is currently open.
     * This guarantees that things like unread message count and read status
     * are updated.
     */
    @Override
    public void onResume() {
        super.onResume();

        Context appContext = getActivity().getApplicationContext();

        // Check if we have connectivity.  Cache the value.
        if (mHasConnectivity == null) {
            mHasConnectivity = Utility.hasConnectivity(getActivity().getApplication());
        }

        mLocalBroadcastManager.registerReceiver(mCacheBroadcastReceiver, mCacheIntentFilter);
        mListener.onResume(getActivity());
        //mController.addListener(mListener);

        //Cancel pending new mail notifications when we open an account
        List<Account> accountsWithNotification;

        Account account = mAccount;
        if (account != null) {
            accountsWithNotification = Collections.singletonList(account);
        } else {
            accountsWithNotification = mPreferences.getAccounts();
        }

        for (Account accountWithNotification : accountsWithNotification) {
            notificationHelper.notifyAccountCancel(appContext, accountWithNotification);
        }

        if (mAccount != null && mFolderName != null && !mSearch.isManualSearch()) {
            mController.getFolderUnreadMessageCount(mAccount, mFolderName, mListener);
        }

        updateTitle();
    }

    private void initializePullToRefresh(LayoutInflater inflater, View layout) {
        /*mPullToRefreshView = findById(layout, R.id.message_list);

        // Set empty view
        View loadingView = inflater.inflate(R.layout.message_list_loading, null);
        mPullToRefreshView.setEmptyView(loadingView);

        if (isRemoteSearchAllowed()) {
            // "Pull to search server"
            mPullToRefreshView.setOnRefreshListener(
                    new PullToRefreshBase.OnRefreshListener<ListView>() {
                        @Override
                        public void onRefresh(PullToRefreshBase<ListView> refreshView) {
                            mPullToRefreshView.onRefreshComplete();
                            onRemoteSearchRequested();
                        }
                    });
            ILoadingLayout proxy = mPullToRefreshView.getLoadingLayoutProxy();
            proxy.setPullLabel(getString(
                    R.string.pull_to_refresh_remote_search_from_local_search_pull));
            proxy.setReleaseLabel(getString(
                    R.string.pull_to_refresh_remote_search_from_local_search_release));
        } else if (isCheckMailSupported()) {
            // "Pull to refresh"
            mPullToRefreshView.setOnRefreshListener(
                    new PullToRefreshBase.OnRefreshListener<ListView>() {
                        @Override
                        public void onRefresh(PullToRefreshBase<ListView> refreshView) {
                            checkMail();
                        }
                    });
        }

        // Disable pull-to-refresh until the message list has been loaded
        setPullToRefreshEnabled(false);*/
    }

    /**
     * Enable or disable pull-to-refresh.
     *
     * @param enable {@code true} to enable. {@code false} to disable.
     */
    private void setPullToRefreshEnabled(boolean enable) {
        //mPullToRefreshView.setMode((enable) ? PullToRefreshBase.Mode.PULL_FROM_START : PullToRefreshBase.Mode.DISABLED);
    }

    private void initializeLayout() {
       /* mListView = mPullToRefreshView.getRefreshableView();
        mListView.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
        mListView.setLongClickable(true);
        mListView.setFastScrollEnabled(true);
        mListView.setScrollingCacheEnabled(false);
        mListView.setOnItemClickListener(this);

        registerForContextMenu(mListView);*/
    }

    public void onCompose() {
        if (!mSingleAccountMode) {
            /*
             * If we have a query string, we don't have an account to let
             * compose start the default action.
             */
            mFragmentListener.onCompose(null);
        } else {
            mFragmentListener.onCompose(mAccount);
        }
    }

    public void onReply(LocalMessage message) {
        mFragmentListener.onReply(message);
    }

    public void onReplyAll(LocalMessage message) {
        mFragmentListener.onReplyAll(message);
    }

    public void onForward(LocalMessage message) {
        mFragmentListener.onForward(message);
    }

    public void onResendMessage(LocalMessage message) {
        mFragmentListener.onResendMessage(message);
    }

    public void changeSort(SortType sortType) {
        Boolean sortAscending = (mSortType == sortType) ? !mSortAscending : null;
        changeSort(sortType, sortAscending);
    }

    /**
     * User has requested a remote search.  Setup the bundle and start the intent.
     */
    public void onRemoteSearchRequested() {
        String searchAccount;
        String searchFolder;

        searchAccount = mAccount.getUuid();
        searchFolder = mCurrentFolder.name;

        String queryString = mSearch.getRemoteSearchArguments();

        mRemoteSearchPerformed = true;
        mRemoteSearchFuture = mController.searchRemoteMessages(searchAccount, searchFolder,
                queryString, null, null, mListener);

        setPullToRefreshEnabled(false);

        mFragmentListener.remoteSearchStarted();
    }

    public void onCycleSort() {
        SortType[] sorts = SortType.values();
        int curIndex = 0;

        for (int i = 0; i < sorts.length; i++) {
            if (sorts[i] == mSortType) {
                curIndex = i;
                break;
            }
        }

        curIndex++;

        if (curIndex == sorts.length) {
            curIndex = 0;
        }

        changeSort(sorts[curIndex]);
    }

    @Override
    public void move(LocalMessage message, String destFolder) {
        onMove(message);
    }

    @Override
    public void delete(LocalMessage message){
        onDelete(message);
    }

    @Override
    public void archive(LocalMessage message) {
        onArchive(message);
    }

    @Override
    public void remindMe(LocalMessage message) {
        onRemindMe(message);
    }

    @Override
    public void reply(LocalMessage message) {
        onReply(message);
    }

    @Override
    public void replyAll(LocalMessage message){
        onReplyAll(message);
    }

    @Override
    public void openMessage(MessageReference messageReference) {
        mHandler.openMessage(messageReference);
    }

    @Override
    public void sort(SortType sortType, Boolean ascending) {
        presenter.sort(sortType, ascending);
    }

    @Override
    public void enableThreadedList(boolean enable) {
        mThreadedList = enable;
    }

    /**
     * Change the sort type and sort order used for the message list.
     *
     * @param sortType      Specifies which field to use for sorting the message list.
     * @param sortAscending Specifies the sort order. If this argument is {@code null} the default search order
     *                      for the sort type is used.
     */
    // FIXME: Don't save the changes in the UI thread
    private void changeSort(SortType sortType, Boolean sortAscending) {
        mSortType = sortType;
        Account account = mAccount;

        if (account != null) {

            if (sortAscending == null) {
                mSortAscending = account.isSortAscending(mSortType);
            } else {
                mSortAscending = sortAscending;
            }
        } else {
            if (sortAscending == null) {
                mSortAscending = K9.isSortAscending(mSortType);
            } else {
                mSortAscending = sortAscending;
            }
        }

        reSort(sortType, mSortAscending);
    }

    private void reSort(SortType sortType, Boolean sortAscending) {
        int toastString = sortType.getToast(sortAscending);

        Toast toast = Toast.makeText(getActivity(), toastString, Toast.LENGTH_SHORT);
        toast.show();
        sort(sortType, sortAscending);
    }

    @Override
    public void setView(IMessageListView messageListView) {
        this.presenter.setView(messageListView);
    }

    private void onDelete(LocalMessage message) {
        onDelete(Collections.singletonList(message));
    }

    private void onDelete(List<LocalMessage> messages) {
        if (K9.confirmDelete()) {
            // remember the message selection for #onCreateDialog(int)
            mActiveMessages = messages;
            showDialog(R.id.dialog_confirm_delete);
        } else {
            onDeleteConfirmed(messages);
        }
    }

    private void onDeleteConfirmed(List<LocalMessage> messages) {
        if (mThreadedList) {
            mController.deleteThreads(messages);
        } else {
            mController.deleteMessages(messages, null);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != Activity.RESULT_OK) {
            return;
        }

        switch (requestCode) {
            case ACTIVITY_CHOOSE_FOLDER_MOVE:
            case ACTIVITY_CHOOSE_FOLDER_COPY: {
                if (data == null) {
                    return;
                }

                final String destFolderName = data.getStringExtra(ChooseFolder.EXTRA_NEW_FOLDER);
                final List<LocalMessage> messages = mActiveMessages;

                if (destFolderName != null) {

                    mActiveMessages = null; // don't need it any more

                    if (messages.size() > 0) {
                        messages.get(0).getFolder().setLastSelectedFolderName(destFolderName);
                    }

                    switch (requestCode) {
                        case ACTIVITY_CHOOSE_FOLDER_MOVE:
                            move(messages, destFolderName);
                            break;

                        case ACTIVITY_CHOOSE_FOLDER_COPY:
                            copy(messages, destFolderName);
                            break;
                    }
                }
                break;
            }
        }
    }

    public void onExpunge() {
        if (mCurrentFolder != null) {
            onExpunge(mAccount, mCurrentFolder.name);
        }
    }

    private void onExpunge(final Account account, String folderName) {
        mController.expunge(account, folderName, null);
    }

    private void showDialog(int dialogId) {
        DialogFragment fragment;
        switch (dialogId) {
            case R.id.dialog_confirm_spam: {
                String title = getString(R.string.dialog_confirm_spam_title);

                int selectionSize = mActiveMessages.size();
                String message = getResources().getQuantityString(
                        R.plurals.dialog_confirm_spam_message, selectionSize,
                        Integer.valueOf(selectionSize));

                String confirmText = getString(R.string.dialog_confirm_spam_confirm_button);
                String cancelText = getString(R.string.dialog_confirm_spam_cancel_button);

                fragment = ConfirmationDialogFragment.newInstance(dialogId, title, message,
                        confirmText, cancelText);
                break;
            }
            case R.id.dialog_confirm_delete: {
                String title = getString(R.string.dialog_confirm_delete_title);

                int selectionSize = mActiveMessages.size();
                String message = getResources().getQuantityString(
                        R.plurals.dialog_confirm_delete_messages, selectionSize,
                        Integer.valueOf(selectionSize));

                String confirmText = getString(R.string.dialog_confirm_delete_confirm_button);
                String cancelText = getString(R.string.dialog_confirm_delete_cancel_button);

                fragment = ConfirmationDialogFragment.newInstance(dialogId, title, message,
                        confirmText, cancelText);
                break;
            }
            default: {
                throw new RuntimeException("Called showDialog(int) with unknown dialog id.");
            }
        }

        fragment.setTargetFragment(this, dialogId);
        fragment.show(getFragmentManager(), getDialogTag(dialogId));
    }

    private String getDialogTag(int dialogId) {
        return "dialog-" + dialogId;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        switch (itemId) {
            case R.id.set_sort_date: {
                changeSort(SortType.SORT_DATE);
                return true;
            }
            case R.id.set_sort_arrival: {
                changeSort(SortType.SORT_ARRIVAL);
                return true;
            }
            case R.id.set_sort_subject: {
                changeSort(SortType.SORT_SUBJECT);
                return true;
            }
            case R.id.set_sort_sender: {
                changeSort(SortType.SORT_SENDER);
                return true;
            }
            case R.id.set_sort_flag: {
                changeSort(SortType.SORT_FLAGGED);
                return true;
            }
            case R.id.set_sort_unread: {
                changeSort(SortType.SORT_UNREAD);
                return true;
            }
            case R.id.set_sort_attach: {
                changeSort(SortType.SORT_ATTACHMENT);
                return true;
            }
            case R.id.select_all: {
                selectAll();
                return true;
            }
            case R.id.mark_all_as_read: {
                markAllAsRead();
                return true;
            }
        }

        if (!mSingleAccountMode) {
            // None of the options after this point are "safe" for search results
            //TODO: This is not true for "unread" and "starred" searches in regular folders
            return false;
        }

        switch (itemId) {
            case R.id.send_messages: {
                onSendPendingMessages();
                return true;
            }
            case R.id.expunge: {
                if (mCurrentFolder != null) {
                    onExpunge(mAccount, mCurrentFolder.name);
                }
                return true;
            }
            default: {
                return super.onOptionsItemSelected(item);
            }
        }
    }

    public void onSendPendingMessages() {
        mController.sendPendingMessages(mAccount, null);
    }
/*
    @Override
    public boolean onContextItemSelected(android.view.MenuItem item) {
        if (mContextMenuUniqueId == 0) {
            return false;
        }

        int adapterPosition = getPositionForUniqueId(mContextMenuUniqueId);
        if (adapterPosition == AdapterView.INVALID_POSITION) {
            return false;
        }

        switch (item.getItemId()) {
            case R.id.deselect:
            case R.id.select: {
                toggleMessageSelectWithAdapterPosition(adapterPosition);
                break;
            }
            case R.id.reply: {
                onReply(getMessageAtPosition(adapterPosition));
                break;
            }
            case R.id.reply_all: {
                onReplyAll(getMessageAtPosition(adapterPosition));
                break;
            }
            case R.id.forward: {
                onForward(getMessageAtPosition(adapterPosition));
                break;
            }
            case R.id.send_again: {
                onResendMessage(getMessageAtPosition(adapterPosition));
                mSelectedCount = 0;
                break;
            }
            case R.id.same_sender: {
                onSameSender(getMessageAtPosition(adapterPosition));
                break;
            }
            case R.id.delete: {
                LocalMessage message = getMessageAtPosition(adapterPosition);
                onDelete(message);
                break;
            }
            case R.id.mark_as_read: {
                setFlag(adapterPosition, Flag.SEEN, true);
                break;
            }
            case R.id.mark_as_unread: {
                setFlag(adapterPosition, Flag.SEEN, false);
                break;
            }
            case R.id.flag: {
                setFlag(adapterPosition, Flag.FLAGGED, true);
                break;
            }
            case R.id.unflag: {
                setFlag(adapterPosition, Flag.FLAGGED, false);
                break;
            }

            // only if the account supports this
            case R.id.archive: {
                onArchive(getMessageAtPosition(adapterPosition));
                break;
            }
            case R.id.spam: {
                onSpam(getMessageAtPosition(adapterPosition));
                break;
            }
            case R.id.move: {
                onMove(getMessageAtPosition(adapterPosition));
                break;
            }
            case R.id.remindme: {
                onRemindMe(getMessageAtPosition(adapterPosition));
                break;
            }
            case R.id.copy: {
                onCopy(getMessageAtPosition(adapterPosition));
                break;
            }
        }

        mContextMenuUniqueId = 0;
        return true;
    }

    private void onSameSender(LocalMessage message) {
        String senderAddress = getSenderAddressFromCursor(message);
        if (senderAddress != null) {
            mFragmentListener.showMoreFromSameSender(senderAddress);
        }
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);

        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
        LocalMessage message = getMessageAtPosition(info.position);

        if (message == null) {
            return;
        }

        getActivity().getMenuInflater().inflate(R.menu.message_list_item_context, menu);

        mContextMenuUniqueId = message.getId();
        Account account = message.getAccount();

        String subject = message.getSubject();
        boolean read = message.isSet(Flag.SEEN);
        boolean flagged = message.isSet(Flag.FLAGGED);

        menu.setHeaderTitle(subject);

        if (mSelected.contains(mContextMenuUniqueId)) {
            menu.findItem(R.id.select).setVisible(false);
        } else {
            menu.findItem(R.id.deselect).setVisible(false);
        }

        if (read) {
            menu.findItem(R.id.mark_as_read).setVisible(false);
        } else {
            menu.findItem(R.id.mark_as_unread).setVisible(false);
        }

        if (flagged) {
            menu.findItem(R.id.flag).setVisible(false);
        } else {
            menu.findItem(R.id.unflag).setVisible(false);
        }

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

    protected int listViewToAdapterPosition(int position) {
        if (position > 0 && position <= mAdapter.getCount()) {
            return position - 1;
        }

        return AdapterView.INVALID_POSITION;
    }

    private int adapterToListViewPosition(int position) {
        if (position >= 0 && position < mAdapter.getCount()) {
            return position + 1;
        }

        return AdapterView.INVALID_POSITION;
    }
*/

    /**
     * Set selection state for all messages.
     *
     * @param selected If {@code true} all messages get selected. Otherwise, all messages get deselected and
     *                 action mode is finished.
     */
    private void setSelectionState(boolean selected) {
        /*if (selected) {
            if (mAdapter.getCount() == 0) {
                // Nothing to do if there are no messages
                return;
            }

            mSelectedCount = 0;
            for (int i = 0, end = mAdapter.getCount(); i < end; i++) {
                LocalMessage message = getMessageAtPosition(i);
                long uniqueId = message.getId();
                mSelected.add(uniqueId);

                if (mThreadedList) {
                    int threadCount = 0;
                    try {
                        threadCount = message.getFolder().getThreadCount(message.getRootId());
                    } catch (MessagingException e) {
                        Log.e(K9.LOG_TAG, "error in setSelectionState ", e);
                    }

                    mSelectedCount += (threadCount > 1) ? threadCount : 1;
                } else {
                    mSelectedCount++;
                }
            }

            if (mActionMode == null) {
                startAndPrepareActionMode();
            }

            computeBatchDirection();
            updateActionModeTitle();
            computeSelectAllVisibility();
        } else {
            mSelected.clear();
            mSelectedCount = 0;
            if (mActionMode != null) {
                mActionMode.finish();
                mActionMode = null;
            }
        }

        mAdapter.notifyDataSetChanged();*/
    }

    protected void toggleMessageSelect(int listViewPosition) {
        /*int adapterPosition = listViewToAdapterPosition(listViewPosition);
        if (adapterPosition == AdapterView.INVALID_POSITION) {
            return;
        }

        toggleMessageSelectWithAdapterPosition(adapterPosition);*/
    }

    private void toggleMessageFlagWithAdapterPosition(int adapterPosition) {
        LocalMessage message = getMessageAtPosition(adapterPosition);
        setFlag(message, Flag.FLAGGED);
    }

    private void toggleMessageSelectWithAdapterPosition(int adapterPosition) {
        LocalMessage message = getMessageAtPosition(adapterPosition);
        if(message == null) {
            return;
        }

        long uniqueId = message.getId();

        boolean selected = mSelected.contains(uniqueId);
        if (!selected) {
            mSelected.add(uniqueId);
        } else {
            mSelected.remove(uniqueId);
        }

        int selectedCountDelta = 1;
        if (mThreadedList) {
            int threadCount = 0;
            try {
                threadCount = message.getFolder().getThreadCount(message.getRootId());
            } catch (MessagingException e) {
                Log.e(K9.LOG_TAG, "toggleMessageSelectWithAdapterPosition: ", e);
            }
            if (threadCount > 1) {
                selectedCountDelta = threadCount;
            }
        }

        if (mActionMode != null) {
            if (mSelectedCount == selectedCountDelta && selected) {
                mActionMode.finish();
                mActionMode = null;
                return;
            }
        } else {
            startAndPrepareActionMode();
        }

        if (selected) {
            mSelectedCount -= selectedCountDelta;
        } else {
            mSelectedCount += selectedCountDelta;
        }

        computeBatchDirection();
        updateActionModeTitle();

        computeSelectAllVisibility();

        //mAdapter.notifyDataSetChanged();
    }

    private void updateActionModeTitle() {
        mActionMode.setTitle(String.format(getString(R.string.actionbar_selected), mSelectedCount));
    }

    private void computeSelectAllVisibility() {
        //mActionModeCallback.showSelectAll(mSelected.size() != mAdapter.getCount());
    }

    @Override
    public void setFlag(LocalMessage message, final Flag flag) {
        presenter.setFlag(message, flag);
    }

    private void computeBatchDirection() {
        /*boolean isBatchFlag = false;
        boolean isBatchRead = false;

        for (int i = 0, end = mAdapter.getCount(); i < end; i++) {
            LocalMessage message = getMessageAtPosition(i);
            if(message == null) {
                continue;
            }

            long uniqueId = message.getId();

            if (mSelected.contains(uniqueId)) {
                boolean read = message.isSet(Flag.SEEN);
                boolean flagged = message.isSet(Flag.FLAGGED);

                if (!flagged) {
                    isBatchFlag = true;
                }

                if (!read) {
                    isBatchRead = true;
                }

                if (isBatchFlag && isBatchRead) {
                    break;
                }
            }
        }

        mActionModeCallback.showMarkAsRead(isBatchRead);
        mActionModeCallback.showFlag(isBatchFlag);*/
    }

    private void setFlagForSelected(final Flag flag, final boolean newState) {
        /*if (mSelected.isEmpty()) {
            return;
        }

        Map<Account, List<Long>> messageMap = new HashMap<>();
        Map<Account, List<Long>> threadMap = new HashMap<>();
        Set<Account> accounts = new HashSet<>();

        for (int position = 0; position < mAdapter.getCount(); position++) {
            LocalMessage message = getMessageAtPosition(position);
            if(message == null) {
                continue;
            }

            long uniqueId = message.getId();

            if (mSelected.contains(uniqueId)) {
                Account account = message.getAccount();
                accounts.add(account);
                LocalFolder folder = message.getFolder();
                int threadCount = 0;

                try {
                    threadCount = folder.getThreadCount(message.getRootId());
                } catch (MessagingException e) {
                    Log.e(K9.LOG_TAG, "error in setFlag ", e);
                }

                if (mThreadedList && threadCount > 1) {
                    List<Long> threadRootIdList = threadMap.get(account);
                    if (threadRootIdList == null) {
                        threadRootIdList = new ArrayList<>();
                        threadMap.put(account, threadRootIdList);
                    }

                    threadRootIdList.add(message.getRootId());
                } else {
                    List<Long> messageIdList = messageMap.get(account);
                    if (messageIdList == null) {
                        messageIdList = new ArrayList<>();
                        messageMap.put(account, messageIdList);
                    }

                    messageIdList.add(message.getId());
                }
            }
        }

        for (Account account : accounts) {
            List<Long> messageIds = messageMap.get(account);
            List<Long> threadRootIds = threadMap.get(account);

            if (messageIds != null) {
                mController.setFlag(account, messageIds, flag, newState);
            }

            if (threadRootIds != null) {
                mController.setFlagForThreads(account, threadRootIds, flag, newState);
            }
        }

        computeBatchDirection();*/
    }

    private void onMove(LocalMessage message) {
        onMove(Collections.singletonList(message));
    }

    /**
     * Display the message move activity.
     *
     * @param messages Never {@code null}.
     */
    private void onMove(List<LocalMessage> messages) {
        if (!checkCopyOrMovePossible(messages, FolderOperation.MOVE)) {
            return;
        }

        final Folder folder;
        if (mIsThreadDisplay) {
            folder = messages.get(0).getFolder();
        } else if (mSingleFolderMode) {
            folder = mCurrentFolder.folder;
        } else {
            folder = null;
        }


        displayFolderChoice(ACTIVITY_CHOOSE_FOLDER_MOVE, folder,
                messages.get(0).getFolder().getAccountUuid(), null,
                messages);
    }

    private void onRemindMe(LocalMessage message) {
        startActivity(RemindMeList.createRemindMe(this.getActivity(), message));
    }

    private void onCopy(LocalMessage message) {
        onCopy(Collections.singletonList(message));
    }

    /**
     * Display the message copy activity.
     *
     * @param messages Never {@code null}.
     */
    private void onCopy(List<LocalMessage> messages) {
        if (!checkCopyOrMovePossible(messages, FolderOperation.COPY)) {
            return;
        }

        final Folder folder;
        if (mIsThreadDisplay) {
            folder = messages.get(0).getFolder();
        } else if (mSingleFolderMode) {
            folder = mCurrentFolder.folder;
        } else {
            folder = null;
        }

        displayFolderChoice(ACTIVITY_CHOOSE_FOLDER_COPY, folder,
                messages.get(0).getFolder().getAccountUuid(),
                null,
                messages);
    }

    /**
     * Helper method to manage the invocation of {@link #startActivityForResult(Intent, int)} for a
     * folder operation ({@link ChooseFolder} activity), while saving a list of associated messages.
     *
     * @param requestCode If {@code >= 0}, this code will be returned in {@code onActivityResult()} when the
     *                    activity exits.
     * @param folder      The source folder. Never {@code null}.
     * @param messages    Messages to be affected by the folder operation. Never {@code null}.
     * @see #startActivityForResult(Intent, int)
     */
    private void displayFolderChoice(int requestCode, Folder folder,
                                     String accountUuid, String lastSelectedFolderName,
                                     List<LocalMessage> messages) {

        Intent intent = new Intent(getActivity(), ChooseFolder.class);
        intent.putExtra(ChooseFolder.EXTRA_ACCOUNT, accountUuid);
        intent.putExtra(ChooseFolder.EXTRA_SEL_FOLDER, lastSelectedFolderName);

        if (folder == null) {
            intent.putExtra(ChooseFolder.EXTRA_SHOW_CURRENT, "yes");
        } else {
            intent.putExtra(ChooseFolder.EXTRA_CUR_FOLDER, folder.getName());
        }

        // remember the selected messages for #onActivityResult
        mActiveMessages = messages;
        startActivityForResult(intent, requestCode);
    }

    private void onArchive(final LocalMessage message) {
        onArchive(Collections.singletonList(message));
    }

    private void onArchive(final List<LocalMessage> messages) {
        Map<Account, List<LocalMessage>> messagesByAccount = groupMessagesByAccount(messages);

        for (Entry<Account, List<LocalMessage>> entry : messagesByAccount.entrySet()) {
            Account account = entry.getKey();
            String archiveFolder = account.getArchiveFolderName();

            if (!K9.FOLDER_NONE.equals(archiveFolder)) {
                move(entry.getValue(), archiveFolder);
            }
        }
    }

    private Map<Account, List<LocalMessage>> groupMessagesByAccount(final List<LocalMessage> messages) {
        Map<Account, List<LocalMessage>> messagesByAccount = new HashMap<>();
        for (LocalMessage message : messages) {
            Account account = message.getAccount();

            List<LocalMessage> msgList = messagesByAccount.get(account);
            if (msgList == null) {
                msgList = new ArrayList<>();
                messagesByAccount.put(account, msgList);
            }

            msgList.add(message);
        }

        return messagesByAccount;
    }

    private void onSpam(LocalMessage message) {
        onSpam(Collections.singletonList(message));
    }

    /**
     * Move messages to the spam folder.
     *
     * @param messages The messages to move to the spam folder. Never {@code null}.
     */
    private void onSpam(List<LocalMessage> messages) {
        if (K9.confirmSpam()) {
            // remember the message selection for #onCreateDialog(int)
            mActiveMessages = messages;
            showDialog(R.id.dialog_confirm_spam);
        } else {
            onSpamConfirmed(messages);
        }
    }

    private void onSpamConfirmed(List<LocalMessage> messages) {
        Map<Account, List<LocalMessage>> messagesByAccount = groupMessagesByAccount(messages);

        for (Entry<Account, List<LocalMessage>> entry : messagesByAccount.entrySet()) {
            Account account = entry.getKey();
            String spamFolder = account.getSpamFolderName();

            if (!K9.FOLDER_NONE.equals(spamFolder)) {
                move(entry.getValue(), spamFolder);
            }
        }
    }

    /**
     * Display a Toast message if any message isn't synchronized
     *
     * @param messages  The messages to copy or move. Never {@code null}.
     * @param operation The type of operation to perform. Never {@code null}.
     * @return {@code true}, if operation is possible.
     */
    private boolean checkCopyOrMovePossible(final List<LocalMessage> messages,
                                            final FolderOperation operation) {
        if (messages.isEmpty()) {
            return false;
        }

        boolean first = true;
        for (final LocalMessage message : messages) {
            if (first) {
                first = false;
                // account check
                final Account account = message.getAccount();
                if ((operation == FolderOperation.MOVE && !mController.isMoveCapable(account)) ||
                        (operation == FolderOperation.COPY && !mController.isCopyCapable(account))) {
                    return false;
                }
            }
            // message check
            if ((operation == FolderOperation.MOVE && !mController.isMoveCapable(message)) ||
                    (operation == FolderOperation.COPY && !mController.isCopyCapable(message))) {
                final Toast toast = Toast.makeText(getActivity(), R.string.move_copy_cannot_copy_unsynced_message,
                        Toast.LENGTH_LONG);
                toast.show();
                return false;
            }
        }
        return true;
    }

    /**
     * Copy the specified messages to the specified folder.
     *
     * @param messages    List of messages to copy. Never {@code null}.
     * @param destination The name of the destination folder. Never {@code null}.
     */
    private void copy(List<LocalMessage> messages, final String destination) {
        copyOrMove(messages, destination, FolderOperation.COPY);
    }

    /**
     * Move the specified messages to the specified folder.
     *
     * @param messages    The list of messages to move. Never {@code null}.
     * @param destination The name of the destination folder. Never {@code null}.
     */
    private void move(List<LocalMessage> messages, final String destination) {
        copyOrMove(messages, destination, FolderOperation.MOVE);
    }

    /**
     * The underlying implementation for {@link #copy(List, String)} and
     * {@link #move(List, String)}. This method was added mainly because those 2
     * methods share common behavior.
     *
     * @param messages    The list of messages to copy or move. Never {@code null}.
     * @param destination The name of the destination folder. Never {@code null} or {@link K9#FOLDER_NONE}.
     * @param operation   Specifies what operation to perform. Never {@code null}.
     */
    private void copyOrMove(List<LocalMessage> messages, final String destination,
                            final FolderOperation operation) {

        Map<String, List<LocalMessage>> folderMap = new HashMap<String, List<LocalMessage>>();

        for (LocalMessage message : messages) {
            if ((operation == FolderOperation.MOVE && !mController.isMoveCapable(message)) ||
                    (operation == FolderOperation.COPY && !mController.isCopyCapable(message))) {

                Toast.makeText(getActivity(), R.string.move_copy_cannot_copy_unsynced_message,
                        Toast.LENGTH_LONG).show();

                // XXX return meaningful error value?

                // message isn't synchronized
                return;
            }

            String folderName = message.getFolder().getName();
            if (folderName.equals(destination)) {
                // Skip messages already in the destination folder
                continue;
            }

            List<LocalMessage> outMessages = folderMap.get(folderName);
            if (outMessages == null) {
                outMessages = new ArrayList<>();
                folderMap.put(folderName, outMessages);
            }

            outMessages.add(message);
        }

        for (Map.Entry<String, List<LocalMessage>> entry : folderMap.entrySet()) {
            String folderName = entry.getKey();
            List<LocalMessage> outMessages = entry.getValue();
            Account account = outMessages.get(0).getAccount();

            if (operation == FolderOperation.MOVE) {
                if (mThreadedList) {
                    mController.moveMessagesInThread(account, folderName, outMessages, destination);
                } else {
                    mController.moveMessages(account, folderName, outMessages, destination, null);
                }
            } else {
                if (mThreadedList) {
                    mController.copyMessagesInThread(account, folderName, outMessages, destination);
                } else {
                    mController.copyMessages(account, folderName, outMessages, destination, null);
                }
            }
        }
    }

    @Override
    public void doPositiveClick(int dialogId) {
        switch (dialogId) {
            case R.id.dialog_confirm_spam: {
                onSpamConfirmed(mActiveMessages);
                // No further need for this reference
                mActiveMessages = null;
                break;
            }
            case R.id.dialog_confirm_delete: {
                onDeleteConfirmed(mActiveMessages);
                mActiveMessage = null;
                break;
            }
        }
    }

    @Override
    public void doNegativeClick(int dialogId) {
        switch (dialogId) {
            case R.id.dialog_confirm_spam:
            case R.id.dialog_confirm_delete: {
                // No further need for this reference
                mActiveMessages = null;
                break;
            }
        }
    }

    @Override
    public void dialogCancelled(int dialogId) {
        doNegativeClick(dialogId);
    }

    public void checkMail() {
        if (isSingleAccountMode() && isSingleFolderMode()) {
            mController.synchronizeMailbox(mAccount, mFolderName, mListener, null);
            mController.sendPendingMessages(mAccount, mListener);
        } else if (mAllAccounts) {
            mController.checkMail(mContext, null, true, true, mListener);
        } else {
            for (String accountUuid : mAccountUuids) {
                Account account = mPreferences.getAccount(accountUuid);
                mController.checkMail(mContext, account, true, true, mListener);
            }
        }
    }

    /**
     * We need to do some special clean up when leaving a remote search result screen. If no
     * remote search is in progress, this method does nothing special.
     */
    @Override
    public void onStop() {
        // If we represent a remote search, then kill that before going back.
        if (isRemoteSearch() && mRemoteSearchFuture != null) {
            try {
                Log.i(K9.LOG_TAG, "Remote search in progress, attempting to abort...");
                // Canceling the future stops any message fetches in progress.
                final boolean cancelSuccess = mRemoteSearchFuture.cancel(true);   // mayInterruptIfRunning = true
                if (!cancelSuccess) {
                    Log.e(K9.LOG_TAG, "Could not cancel remote search future.");
                }
                // Closing the folder will kill off the connection if we're mid-search.
                final Account searchAccount = mAccount;
                final Folder remoteFolder = mCurrentFolder.folder;
                remoteFolder.close();
                // Send a remoteSearchFinished() message for good measure.
                mListener.remoteSearchFinished(mCurrentFolder.name, 0, searchAccount.getRemoteSearchNumResults(), null);
            } catch (Exception e) {
                // Since the user is going back, log and squash any exceptions.
                Log.e(K9.LOG_TAG, "Could not abort remote search before going back", e);
            }
        }
        super.onStop();
    }

    public List<MessageReference> getMessageReferences() {
        List<MessageReference> messageRefs = new ArrayList<>();

        for (int i = 0; i < messages.size(); i++) {
            messageRefs.add(messages.get(i).makeMessageReference());
        }

        return messageRefs;
    }

    public void selectAll() {
        setSelectionState(true);
    }

    public void onMoveUp() {
        /*int currentPosition = mListView.getSelectedItemPosition();
        if (currentPosition == AdapterView.INVALID_POSITION || mListView.isInTouchMode()) {
            currentPosition = mListView.getFirstVisiblePosition();
        }
        if (currentPosition > 0) {
            mListView.setSelection(currentPosition - 1);
        }*/
    }

    public void onMoveDown() {
        /*int currentPosition = mListView.getSelectedItemPosition();
        if (currentPosition == AdapterView.INVALID_POSITION || mListView.isInTouchMode()) {
            currentPosition = mListView.getFirstVisiblePosition();
        }

        if (currentPosition < mListView.getCount()) {
            mListView.setSelection(currentPosition + 1);
        }*/
    }

    @Override
    public boolean openPreviousMessage(MessageReference messageReference) {
        return presenter.openPreviousMessage(messageReference);
    }

    @Override
    public boolean openNextMessage(MessageReference messageReference) {
        return presenter.openNextMessage(messageReference);
    }

    @Override
    public boolean isFirst(MessageReference messageReference) {
        return presenter.isFirst(messageReference);
    }

    @Override
    public boolean isLast(MessageReference messageReference) {
        return presenter.isLast(messageReference);
    }

    public void onReverseSort() {
        changeSort(mSortType);
    }

    private LocalMessage getSelectedMessage() {
        /*int listViewPosition = mListView.getSelectedItemPosition();
        int adapterPosition = listViewToAdapterPosition(listViewPosition);

        return getMessageAtPosition(adapterPosition);*/
        return null;
    }

    private int getAdapterPositionForSelectedMessage() {
        //int listViewPosition = mListView.getSelectedItemPosition();
        //return listViewToAdapterPosition(listViewPosition);
        return -1;
    }

    private LocalMessage getMessageAtPosition(int position) {
        if (position < 0 || position >= messages.size()) {
            return null;
        }

        return messages.get(position);
    }

    private List<LocalMessage> getCheckedMessages() {
        List<LocalMessage> checkedMessages = new ArrayList<>(mSelected.size());
        /*for (int position = 0; position < mAdapter.getCount(); position++) {
            LocalMessage message = getMessageAtPosition(position);
            if (message == null) {
                continue;
            }

            long uniqueId = message.getId();

            if (mSelected.contains(uniqueId)) {
                    checkedMessages.add(message);
            }
        }*/

        return checkedMessages;
    }

    public void onDelete() {
        LocalMessage message = getSelectedMessage();
        if (message != null) {
            onDelete(message);
        }
    }

    public void toggleMessageSelect() {
        //toggleMessageSelect(mListView.getSelectedItemPosition());
    }

    public void onToggleFlagged() {
        onToggleFlag(Flag.FLAGGED);
    }

    public void onToggleRead() {
        onToggleFlag(Flag.SEEN);
    }

    private void onToggleFlag(Flag flag) {
        int adapterPosition = getAdapterPositionForSelectedMessage();
        if (adapterPosition == ListView.INVALID_POSITION) {
            return;
        }

        LocalMessage message = getMessageAtPosition(adapterPosition);
        if(message == null) {
            return;
        }

        setFlag(message, flag);
    }

    public void onMove() {
        LocalMessage message = getSelectedMessage();
        if (message != null) {
            onMove(message);
        }
    }

    public void onArchive() {
        LocalMessage message = getSelectedMessage();
        if (message != null) {
            onArchive(message);
        }
    }

    public void onCopy() {
        LocalMessage message = getSelectedMessage();
        if (message != null) {
            onCopy(message);
        }
    }

    public boolean isOutbox() {
        return (mFolderName != null && mFolderName.equals(mAccount.getOutboxFolderName()));
    }

    public boolean isErrorFolder() {
        return K9.ERROR_FOLDER_NAME.equals(mFolderName);
    }

    public boolean isRemoteFolder() {
        if (mSearch.isManualSearch() || isOutbox() || isErrorFolder()) {
            return false;
        }

        if (!mController.isMoveCapable(mAccount)) {
            // For POP3 accounts only the Inbox is a remote folder.
            return (mFolderName != null && mFolderName.equals(mAccount.getInboxFolderName()));
        }

        return true;
    }

    public boolean isManualSearch() {
        return mSearch.isManualSearch();
    }

    public boolean isAccountExpungeCapable() {
        try {
            return (mAccount != null && mAccount.getRemoteStore().isExpungeCapable());
        } catch (Exception e) {
            return false;
        }
    }

    public void onRemoteSearch() {
        // Remote search is useless without the network.
        if (mHasConnectivity) {
            onRemoteSearchRequested();
        } else {
            Toast.makeText(getActivity(), getText(R.string.remote_search_unavailable_no_network),
                    Toast.LENGTH_SHORT).show();
        }
    }

    public boolean isRemoteSearch() {
        return mRemoteSearchPerformed;
    }

    public boolean isRemoteSearchAllowed() {
        if (!mSearch.isManualSearch() || mRemoteSearchPerformed || !mSingleFolderMode) {
            return false;
        }

        boolean allowRemoteSearch = false;
        final Account searchAccount = mAccount;
        if (searchAccount != null) {
            allowRemoteSearch = searchAccount.allowRemoteSearch();
        }

        return allowRemoteSearch;
    }

    public boolean onSearchRequested() {
        String folderName = (mCurrentFolder != null) ? mCurrentFolder.name : null;
        return mFragmentListener.startSearch(mAccount, folderName);
    }


    /**
     * Close the context menu when the message it was opened for is no longer in the message list.
     */
    private void updateContextMenu() {
        if (mContextMenuUniqueId == 0) {
            return;
        }

        for (LocalMessage message : messages) {
            long uniqueId = message.getId();
            if (uniqueId == mContextMenuUniqueId) {
                return;
            }
        }

        mContextMenuUniqueId = 0;
        Activity activity = getActivity();
        if (activity != null) {
            activity.closeContextMenu();
        }
    }

    private void cleanupSelected() {
        if (mSelected.isEmpty()) {
            return;
        }

        Set<Long> selected = new HashSet<>();
        for (LocalMessage message : messages) {
            long uniqueId = message.getId();
            if (mSelected.contains(uniqueId)) {
                selected.add(uniqueId);
            }
        }

        mSelected = selected;
    }

    /**
     * Starts or finishes the action mode when necessary.
     */
    private void resetActionMode() {
        if (mSelected.isEmpty()) {
            if (mActionMode != null) {
                mActionMode.finish();
            }
            return;
        }

        if (mActionMode == null) {
            startAndPrepareActionMode();
        }

        recalculateSelectionCount();
        updateActionModeTitle();
    }

    private void startAndPrepareActionMode() {
        AppCompatDelegate appCompatDelegate = AppCompatDelegate.create(getActivity(), null);
        mActionMode = appCompatDelegate.startSupportActionMode(mActionModeCallback);
        mActionMode.invalidate();
    }

    /**
     * Recalculates the selection count.
     * <p/>
     * <p>
     * For non-threaded lists this is simply the number of visibly selected messages. If threaded
     * view is enabled this method counts the number of messages in the selected threads.
     * </p>
     */
    private void recalculateSelectionCount() {
        if (!mThreadedList) {
            mSelectedCount = mSelected.size();
            return;
        }

        mSelectedCount = 0;
        /*for (int i = 0; i < mAdapter.getCount(); i++) {
            LocalMessage message = getMessageAtPosition(i);
            long uniqueId = message.getId();

            if (mSelected.contains(uniqueId)) {
                int threadCount = 0;
                try {
                    threadCount = message.getFolder().getThreadCount(message.getRootId());
                } catch (MessagingException e) {
                    Log.e(K9.LOG_TAG, "recalculateSelectionCount", e);
                }
                
                mSelectedCount += (threadCount > 1) ? threadCount : 1;
            }
        }*/
    }

    void remoteSearchFinished() {
        mRemoteSearchFuture = null;
    }

    /**
     * Mark a message as 'active'.
     * <p/>
     * <p>
     * The active message is the one currently displayed in the message view portion of the split
     * view.
     * </p>
     *
     * @param messageReference {@code null} to not mark any message as being 'active'.
     */
    public void setActiveMessage(MessageReference messageReference) {
        mActiveMessage = messageReference;
    }

    public boolean isSingleAccountMode() {
        return mSingleAccountMode;
    }

    public boolean isSingleFolderMode() {
        return mSingleFolderMode;
    }

    public boolean isInitialized() {
        return mInitialized;
    }

    public boolean isMarkAllAsReadSupported() {
        return (isSingleAccountMode() && isSingleFolderMode());
    }

    public void markAllAsRead() {
        if (isMarkAllAsReadSupported()) {
            mController.markAllMessagesRead(mAccount, mFolderName);
        }
    }

    public boolean isCheckMailSupported() {
        return (mAllAccounts || !isSingleAccountMode() || !isSingleFolderMode() ||
                isRemoteFolder());
    }

    private boolean isCheckMailAllowed() {
        return (!isManualSearch() && isCheckMailSupported());
    }

    private boolean isPullToRefreshAllowed() {
        return (isRemoteSearchAllowed() || isCheckMailAllowed());
    }

    @Override
    public void setPresenter(IMessageListPresenter presenter) {
        this.presenter = presenter;
        this.presenter.enableThreadedList(mThreadedList);
    }

    @Override
    public void showMessageList(List<LocalMessage> messageList) {
        messages.clear();
        messages.addAll(messageList);
        messageListView.getAdapter().notifyDataSetChanged();
    }

    private enum FolderOperation {
        COPY, MOVE
    }

    class MessageListActivityListener extends ActivityListener {
        @Override
        public void remoteSearchFailed(String folder, final String err) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    Activity activity = getActivity();
                    if (activity != null) {
                        Toast.makeText(activity, R.string.remote_search_error,
                                Toast.LENGTH_LONG).show();
                    }
                }
            });
        }

        @Override
        public void remoteSearchStarted(String folder) {
            mHandler.progress(true);
        }

        @Override
        public void enableProgressIndicator(boolean enable) {
            mHandler.progress(enable);
        }

        @Override
        public void remoteSearchFinished(String folder, int numResults, int maxResults, List<Message> extraResults) {
            mHandler.progress(false);
            mHandler.remoteSearchFinished();
            mExtraSearchResults = extraResults;
            mFragmentListener.setMessageListProgress(Window.PROGRESS_END);
        }

        @Override
        public void remoteSearchServerQueryComplete(String folderName, int numResults, int maxResults) {
            mHandler.progress(true);
            mFragmentListener.setMessageListProgress(Window.PROGRESS_START);
        }

        @Override
        public void informUserOfStatus() {
            mHandler.refreshTitle();
        }

        @Override
        public void synchronizeMailboxStarted(Account account, String folder) {
            if (updateForMe(account, folder)) {
                mHandler.progress(true);
                mHandler.folderLoading(folder, true);
            }

            super.synchronizeMailboxStarted(account, folder);
        }

        @Override
        public void synchronizeMailboxFinished(Account account, String folder,
                                               int totalMessagesInMailbox, int numNewMessages) {

            Log.d(K9.LOG_TAG, "MessageListFragment.synchronizeMailboxFinished");

            if (updateForMe(account, folder)) {
                mHandler.progress(false);
                mHandler.folderLoading(folder, false);
            }

            super.synchronizeMailboxFinished(account, folder, totalMessagesInMailbox, numNewMessages);
        }

        @Override
        public void synchronizeMailboxFailed(Account account, String folder, String message) {

            if (updateForMe(account, folder)) {
                mHandler.progress(false);
                mHandler.folderLoading(folder, false);
            }
            super.synchronizeMailboxFailed(account, folder, message);
        }

        @Override
        public void folderStatusChanged(Account account, String folder, int unreadMessageCount) {
            if (isSingleAccountMode() && isSingleFolderMode() && mAccount.equals(account) &&
                    mFolderName.equals(folder)) {
                mUnreadMessageCount = unreadMessageCount;
            }
            super.folderStatusChanged(account, folder, unreadMessageCount);
        }

        private boolean updateForMe(Account account, String folder) {
            if (account == null || folder == null) {
                return false;
            }

            if (!Utility.arrayContains(mAccountUuids, account.getUuid())) {
                return false;
            }

            List<String> folderNames = mSearch.getFolderNames();
            return (folderNames.isEmpty() || folderNames.contains(folder));
        }
    }

   /* public class MessageListLoaderCallback implements LoaderManager.LoaderCallbacks<Cursor> {
        private final Context context;
        private final Preferences mPreferences;

        public MessageListLoaderCallback(Context context) {
            this.context = context;
            this.mPreferences = Preferences.getPreferences(context);
        }

        @Override
        public Loader<Cursor> onCreateLoader(int id, Bundle args) {
            String accountUuid = mAccountUuids[id];
            Account account = mPreferences.getAccount(accountUuid);

            String threadId = getThreadId(mSearch);

            Uri uri;
            String[] projection = null;
            boolean needConditions;
            if (threadId != null) {
                uri = Uri.withAppendedPath(EmailProvider.CONTENT_URI, "account/" + accountUuid + "/thread/" + threadId);
                //projection = PROJECTION;
                needConditions = false;
            } else if (mThreadedList) {
                uri = Uri.withAppendedPath(EmailProvider.CONTENT_URI, "account/" + accountUuid + "/messages/threaded");
                //projection = THREADED_PROJECTION;
                needConditions = true;
            } else {
                uri = Uri.withAppendedPath(EmailProvider.CONTENT_URI, "account/" + accountUuid + "/messages");
                //projection = PROJECTION;
                needConditions = true;
            }

            StringBuilder query = new StringBuilder();
            List<String> queryArgs = new ArrayList<>();
            if (needConditions) {
                boolean selectActive = mActiveMessage != null && mActiveMessage.getAccountUuid().equals(accountUuid);

                if (selectActive) {
                    query.append("(" + EmailProvider.MessageColumns.UID + " = ? AND " + EmailProvider.SpecialColumns.FOLDER_NAME + " = ?) OR (");
                    queryArgs.add(mActiveMessage.getUid());
                    queryArgs.add(mActiveMessage.getFolderName());
                }

                SqlQueryBuilder.buildWhereClause(account, mSearch.getConditions(), query, queryArgs);

                if (selectActive) {
                    query.append(')');
                }
            }

            String selection = query.toString();
            String[] selectionArgs = queryArgs.toArray(new String[queryArgs.size()]);

            String sortOrder = buildSortOrder();

            return new CursorLoader(context, uri, projection, selection, selectionArgs,
                    sortOrder);
        }

        @Override
        public void onLoaderReset(Loader<Cursor> loader) {
            //mAdapter.swapCursor(null);
        }

        @Override
        public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
            if (mIsThreadDisplay && data.getCount() == 0) {
                mHandler.goBack();
                return;
            }

            // Remove the "Loading..." view
            mPullToRefreshView.setEmptyView(null);
            setPullToRefreshEnabled(isPullToRefreshAllowed());

            final int loaderId = loader.getId();
            mCursors[loaderId] = data;
            mCursorValid[loaderId] = true;

            Cursor cursor;
            if (mCursors.length > 1) {
                cursor = new MergeCursorWithUniqueId(mCursors, getComparator());
                mUniqueIdColumn = cursor.getColumnIndex("_id");
            } else {
                cursor = data;
                mUniqueIdColumn = ID_COLUMN;
            }

            messages = new ArrayList<>(cursor.getCount());
            for(cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                long folderId = cursor.getLong(cursor.getColumnIndex(MessageColumns.FOLDER_ID));
                String messageUid = cursor.getString(cursor.getColumnIndex(MessageColumns.UID));
                Account account = getAccountFromCursor(cursor);
                LocalFolder folder = FolderHelper.getFolderById(account, folderId);
                try {
                    messages.add(folder.getMessage(messageUid));
                } catch (MessagingException e) {
                    Log.e(K9.LOG_TAG, "error in onLoadFinished", e);
                }
            }

            if (mIsThreadDisplay && messages.size() > 0) {
                LocalMessage message = messages.get(0);
                mTitle = message.getSubject();
                if (!TextUtils.isEmpty(mTitle)) {
                    mTitle = Utility.stripSubject(mTitle);
                }

                if (TextUtils.isEmpty(mTitle)) {
                    mTitle = getString(R.string.general_no_subject);
                }

                updateTitle();
            }

            cleanupSelected();
            updateContextMenu();

            mAdapter.swapCursor(cursor);

            resetActionMode();
            computeBatchDirection();

            if (isLoadFinished()) {
                if (mSavedListState != null) {
                    mHandler.restoreListPosition();
                }

                mFragmentListener.updateMenu();
            }
        }

        public boolean isLoadFinished() {
            if (mCursorValid == null) {
                return false;
            }

            for (boolean cursorValid : mCursorValid) {
                if (!cursorValid) {
                    return false;
                }
            }

            return true;
        }

        private String getThreadId(LocalSearch search) {
            for (ConditionsTreeNode node : search.getLeafSet()) {
                SearchCondition condition = node.mCondition;
                if (condition.field == SearchSpecification.SearchField.THREAD_ID) {
                    return condition.value;
                }
            }

            return null;
        }

        private String buildSortOrder() {
            String sortColumn = EmailProvider.MessageColumns.ID;
            switch (mSortType) {
                case SORT_ARRIVAL: {
                    sortColumn = EmailProvider.MessageColumns.INTERNAL_DATE;
                    break;
                }
                case SORT_ATTACHMENT: {
                    sortColumn = "(" + EmailProvider.MessageColumns.ATTACHMENT_COUNT + " < 1)";
                    break;
                }
                case SORT_FLAGGED: {
                    sortColumn = "(" + EmailProvider.MessageColumns.FLAGGED + " != 1)";
                    break;
                }
                case SORT_SENDER: {
                    //FIXME
                    sortColumn = EmailProvider.MessageColumns.SENDER_LIST;
                    break;
                }
                case SORT_SUBJECT: {
                    sortColumn = EmailProvider.MessageColumns.SUBJECT + " COLLATE NOCASE";
                    break;
                }
                case SORT_UNREAD: {
                    sortColumn = EmailProvider.MessageColumns.READ;
                    break;
                }
                case SORT_DATE:
                default: {
                    sortColumn = EmailProvider.MessageColumns.DATE;
                }
            }

            String sortDirection = (mSortAscending) ? " ASC" : " DESC";
            String secondarySort;
            if (mSortType == Account.SortType.SORT_DATE || mSortType == Account.SortType.SORT_ARRIVAL) {
                secondarySort = "";
            } else {
                secondarySort = EmailProvider.MessageColumns.DATE + ((mSortDateAscending) ? " ASC, " : " DESC, ");
            }

            String sortOrder = sortColumn + sortDirection + ", " + secondarySort +
                    EmailProvider.MessageColumns.ID + " DESC";
            return sortOrder;
        }
    }*/

    public class ActionModeCallback implements ActionMode.Callback {
        private MenuItem mSelectAll;
        private MenuItem mMarkAsRead;
        private MenuItem mMarkAsUnread;
        private MenuItem mFlag;
        private MenuItem mUnflag;
        private final Account mAccount;
        private final Preferences mPreferences;
        private boolean mSingleAccountMode;


        public ActionModeCallback(Account mAccount, boolean mSingleAccountMode) {
            this.mAccount = mAccount;
            this.mPreferences = Preferences.getPreferences(K9.getApplication());
            this.mSingleAccountMode = mSingleAccountMode;
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
            switch (item.getItemId()) {
                case R.id.delete: {
                    List<LocalMessage> messages = getCheckedMessages();
                    //TODO: onDelete(messages);
                    mSelectedCount = 0;
                    break;
                }
                case R.id.mark_as_read: {
                    setFlagForSelected(Flag.SEEN, true);
                    break;
                }
                case R.id.mark_as_unread: {
                    setFlagForSelected(Flag.SEEN, false);
                    break;
                }
                case R.id.flag: {
                    setFlagForSelected(Flag.FLAGGED, true);
                    break;
                }
                case R.id.unflag: {
                    setFlagForSelected(Flag.FLAGGED, false);
                    break;
                }
                case R.id.select_all: {
                    selectAll();
                    break;
                }

                // only if the account supports this
                case R.id.archive: {
                    onArchive(getCheckedMessages());
                    mSelectedCount = 0;
                    break;
                }
                case R.id.spam: {
                    onSpam(getCheckedMessages());
                    mSelectedCount = 0;
                    break;
                }
                case R.id.move: {
                    onMove(getCheckedMessages());
                    mSelectedCount = 0;
                    break;
                }
                case R.id.copy: {
                    onCopy(getCheckedMessages());
                    mSelectedCount = 0;
                    break;
                }
            }
            if (mSelectedCount == 0) {
                mActionMode.finish();
            }

            return true;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            mActionMode = null;
            mSelectAll = null;
            mMarkAsRead = null;
            mMarkAsUnread = null;
            mFlag = null;
            mUnflag = null;
            setSelectionState(false);
        }

        /**
         * Get the set of account UUIDs for the selected messages.
         */
        private Set<String> getAccountUuidsForSelected() {
            int maxAccounts = mAccountUuids.length;
            Set<String> accountUuids = new HashSet<String>(maxAccounts);

            /*for (int position = 0, end = mAdapter.getCount(); position < end; position++) {
                Cursor cursor = (Cursor) mAdapter.getItem(position);
                long uniqueId = cursor.getLong(mUniqueIdColumn);

                if (mSelected.contains(uniqueId)) {
                    String accountUuid = cursor.getString(ACCOUNT_UUID_COLUMN);
                    accountUuids.add(accountUuid);

                    if (accountUuids.size() == mAccountUuids.length) {
                        break;
                    }
                }
            }*/

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
            if (mActionMode != null) {
                mSelectAll.setVisible(show);
            }
        }

        public void showMarkAsRead(boolean show) {
            if (mActionMode != null) {
                mMarkAsRead.setVisible(show);
                mMarkAsUnread.setVisible(!show);
            }
        }

        public void showFlag(boolean show) {
            if (mActionMode != null) {
                mFlag.setVisible(show);
                mUnflag.setVisible(!show);
            }
        }
    }

    private static class MessageItemViewOnTouchListener implements RecyclerView.OnItemTouchListener {
        private final Context context;
        private final View.OnClickListener clickListener;
        private final GestureDetector detector;

        public MessageItemViewOnTouchListener(Context context, View.OnClickListener clickListener) {
            this.context = context;
            this.clickListener = clickListener;
            detector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
                @Override
                public boolean onDown(MotionEvent e) {
                    return true;
                }

                @Override
                public boolean onSingleTapUp(MotionEvent e) {
                    return true;
                }
            });
        }

        @Override
        public boolean onInterceptTouchEvent(RecyclerView rv, MotionEvent e) {
            View view = rv.findChildViewUnder(e.getX(), e.getY());
            if(view instanceof MessageListItemView && detector.onTouchEvent(e)) {
                if(clickListener != null) {
                    clickListener.onClick(view);
                }

                return true;
            }

            return false;
        }

        @Override
        public void onTouchEvent(RecyclerView rv, MotionEvent e) {
        }

        @Override
        public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {
            K9.logDebug("onRequestDisallow");
        }
    }

    private class MessageItemViewOnClickListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.flagged: {
                    MessageListItemView itemView = (MessageListItemView)messageListView.findChildViewUnder(v.getX(), v.getY());
                    LocalMessage message = itemView.getMessage();
                    presenter.setFlag(message, Flag.FLAGGED);
                    break;
                }
                default: {
                    MessageListItemView itemView = (MessageListItemView)v;
                    LocalMessage message = itemView.getMessage();
                    presenter.openMessage(message.makeMessageReference());
                }
            }
        }
    }
}
