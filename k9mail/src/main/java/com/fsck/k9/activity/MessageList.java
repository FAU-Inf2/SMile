package com.fsck.k9.activity;
import android.app.AlertDialog;
import android.app.SearchManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.webkit.WebView;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.fsck.k9.Account;
import com.fsck.k9.Account.SortType;
import com.fsck.k9.K9;
import com.fsck.k9.adapter.DividerItemDecoration;
import com.fsck.k9.adapter.RecyclerViewAdapter;
import com.fsck.k9.helper.FolderHelper;
import com.fsck.k9.preferences.SplitViewMode;
import com.fsck.k9.Preferences;
import com.fsck.k9.crypto.PgpData;
import com.fsck.k9.fragment.MessageListFragment;
import com.fsck.k9.fragment.MessageListFragmentListener;
import com.fsck.k9.fragment.SmsListFragment;
import com.fsck.k9.mail.Address;
import com.fsck.k9.mailstore.LocalMessage;
import com.fsck.k9.mailstore.StorageManager;
import com.fsck.k9.search.ConditionsTreeNode;
import com.fsck.k9.search.LocalSearch;
import com.fsck.k9.search.SearchAccount;
import com.fsck.k9.search.SearchSpecification;
import com.fsck.k9.search.SearchSpecification.Attribute;
import com.fsck.k9.search.SearchCondition;
import com.fsck.k9.search.SearchSpecification.SearchField;
import com.fsck.k9.ui.messageview.MessageViewFragment;
import com.fsck.k9.ui.messageview.MessageViewFragmentListener;
import com.fsck.k9.view.AccountView;
import com.fsck.k9.view.MessageHeader;
import com.fsck.k9.view.ViewSwitcher;
import com.fsck.k9.view.ViewSwitcher.OnSwitchCompleteListener;

import java.util.Calendar;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.TreeSet;

import butterknife.ButterKnife;
import de.cketti.library.changelog.ChangeLog;
import de.fau.cs.mad.smile.android.R;

import static butterknife.ButterKnife.findById;


/**
 * MessageList is the primary user interface for the program. This Activity
 * shows a list of messages.
 * From this Activity the user can perform all standard message operations.
 */
public class MessageList extends K9Activity
        implements MessageListFragmentListener,
        MessageViewFragmentListener,
        FragmentManager.OnBackStackChangedListener,
        OnSwitchCompleteListener {

    // for this activity
    private static final String EXTRA_SEARCH = "search";
    private static final String EXTRA_NO_THREADING = "no_threading";

    private static final String ACTION_SHORTCUT = "shortcut";
    private static final String EXTRA_SPECIAL_FOLDER = "special_folder";

    private static final String EXTRA_MESSAGE_REFERENCE = "message_reference";

    // used for remote search
    public static final String EXTRA_SEARCH_ACCOUNT = "com.fsck.k9.search_account";
    private static final String EXTRA_SEARCH_FOLDER = "com.fsck.k9.search_folder";

    private static final String STATE_DISPLAY_MODE = "displayMode";
    private static final String STATE_MESSAGE_LIST_WAS_DISPLAYED = "messageListWasDisplayed";

    // Used for navigating to next/previous message
    private static final int PREVIOUS = 1;
    private static final int NEXT = 2;


    public static void actionDisplaySearch(Context context,
                                           SearchSpecification search,
                                           boolean noThreading,
                                           boolean newTask) {
        actionDisplaySearch(context, search, noThreading, newTask, true);
    }

    public static void actionDisplaySearch(Context context,
                                           SearchSpecification search,
                                           boolean noThreading,
                                           boolean newTask,
                                           boolean clearTop) {
        context.startActivity(intentDisplaySearch(context, search, noThreading, newTask, clearTop));
    }

    public static Intent intentDisplaySearch(Context context, SearchSpecification search,
                                             boolean noThreading, boolean newTask, boolean clearTop) {
        Intent intent = new Intent(context, MessageList.class);
        intent.putExtra(EXTRA_SEARCH, search);
        intent.putExtra(EXTRA_NO_THREADING, noThreading);

        if (clearTop) {
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        }

        if (newTask) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }

        return intent;
    }

    public static Intent shortcutIntent(Context context, String specialFolder) {
        Intent intent = new Intent(context, MessageList.class);
        intent.setAction(ACTION_SHORTCUT);
        intent.putExtra(EXTRA_SPECIAL_FOLDER, specialFolder);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        return intent;
    }

    public static Intent actionDisplayMessageIntent(Context context,
                                                    MessageReference messageReference) {
        Intent intent = new Intent(context, MessageList.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra(EXTRA_MESSAGE_REFERENCE, messageReference);
        return intent;
    }

    private enum DisplayMode {
        MESSAGE_LIST,
        MESSAGE_VIEW,
        SMS_LIST,
        SPLIT_VIEW
    }

    private StorageManager.StorageListener mStorageListener = new StorageListenerImplementation();

    // Name and email in HeaderView -- TODO: for SMile-UI -> get from resources
    String mName;
    String mEmail;
    //titles and icons for ListView
    int mIcons[] = {R.drawable.ic_inbox_black_24dp, R.drawable.ic_send_black_24dp,
            R.drawable.ic_drafts_black_24dp, R.drawable.ic_delete_black_24dp,
            R.drawable.ic_list_black_24dp, R.drawable.ic_settings_black_24dp,
            R.drawable.ic_info_black_24dp};
    String mTitles[];

    private Toolbar toolbar;
    private ActionBar actionBar;
    private TextView mActionBarUnread;
    private Menu mMenu;

    private RecyclerView mRecyclerView;
    private RecyclerView.Adapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;
    private DrawerLayout mDrawer;
    private ActionBarDrawerToggle mDrawerToggle;

    private ViewGroup mMessageViewContainer;
    private View mMessageViewPlaceHolder;

    private MessageListFragment mMessageListFragment;
    private MessageViewFragment mMessageViewFragment;
    private int mFirstBackStackId = -1;

    private LinearLayout leftLinearLayoutContacts;

    private Account mAccount;
    private String mFolderName;
    private LocalSearch mSearch;
    private boolean mSingleFolderMode;
    private boolean mSingleAccountMode;

    private ProgressBar mActionBarProgress;
    private MenuItem mMenuButtonCheckMail;
    private View mActionButtonIndeterminateProgress;
    private int mLastDirection = (K9.messageViewShowNext()) ? NEXT : PREVIOUS;

    /**
     * {@code true} if the message list should be displayed as flat list (i.e. no threading)
     * regardless whether or not message threading was enabled in the settings. This is used for
     * filtered views, e.g. when only displaying the unread messages in a folder.
     */
    private boolean mNoThreading;

    private DisplayMode mDisplayMode;
    private MessageReference mMessageReference;

    /**
     * {@code true} when the message list was displayed once. This is used in
     * {@link #onBackPressed()} to decide whether to go from the message view to the message list or
     * finish the activity.
     */
    private boolean mMessageListWasDisplayed = false;
    private ViewSwitcher mViewSwitcher;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (UpgradeDatabases.actionUpgradeDatabases(this, getIntent())) {
            finish();
            return;
        }

        if (useSplitView()) {
            setContentView(R.layout.split_message_list);
        } else {
            setContentView(R.layout.folder);
            mViewSwitcher = (ViewSwitcher) findViewById(R.id.container);
            mViewSwitcher.setFirstInAnimation(AnimationUtils.loadAnimation(this, R.anim.slide_in_left));
            mViewSwitcher.setFirstOutAnimation(AnimationUtils.loadAnimation(this, R.anim.slide_out_right));
            mViewSwitcher.setSecondInAnimation(AnimationUtils.loadAnimation(this, R.anim.slide_in_right));
            mViewSwitcher.setSecondOutAnimation(AnimationUtils.loadAnimation(this, R.anim.slide_out_left));
            mViewSwitcher.setThirdInAnimation(AnimationUtils.loadAnimation(this, R.anim.slide_in_left));
            mViewSwitcher.setThirdOutAnimation(AnimationUtils.loadAnimation(this, R.anim.slide_out_left));
            mViewSwitcher.setOnSwitchCompleteListener(this);
        }

        ButterKnife.bind(this);

        // Enable gesture detection for MessageLists
        //setupGestureDetector(this);

        if (!decodeExtras(getIntent())) {
            return;
        }

        initializeActionBar();
        initializeNavigationDrawer();

        findFragments();
        initializeDisplayMode(savedInstanceState);
        initializeLayout();
        initializeFragments();
        displayViews();

        ChangeLog cl = new ChangeLog(this);
        if (cl.isFirstRun()) {
            cl.getLogDialog().show();
        }
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        mDrawerToggle.syncState();
    }

    @Override
    public void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        setIntent(intent);

        if (mFirstBackStackId >= 0) {
            getFragmentManager().popBackStackImmediate(mFirstBackStackId,
                    FragmentManager.POP_BACK_STACK_INCLUSIVE);
            mFirstBackStackId = -1;
        }

        removeMessageListFragment();
        removeMessageViewFragment();

        mMessageReference = null;
        mSearch = null;
        mFolderName = null;

        if (!decodeExtras(intent)) {
            return;
        }

        initializeDisplayMode(null);
        initializeFragments();
        displayViews();
    }

    /**
     * Get references to existing fragments if the activity was restarted.
     */
    private void findFragments() {
        FragmentManager fragmentManager = getSupportFragmentManager();
        mMessageListFragment = (MessageListFragment) fragmentManager.findFragmentById(
                R.id.message_list_container);
        mMessageViewFragment = (MessageViewFragment) fragmentManager.findFragmentById(
                R.id.message_view_container);
    }

    /**
     * Create fragment instances if necessary.
     *
     * @see #findFragments()
     */
    private void initializeFragments() {
        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.addOnBackStackChangedListener(this);

        boolean hasMessageListFragment = (mMessageListFragment != null);

        if (!hasMessageListFragment) {
            FragmentTransaction ft = fragmentManager.beginTransaction();
            mMessageListFragment = MessageListFragment.newInstance(mSearch, false,
                    (K9.isThreadedViewEnabled() && !mNoThreading));
            ft.add(R.id.message_list_container, mMessageListFragment);
            ft.commit();
        }

        // Check if the fragment wasn't restarted and has a MessageReference in the arguments. If
        // so, open the referenced message.
        if (!hasMessageListFragment && mMessageViewFragment == null &&
                mMessageReference != null) {
            openMessage(mMessageReference);
        }
    }

    /**
     * Set the initial display mode (message list, message view, or split view).
     * <p/>
     * <p><strong>Note:</strong>
     * This method has to be called after {@link #findFragments()} because the result depends on
     * the availability of a {@link MessageViewFragment} instance.
     * </p>
     *
     * @param savedInstanceState The saved instance state that was passed to the activity as argument to
     *                           {@link #onCreate(Bundle)}. May be {@code null}.
     */
    private void initializeDisplayMode(Bundle savedInstanceState) {
        if (useSplitView()) {
            mDisplayMode = DisplayMode.SPLIT_VIEW;
            return;
        }

        if (savedInstanceState != null) {
            DisplayMode savedDisplayMode =
                    (DisplayMode) savedInstanceState.getSerializable(STATE_DISPLAY_MODE);
            if (savedDisplayMode != DisplayMode.SPLIT_VIEW) {
                mDisplayMode = savedDisplayMode;
                return;
            }
        }

        if (mMessageViewFragment != null || mMessageReference != null) {
            mDisplayMode = DisplayMode.MESSAGE_VIEW;
        } else {
            mDisplayMode = DisplayMode.MESSAGE_LIST;
        }
    }

    private boolean useSplitView() {
        SplitViewMode splitViewMode = K9.getSplitViewMode();
        int orientation = getResources().getConfiguration().orientation;

        return (splitViewMode == SplitViewMode.ALWAYS ||
                (splitViewMode == SplitViewMode.WHEN_IN_LANDSCAPE &&
                        orientation == Configuration.ORIENTATION_LANDSCAPE));
    }

    private void initializeLayout() {
        mMessageViewContainer = (ViewGroup) findViewById(R.id.message_view_container);
        mMessageViewPlaceHolder = getLayoutInflater().inflate(R.layout.empty_message_view, null);
    }

    private void displayViews() {
        switch (mDisplayMode) {
            case MESSAGE_LIST: {
                showMessageList();
                break;
            }
            case MESSAGE_VIEW: {
                showMessageView();
                break;
            }
            case SPLIT_VIEW: {
                mMessageListWasDisplayed = true;
                if (mMessageViewFragment == null) {
                    showMessageViewPlaceHolder();
                } else {
                    MessageReference activeMessage = mMessageViewFragment.getMessageReference();
                    if (activeMessage != null) {
                        mMessageListFragment.setActiveMessage(activeMessage);
                    }
                }
                break;
            }
        }
    }

    private boolean decodeExtras(Intent intent) {
        String action = intent.getAction();

        if (Intent.ACTION_VIEW.equals(action) && intent.getData() != null) {
            decodeExtraActionView(intent);
        } else if (ACTION_SHORTCUT.equals(action)) {
            decodeExtraActionShortcut(intent);
        } else if (intent.getStringExtra(SearchManager.QUERY) != null) {
            decodeExtraActionSearch(intent);
        } else {
            // regular LocalSearch object was passed
            mSearch = intent.getParcelableExtra(EXTRA_SEARCH);
            mNoThreading = intent.getBooleanExtra(EXTRA_NO_THREADING, false);
        }

        if (mMessageReference == null) {
            mMessageReference = intent.getParcelableExtra(EXTRA_MESSAGE_REFERENCE);
        }

        if (mMessageReference != null) {
            mSearch = new LocalSearch();
            mSearch.addAccountUuid(mMessageReference.getAccountUuid());
            mSearch.addAllowedFolder(mMessageReference.getFolderName());
        }

        if (mSearch == null) {
            // We've most likely been started by an old unread widget
            String accountUuid = intent.getStringExtra("account");
            String folderName = intent.getStringExtra("folder");

            mSearch = new LocalSearch(folderName);
            mSearch.addAccountUuid((accountUuid == null) ? "invalid" : accountUuid);
            if (folderName != null) {
                mSearch.addAllowedFolder(folderName);
            }
        }

        Preferences prefs = Preferences.getPreferences(getApplicationContext());
        String[] accountUuids = mSearch.getAccountUuids();

        Log.d(K9.LOG_TAG, "MessageList.decodeExtras Account: " + mAccount);

        if (mSearch.searchAllAccounts()) {
            List<Account> accounts = prefs.getAccounts();
            mSingleAccountMode = (accounts.size() == 1);

            if (mSingleAccountMode) {
                mAccount = accounts.get(0);
            }
        } else {
            mSingleAccountMode = (accountUuids.length == 1);
            if (mSingleAccountMode) {
                mAccount = prefs.getAccount(accountUuids[0]);
            }
        }

        mSingleFolderMode = mSingleAccountMode && (mSearch.getFolderNames().size() == 1);

        if (mSingleAccountMode && (mAccount == null || !mAccount.isAvailable(this))) {
            Log.i(K9.LOG_TAG, "not opening MessageList of unavailable account");
            onAccountUnavailable();
            return false;
        }

        if (mSingleFolderMode) {
            mFolderName = mSearch.getFolderNames().get(0);
        }

        // now we know if we are in single account mode and need a subtitle
        //mActionBarSubTitle.setVisibility((!mSingleFolderMode) ? View.GONE : View.VISIBLE);

        return true;
    }

    private void decodeExtraActionSearch(Intent intent) {
        // check if this intent comes from the system search ( remote )
        if (!Intent.ACTION_SEARCH.equals(intent.getAction())) {
            return;
        }
        //Query was received from Search Dialog
        String query = intent.getStringExtra(SearchManager.QUERY).trim();

        mSearch = new LocalSearch(getString(R.string.search_results));
        mSearch.setManualSearch(true);
        mNoThreading = true;

        mSearch.or(new SearchCondition(SearchField.SENDER, Attribute.CONTAINS, query));
        mSearch.or(new SearchCondition(SearchField.SUBJECT, Attribute.CONTAINS, query));
        // FIXME: SqlQueryBuilder throws RTE
        //mSearch.or(new SearchCondition(SearchField.MESSAGE_CONTENTS, Attribute.CONTAINS, query));

        Bundle appData = intent.getBundleExtra(SearchManager.APP_DATA);
        if (appData != null) {
            mSearch.addAccountUuid(appData.getString(EXTRA_SEARCH_ACCOUNT));
            // searches started from a folder list activity will provide an account, but no folder
            if (appData.getString(EXTRA_SEARCH_FOLDER) != null) {
                mSearch.addAllowedFolder(appData.getString(EXTRA_SEARCH_FOLDER));
            }
        } else {
            mSearch.addAccountUuid(LocalSearch.ALL_ACCOUNTS);
        }
    }

    private void decodeExtraActionShortcut(Intent intent) {
        // Handle shortcut intents
        String specialFolder = intent.getStringExtra(EXTRA_SPECIAL_FOLDER);
        if (SearchAccount.UNIFIED_INBOX.equals(specialFolder)) {
            mSearch = SearchAccount.createUnifiedInboxAccount(this).getRelatedSearch();
        } else if (SearchAccount.ALL_MESSAGES.equals(specialFolder)) {
            mSearch = SearchAccount.createAllMessagesAccount(this).getRelatedSearch();
        }
    }

    private void decodeExtraActionView(Intent intent) {
        Uri uri = intent.getData();
        List<String> segmentList = uri.getPathSegments();
        String accountId = segmentList.get(0);
        Collection<Account> accounts = Preferences.getPreferences(this).getAvailableAccounts();

        for (Account account : accounts) {
            if (String.valueOf(account.getAccountNumber()).equals(accountId)) {
                String folderName = segmentList.get(1);
                String messageUid = segmentList.get(2);
                mMessageReference = new MessageReference(account.getUuid(), folderName, messageUid, null);
                break;
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        StorageManager.getInstance(getApplication()).removeListener(mStorageListener);
    }

    @Override
    public void onResume() {
        super.onResume();

        /*TODO: if (!(this instanceof Search)) {
            //necessary b/c no guarantee Search.onStop will be called before MessageList.onResume
            //when returning from search results
            Search.setActive(false);
        }*/

        if (mAccount != null && !mAccount.isAvailable(this)) {
            onAccountUnavailable();
            return;
        }

        StorageManager.getInstance(getApplication()).addListener(mStorageListener);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putSerializable(STATE_DISPLAY_MODE, mDisplayMode);
        outState.putBoolean(STATE_MESSAGE_LIST_WAS_DISPLAYED, mMessageListWasDisplayed);
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        mMessageListWasDisplayed = savedInstanceState.getBoolean(STATE_MESSAGE_LIST_WAS_DISPLAYED);
    }

    private void initializeActionBar() {
        toolbar = (Toolbar) findViewById(R.id.toolbar);

        if(toolbar != null) {
            setSupportActionBar(toolbar);
        }

        actionBar = getSupportActionBar();

        if(actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeButtonEnabled(true);
        }

        mActionBarUnread = (TextView) toolbar.findViewById(R.id.actionbar_unread_count);
        mActionBarProgress = (ProgressBar) toolbar.findViewById(R.id.actionbar_progress);
        mActionButtonIndeterminateProgress =
                getLayoutInflater().inflate(R.layout.actionbar_indeterminate_progress_actionview, null);
    }

    private void initializeNavigationDrawer() {
        final AccountView accountView = findById(this, R.id.account_view);
        mRecyclerView = (RecyclerView) findViewById(R.id.RecyclerView);
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.addItemDecoration(new DividerItemDecoration(this));

        mTitles = new String[7];
        if(mAccount != null) {
            accountView.setCurrentAccount(mAccount);
            accountView.setAccountSpinnerListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    final Account selectedAccount = (Account) parent.getItemAtPosition(position);
                    accountView.setCurrentAccount(selectedAccount);
                    showMessageViewPlaceHolder();

                    LocalSearch tmpSearch = new LocalSearch();
                    tmpSearch.addAllowedFolder(selectedAccount.getAutoExpandFolderName());
                    tmpSearch.addAccountUuid(selectedAccount.getUuid());
                    MessageListFragment fragment =MessageListFragment.newInstance(tmpSearch, false,
                            (K9.isThreadedViewEnabled() && !mNoThreading));
                    addMessageListFragment(fragment, true);
                    mDrawer.closeDrawers();
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {

                }
            });
            mTitles[0] = mAccount.getInboxFolderName();
            mTitles[1] = mAccount.getSentFolderName();
            mTitles[2] = mAccount.getDraftsFolderName();
            mTitles[3] = mAccount.getTrashFolderName();
            mTitles[4] = getResources().getString(R.string.folder_list);
            mTitles[5] = getResources().getString(R.string.preferences_title);
            mTitles[6] = getResources().getString(R.string.about_action) + " " + getResources().getString(R.string.app_name);

            mName = mAccount.getName();
            mEmail = mAccount.getEmail();
        } else {
            mTitles[0] = getResources().getString(R.string.special_mailbox_name_inbox);
            mTitles[1] = getResources().getString(R.string.special_mailbox_name_sent);
            mTitles[2] = getResources().getString(R.string.special_mailbox_name_drafts);
            mTitles[3] = getResources().getString(R.string.special_mailbox_name_trash);
            mTitles[4] = getResources().getString(R.string.folder_list);
            mTitles[5] = getResources().getString(R.string.preferences_title);
            mTitles[6] = getResources().getString(R.string.about_action) + " " + getResources().getString(R.string.app_name);

            //TODO: just a workaround to display something
            mName = getString(R.string.app_name);
            mEmail = getString(R.string.app_name);
        }

        mAdapter = new RecyclerViewAdapter(mTitles, mIcons);
        mRecyclerView.setAdapter(mAdapter);

        final GestureDetector mGestureDetector = new GestureDetector(MessageList.this,
                new GestureDetector.SimpleOnGestureListener() {
                    @Override
                    public boolean onSingleTapUp(MotionEvent e) {
                        return true;
                    }
                });

        mRecyclerView.addOnItemTouchListener(new RecyclerView.OnItemTouchListener() {
            @Override
            public boolean onInterceptTouchEvent(RecyclerView recyclerView, MotionEvent motionEvent) {
                View child = recyclerView.findChildViewUnder(motionEvent.getX(), motionEvent.getY());

                if (child != null && mGestureDetector.onTouchEvent(motionEvent)) {
                    mDrawer.closeDrawers();

                    int position = recyclerView.getChildPosition(child);
                    switch(position) {
                        case 0:
                            if(mAccount != null)
                                onOpenFolder(mAccount.getInboxFolderName());
                            break;
                        case 1:
                            if(mAccount != null)
                                onOpenFolder(mAccount.getSentFolderName());
                            break;
                        case 2:
                            if(mAccount != null)
                                onOpenFolder(mAccount.getDraftsFolderName());
                            break;
                        case 3:
                            if(mAccount != null)
                                onOpenFolder(mAccount.getTrashFolderName());
                            break;
                        case 4:
                            if(mAccount != null)
                                onShowFolderList();
                            break;
                        case 5:
                            onEditPrefs();
                            break;
                        case 6:
                            onAbout();
                            break;
                        default:
                            break;
                    }
                    return true;
                }
                return false;
            }

            @Override
            public void onTouchEvent(RecyclerView recyclerView, MotionEvent motionEvent) {
            }

            @Override
            public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {
            }
        });

        mLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(mLayoutManager);
        mDrawer = (DrawerLayout) findViewById(R.id.DrawerLayout);
        mDrawerToggle = new ActionBarDrawerToggle(this, mDrawer, R.string.app_name,
                R.string.app_name) { //TODO: set correct strings
            @Override
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
            }

            @Override
            public void onDrawerClosed(View drawerView) {
                super.onDrawerClosed(drawerView);
            }
        };
        mDrawer.setDrawerListener(mDrawerToggle);
        mDrawerToggle.syncState();
    }

    private void onOpenFolder(final String folder) {
        LocalSearch search = new LocalSearch(folder);
        search.addAccountUuid(mAccount.getUuid());
        search.addAllowedFolder(folder);
        MessageList.actionDisplaySearch(this, search, false, false);
    }


    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mDrawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        boolean ret = false;
        if (KeyEvent.ACTION_DOWN == event.getAction()) {
            ret = onCustomKeyDown(event.getKeyCode(), event);
        }

        if (!ret) {
            ret = super.dispatchKeyEvent(event);
        }

        return ret;
    }

    @Override
    public void onBackPressed() {
        if (mDisplayMode == DisplayMode.MESSAGE_VIEW && mMessageListWasDisplayed) {
            showMessageList();
            mDrawerToggle.setDrawerIndicatorEnabled(true);
        } else {
            super.onBackPressed();
        }
    }

    /**
     * Handle hotkeys
     * <p/>
     * <p>
     * This method is called by {@link #dispatchKeyEvent(KeyEvent)} before any view had the chance
     * to consume this key event.
     * </p>
     *
     * @param keyCode The value in {@code event.getKeyCode()}.
     * @param event   Description of the key event.
     * @return {@code true} if this event was consumed.
     */
    public boolean onCustomKeyDown(final int keyCode, final KeyEvent event) {
        // TODO: Refactor with command pattern?
        switch (keyCode) {
            case KeyEvent.KEYCODE_VOLUME_UP: {
                if (mMessageViewFragment != null && mDisplayMode != DisplayMode.MESSAGE_LIST &&
                        K9.useVolumeKeysForNavigationEnabled()) {
                    showPreviousMessage();
                    return true;
                } else if (mDisplayMode != DisplayMode.MESSAGE_VIEW &&
                        K9.useVolumeKeysForListNavigationEnabled()) {
                    mMessageListFragment.onMoveUp();
                    return true;
                }

                break;
            }
            case KeyEvent.KEYCODE_VOLUME_DOWN: {
                if (mMessageViewFragment != null && mDisplayMode != DisplayMode.MESSAGE_LIST &&
                        K9.useVolumeKeysForNavigationEnabled()) {
                    showNextMessage();
                    return true;
                } else if (mDisplayMode != DisplayMode.MESSAGE_VIEW &&
                        K9.useVolumeKeysForListNavigationEnabled()) {
                    mMessageListFragment.onMoveDown();
                    return true;
                }

                break;
            }
            case KeyEvent.KEYCODE_C: {
                mMessageListFragment.onCompose();
                return true;
            }
            case KeyEvent.KEYCODE_Q: {
                if (mMessageListFragment != null && mMessageListFragment.isSingleAccountMode()) {
                    onShowFolderList();
                }
                return true;
            }
            case KeyEvent.KEYCODE_O: {
                mMessageListFragment.onCycleSort();
                return true;
            }
            case KeyEvent.KEYCODE_I: {
                mMessageListFragment.onReverseSort();
                return true;
            }
            case KeyEvent.KEYCODE_DEL:
            case KeyEvent.KEYCODE_D: {
                if (mDisplayMode == DisplayMode.MESSAGE_LIST) {
                    mMessageListFragment.onDelete();
                } else if (mMessageViewFragment != null) {
                    mMessageViewFragment.onDelete();
                }
                return true;
            }
            case KeyEvent.KEYCODE_S: {
                mMessageListFragment.toggleMessageSelect();
                return true;
            }
            case KeyEvent.KEYCODE_G: {
                if (mDisplayMode == DisplayMode.MESSAGE_LIST) {
                    mMessageListFragment.onToggleFlagged();
                } else if (mMessageViewFragment != null) {
                    mMessageViewFragment.onToggleFlagged();
                }
                return true;
            }
            case KeyEvent.KEYCODE_M: {
                if (mDisplayMode == DisplayMode.MESSAGE_LIST) {
                    mMessageListFragment.onMove();
                } else if (mMessageViewFragment != null) {
                    mMessageViewFragment.onMove();
                }
                return true;
            }
            case KeyEvent.KEYCODE_V: {
                if (mDisplayMode == DisplayMode.MESSAGE_LIST) {
                    mMessageListFragment.onArchive();
                } else if (mMessageViewFragment != null) {
                    mMessageViewFragment.onArchive();
                }
                return true;
            }
            case KeyEvent.KEYCODE_Y: {
                if (mDisplayMode == DisplayMode.MESSAGE_LIST) {
                    mMessageListFragment.onCopy();
                } else if (mMessageViewFragment != null) {
                    mMessageViewFragment.onCopy();
                }
                return true;
            }
            case KeyEvent.KEYCODE_Z: {
                if (mDisplayMode == DisplayMode.MESSAGE_LIST) {
                    mMessageListFragment.onToggleRead();
                } else if (mMessageViewFragment != null) {
                    mMessageViewFragment.onToggleRead();
                }
                return true;
            }
            case KeyEvent.KEYCODE_F: {
                if (mMessageViewFragment != null) {
                    mMessageViewFragment.onForward();
                }
                return true;
            }
            case KeyEvent.KEYCODE_A: {
                if (mMessageViewFragment != null) {
                    mMessageViewFragment.onReplyAll();
                }
                return true;
            }
            case KeyEvent.KEYCODE_R: {
                if (mMessageViewFragment != null) {
                    mMessageViewFragment.onReply();
                }
                return true;
            }
            case KeyEvent.KEYCODE_J:
            case KeyEvent.KEYCODE_P: {
                if (mMessageViewFragment != null) {
                    showPreviousMessage();
                }
                return true;
            }
            case KeyEvent.KEYCODE_N:
            case KeyEvent.KEYCODE_K: {
                if (mMessageViewFragment != null) {
                    showNextMessage();
                }
                return true;
            }
            case KeyEvent.KEYCODE_H: {
                Toast toast = Toast.makeText(this, R.string.message_list_help_key, Toast.LENGTH_LONG);
                toast.show();
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        // Swallow these events too to avoid the audible notification of a volume change
        if (K9.useVolumeKeysForListNavigationEnabled()) {
            if ((keyCode == KeyEvent.KEYCODE_VOLUME_UP) || (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN)) {
                if (K9.DEBUG)
                    Log.v(K9.LOG_TAG, "Swallowed key up.");
                return true;
            }
        }

        return super.onKeyUp(keyCode, event);
    }

    private void onAccounts() {
        Accounts.listAccounts(this);
        finish();
    }

    private void onShowFolderList() {
        FolderList.actionHandleAccount(this, mAccount);
        finish();
    }

    private void onEditPrefs() {
        Settings.actionPreferences(this);
    }

    private void onEditAccount() {
        Settings.actionAccountPreferences(this, mAccount);
    }

    //copied from Accounts.java
    private void onAbout() {
        String appName = getString(R.string.app_name);
        int year = Calendar.getInstance().get(Calendar.YEAR);
        WebView wv = new WebView(this);
        StringBuilder html = new StringBuilder()
                .append("<meta http-equiv=\"content-type\" content=\"text/html; charset=utf-8\" />")
                .append("<img src=\"file:///android_asset/icon.png\" alt=\"").append(appName).append("\"/>")
                .append("<h1>")
                .append(String.format(getString(R.string.about_title_fmt),
                        "<a href=\"" + getString(R.string.app_webpage_url)) + "\">")
                .append(appName)
                .append("</a>")
                .append("</h1><p>")
                .append(appName)
                .append(" ")
                .append(String.format(getString(R.string.debug_version_fmt), getVersionNumber()))
                .append("</p><p>")
                .append(String.format(getString(R.string.app_authors_fmt),
                        getString(R.string.app_authors)))
                .append("</p><p>")
                .append(String.format(getString(R.string.app_revision_fmt),
                        "<a href=\"" + getString(R.string.app_revision_url) + "\">" +
                                getString(R.string.app_revision_url) +
                                "</a>"))
                .append("</p><hr/><p>")
                .append(String.format(getString(R.string.app_copyright_fmt), year, year))
                .append("</p><hr/><p>")
                .append(getString(R.string.app_license))
                .append("</p><hr/><p>");

        StringBuilder libs = new StringBuilder().append("<ul>");
        for (String[] library : K9.USED_LIBRARIES) {
            libs.append("<li><a href=\"").append(library[1]).append("\">").append(library[0]).append("</a></li>");
        }
        libs.append("</ul>");

        html.append(String.format(getString(R.string.app_libraries), libs.toString()))
                .append("</p><hr/><p>")
                .append(String.format(getString(R.string.app_emoji_icons),
                        "<div>TypePad \u7d75\u6587\u5b57\u30a2\u30a4\u30b3\u30f3\u753b\u50cf " +
                                "(<a href=\"http://typepad.jp/\">Six Apart Ltd</a>) / " +
                                "<a href=\"http://creativecommons.org/licenses/by/2.1/jp/\">CC BY 2.1</a></div>"))
                .append("</p><hr/><p>")
                .append(getString(R.string.app_htmlcleaner_license));


        wv.loadDataWithBaseURL("file:///android_res/drawable/", html.toString(), "text/html", "utf-8", null);
        new AlertDialog.Builder(this)
                .setView(wv)
                .setCancelable(true)
                .setPositiveButton(R.string.okay_action, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface d, int c) {
                        d.dismiss();
                    }
                })
                .setNeutralButton(R.string.changelog_full_title, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface d, int c) {
                        new ChangeLog(MessageList.this).getFullLogDialog().show();
                    }
                })
                .show();
    }
    /**
     * Get current version number.
     *
     * @return String version
     */
    private String getVersionNumber() {
        String version = "?";
        try {
            PackageInfo pi = getPackageManager().getPackageInfo(getPackageName(), 0);
            version = pi.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            //Log.e(TAG, "Package name not found", e);
        }
        return version;
    }

    @Override
    public boolean onSearchRequested() {
        return mMessageListFragment.onSearchRequested();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }

        final int itemId = item.getItemId();

        switch (itemId) {
            // implements up navigation
            case android.R.id.home: {
                mDrawerToggle.setDrawerIndicatorEnabled(true);
                goBack();
                return true;
            }
            case R.id.compose: {
                mMessageListFragment.onCompose();
                return true;
            }
            // MessageList
            case R.id.check_mail: {
                mMessageListFragment.checkMail();
                if (mDisplayMode == DisplayMode.SMS_LIST) fillContacts(mMessageListFragment);
                return true;
            }
            /*case R.id.select_all: {
                mMessageListFragment.selectAll();
                return true;
            }*/
            case R.id.settings: {
                onEditPrefs();
                return true;
            }
            case R.id.search: {
                mMessageListFragment.onSearchRequested();
                return true;
            }
            case R.id.search_remote: {
                mMessageListFragment.onRemoteSearch();
                return true;
            }
            case R.id.mark_all_as_read: {
                mMessageListFragment.markAllAsRead();
                return true;
            }
            case R.id.show_folder_list: {
                onShowFolderList();
                return true;
            }
            // MessageView
            case R.id.next_message: {
                showNextMessage();
                return true;
            }
            case R.id.previous_message: {
                showPreviousMessage();
                return true;
            }
            case R.id.delete: {
                mMessageViewFragment.onDelete();
                return true;
            }
            case R.id.reply: {
                mMessageViewFragment.onReply();
                return true;
            }
            case R.id.reply_all: {
                mMessageViewFragment.onReplyAll();
                return true;
            }
            case R.id.forward: {
                mMessageViewFragment.onForward();
                return true;
            }
            case R.id.share: {
                mMessageViewFragment.onSendAlternate();
                return true;
            }
            case R.id.toggle_unread: {
                mMessageViewFragment.onToggleRead();
                return true;
            }
            case R.id.archive:
            case R.id.refile_archive: {
                mMessageViewFragment.onArchive();
                return true;
            }
            case R.id.spam:
            case R.id.refile_spam: {
                mMessageViewFragment.onSpam();
                return true;
            }
            case R.id.move:
            case R.id.refile_move: {
                mMessageViewFragment.onMove();
                return true;
            }
            case R.id.remindme: {
                mMessageViewFragment.onRemindMe();
                return true;
            }
            case R.id.copy:
            case R.id.refile_copy: {
                mMessageViewFragment.onCopy();
                return true;
            }
            case R.id.select_text: {
                mMessageViewFragment.onSelectText();
                return true;
            }
            case R.id.show_headers:
            case R.id.hide_headers: {
                mMessageViewFragment.onToggleAllHeadersView();
                updateMenu();
                return true;
            }
        }

        if (!mSingleFolderMode) {
            // None of the options after this point are "safe" for search results
            //TODO: This is not true for "unread" and "starred" searches in regular folders
            return false;
        }

        switch (itemId) {
            case R.id.send_messages: {
                mMessageListFragment.onSendPendingMessages();
                return true;
            }
            case R.id.expunge: {
                mMessageListFragment.onExpunge();
                return true;
            }
            default: {
                return super.onOptionsItemSelected(item);
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        getMenuInflater().inflate(R.menu.message_list_option, menu);
        mMenu = menu;
        mMenuButtonCheckMail = menu.findItem(R.id.check_mail);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(final Menu menu) {
        configureMenu(menu);
        return true;
    }

    /**
     * Hide menu items not appropriate for the current context.
     * <p/>
     * <p><strong>Note:</strong>
     * Please adjust the comments in {@code res/menu/message_list_option.xml} if you change the
     * visibility of a menu item in this method.
     * </p>
     *
     * @param menu The {@link Menu} instance that should be modified. May be {@code null}; in that case
     *             the method does nothing and immediately returns.
     */
    private void configureMenu(final Menu menu) {
        if (menu == null) {
            return;
        }

        // Set visibility of account/folder settings menu items
        menu.findItem(R.id.settings).setVisible(true);

        /*
         * Set visibility of menu items related to the message view
         */

        if (mDisplayMode == DisplayMode.MESSAGE_LIST
                || mMessageViewFragment == null
                || !mMessageViewFragment.isInitialized()) {
            menu.findItem(R.id.next_message).setVisible(false);
            menu.findItem(R.id.previous_message).setVisible(false);
            menu.findItem(R.id.single_message_options).setVisible(false);
            menu.findItem(R.id.delete).setVisible(false);
            menu.findItem(R.id.compose).setVisible(false);
            menu.findItem(R.id.archive).setVisible(false);
            menu.findItem(R.id.move).setVisible(false);
            menu.findItem(R.id.copy).setVisible(false);
            menu.findItem(R.id.spam).setVisible(false);
            menu.findItem(R.id.refile).setVisible(false);
            menu.findItem(R.id.toggle_unread).setVisible(false);
            menu.findItem(R.id.select_text).setVisible(false);
            menu.findItem(R.id.show_headers).setVisible(false);
            menu.findItem(R.id.hide_headers).setVisible(false);
        } else {
            // hide prev/next buttons in split mode
            if (mDisplayMode != DisplayMode.MESSAGE_VIEW) {
                menu.findItem(R.id.next_message).setVisible(false);
                menu.findItem(R.id.previous_message).setVisible(false);
            } else {
                MessageReference ref = mMessageViewFragment.getMessageReference();
               /* boolean initialized = (mMessageListFragment != null &&
                        mMessageListFragment.isLoadFinished());*/
                boolean initialized = mMessageListFragment != null;
                boolean canDoPrev = (initialized && !mMessageListFragment.isFirst(ref));
                boolean canDoNext = (initialized && !mMessageListFragment.isLast(ref));

                MenuItem prev = menu.findItem(R.id.previous_message);
                prev.setEnabled(canDoPrev);
                prev.getIcon().setAlpha(canDoPrev ? 255 : 127);

                MenuItem next = menu.findItem(R.id.next_message);
                next.setEnabled(canDoNext);
                next.getIcon().setAlpha(canDoNext ? 255 : 127);
            }

            // Set title of menu item to toggle the read state of the currently displayed message
            if (mMessageViewFragment.isMessageRead()) {
                menu.findItem(R.id.toggle_unread).setTitle(R.string.mark_as_unread_action);
            } else {
                menu.findItem(R.id.toggle_unread).setTitle(R.string.mark_as_read_action);
            }

            // Jellybean has built-in long press selection support
            menu.findItem(R.id.select_text).setVisible(Build.VERSION.SDK_INT < 16);

            menu.findItem(R.id.delete).setVisible(K9.isMessageViewDeleteActionVisible());

            /*
             * Set visibility of copy, move, archive, spam in action bar and refile submenu
             */
            if (mMessageViewFragment.isCopyCapable()) {
                menu.findItem(R.id.copy).setVisible(K9.isMessageViewCopyActionVisible());
                menu.findItem(R.id.refile_copy).setVisible(true);
            } else {
                menu.findItem(R.id.copy).setVisible(false);
                menu.findItem(R.id.refile_copy).setVisible(false);
            }

            if (mMessageViewFragment.isMoveCapable()) {
                boolean canMessageBeArchived = mMessageViewFragment.canMessageBeArchived();
                boolean canMessageBeMovedToSpam = mMessageViewFragment.canMessageBeMovedToSpam();

                menu.findItem(R.id.move).setVisible(K9.isMessageViewMoveActionVisible());
                menu.findItem(R.id.archive).setVisible(canMessageBeArchived &&
                        K9.isMessageViewArchiveActionVisible());
                menu.findItem(R.id.spam).setVisible(canMessageBeMovedToSpam &&
                        K9.isMessageViewSpamActionVisible());

                menu.findItem(R.id.refile_move).setVisible(true);
                menu.findItem(R.id.refile_archive).setVisible(canMessageBeArchived);
                menu.findItem(R.id.refile_spam).setVisible(canMessageBeMovedToSpam);
            } else {
                menu.findItem(R.id.move).setVisible(false);
                menu.findItem(R.id.archive).setVisible(false);
                menu.findItem(R.id.spam).setVisible(false);

                menu.findItem(R.id.refile).setVisible(false);
            }

            if (mMessageViewFragment.allHeadersVisible()) {
                menu.findItem(R.id.show_headers).setVisible(false);
            } else {
                menu.findItem(R.id.hide_headers).setVisible(false);
            }
        }


        /*
         * Set visibility of menu items related to the message list
         */

        // Hide both search menu items by default and enable one when appropriate
        menu.findItem(R.id.search).setVisible(false);
        menu.findItem(R.id.search_remote).setVisible(false);

        if (mDisplayMode == DisplayMode.MESSAGE_VIEW || mMessageListFragment == null ||
                !mMessageListFragment.isInitialized()) {
            menu.findItem(R.id.check_mail).setVisible(false);
            menu.findItem(R.id.set_sort).setVisible(false);
            //menu.findItem(R.id.select_all).setVisible(false);
            menu.findItem(R.id.send_messages).setVisible(false);
            menu.findItem(R.id.expunge).setVisible(false);
            menu.findItem(R.id.mark_all_as_read).setVisible(false);
            menu.findItem(R.id.show_folder_list).setVisible(false);
            //menu.findItem(R.id.goto_sms_like_view).setVisible(false);
        } else {
            menu.findItem(R.id.set_sort).setVisible(true);
            //menu.findItem(R.id.select_all).setVisible(true);
            menu.findItem(R.id.compose).setVisible(true);
            menu.findItem(R.id.mark_all_as_read).setVisible(
                    mMessageListFragment.isMarkAllAsReadSupported());

            if (!mMessageListFragment.isSingleAccountMode()) {
                menu.findItem(R.id.expunge).setVisible(false);
                menu.findItem(R.id.send_messages).setVisible(false);
                menu.findItem(R.id.show_folder_list).setVisible(false);
            } else {
                menu.findItem(R.id.send_messages).setVisible(mMessageListFragment.isOutbox());
                menu.findItem(R.id.expunge).setVisible(mMessageListFragment.isRemoteFolder() &&
                        mMessageListFragment.isAccountExpungeCapable());
                menu.findItem(R.id.show_folder_list).setVisible(true);
            }

            menu.findItem(R.id.check_mail).setVisible(mMessageListFragment.isCheckMailSupported());

            // If this is an explicit local search, show the option to search on the server
            if (!mMessageListFragment.isRemoteSearch() &&
                    mMessageListFragment.isRemoteSearchAllowed()) {
                menu.findItem(R.id.search_remote).setVisible(true);
            } else if (!mMessageListFragment.isManualSearch()) {
                menu.findItem(R.id.search).setVisible(true);
            }
        }
    }

    protected void onAccountUnavailable() {
        finish();
        // TODO inform user about account unavailability using Toast
        Accounts.listAccounts(this);
    }

    public void setActionBarTitle(String title) {
        if(toolbar != null)
            toolbar.setTitle(title);
    }

    public void setActionBarSubTitle(String subTitle) {
        if(actionBar != null)
            actionBar.setSubtitle(subTitle);
    }

    public void setActionBarUnread(int unread) {
        if (unread == 0) {
            mActionBarUnread.setVisibility(View.GONE);
        } else {
            mActionBarUnread.setVisibility(View.VISIBLE);
            mActionBarUnread.setText(Integer.toString(unread));
        }
    }

    @Override
    public void onBackStackChanged() {
        findFragments();

        if (mDisplayMode == DisplayMode.SPLIT_VIEW) {
            showMessageViewPlaceHolder();
        }

        configureMenu(mMenu);
    }

    private final class StorageListenerImplementation implements StorageManager.StorageListener {

        @Override
        public void onUnmount(String providerId) {
            if (mAccount != null && providerId.equals(mAccount.getLocalStorageProviderId())) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        onAccountUnavailable();
                    }
                });
            }
        }

        @Override
        public void onMount(String providerId) {
            // no-op
        }

    }

    private void addMessageListFragment(MessageListFragment fragment, boolean addToBackStack) {
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();

        if (mDisplayMode != DisplayMode.SMS_LIST) {
            ft.replace(R.id.message_list_container, fragment);
        } else {
            ft.replace(R.id.sms_message_list_container, fragment);
        }

        if (addToBackStack) {
            ft.addToBackStack(null);
        }

        mMessageListFragment = fragment;
        int transactionId = ft.commit();

        if (transactionId >= 0 && mFirstBackStackId < 0) {
            mFirstBackStackId = transactionId;
        }
    }

    private void removeMessageListFragment() {
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.remove(mMessageListFragment);
        mMessageListFragment = null;
        ft.commit();
    }

    private void showMessageViewPlaceHolder() {
        removeMessageViewFragment();

        // Add placeholder view if necessary
        if (mMessageViewPlaceHolder.getParent() == null) {
            mMessageViewContainer.addView(mMessageViewPlaceHolder);
        }

        mMessageListFragment.setActiveMessage(null);
    }

    /**
     * Remove MessageViewFragment if necessary.
     */
    private void removeMessageViewFragment() {
        if (mMessageViewFragment != null) {
            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            ft.remove(mMessageViewFragment);
            mMessageViewFragment = null;
            ft.commit();

            showDefaultTitleView();
        }
    }

    @Override
    public void onReply(LocalMessage message, PgpData pgpData) {
        MessageCompose.actionReply(this, message, false, pgpData.getDecryptedData());
    }

    @Override
    public void onReplyAll(LocalMessage message, PgpData pgpData) {
        MessageCompose.actionReply(this, message, true, pgpData.getDecryptedData());
    }

    @Override
    public void onForward(LocalMessage mMessage, PgpData mPgpData) {
        MessageCompose.actionForward(this, mMessage, mPgpData.getDecryptedData());
    }

    @Override
    public void showNextMessageOrReturn() {
        if (K9.messageViewReturnToList() || !showLogicalNextMessage()) {
            if (mDisplayMode == DisplayMode.SPLIT_VIEW) {
                showMessageViewPlaceHolder();
            } else {
                showMessageList();
            }
        }
    }

    /**
     * Shows the next message in the direction the user was displaying messages.
     *
     * @return {@code true}
     */
    private boolean showLogicalNextMessage() {
        boolean result = false;

        if (mLastDirection == NEXT) {
            result = showNextMessage();
        } else if (mLastDirection == PREVIOUS) {
            result = showPreviousMessage();
        }

        if (!result) {
            result = showNextMessage() || showPreviousMessage();
        }

        return result;
    }

    @Override
    public void setProgress(boolean enable) {
        mActionBarProgress.setIndeterminate(enable);
        if(enable) {
            mActionBarProgress.setVisibility(View.VISIBLE);
        } else {
            mActionBarProgress.setVisibility(View.GONE);
        }
    }

    @Override
    public void messageHeaderViewAvailable(MessageHeader header) {
        //mActionBarSubject.setMessageHeader(header);
    }

    @Override
    public void displayMessageSubject(String subject) {
        if (mDisplayMode == DisplayMode.MESSAGE_VIEW) {
            setActionBarSubTitle(null);
            if(toolbar != null)
                toolbar.setTitle(subject);
        }
    }

    private boolean showNextMessage() {
        MessageReference ref = mMessageViewFragment.getMessageReference();
        if (ref != null) {
            if (mMessageListFragment.openNext(ref)) {
                mLastDirection = NEXT;
                return true;
            }
        }

        return false;
    }

    private boolean showPreviousMessage() {
        MessageReference ref = mMessageViewFragment.getMessageReference();
        if (ref != null) {
            if (mMessageListFragment.openPrevious(ref)) {
                mLastDirection = PREVIOUS;
                return true;
            }
        }

        return false;
    }

    private void showMessageList() {
        mMessageListWasDisplayed = true;

        switchMessageListFragment(R.id.message_list_container);

        mDisplayMode = DisplayMode.MESSAGE_LIST;
        mViewSwitcher.showFirstView();

        mMessageListFragment.setActiveMessage(null);

        showDefaultTitleView();
        configureMenu(mMenu);
    }

    private void showSmsView() {
        mMessageListWasDisplayed = true;

        switchMessageListFragment(R.id.sms_message_list_container);

        mDisplayMode = DisplayMode.SMS_LIST;
        mViewSwitcher.showThirdView();

        mMessageListFragment.setActiveMessage(null);

        fillContacts(mMessageListFragment);

        showDefaultTitleView();
        configureMenu(mMenu);
    }

    private void fillContacts(final MessageListFragment fragment) {
        leftLinearLayoutContacts = (LinearLayout) findViewById(R.id.smsLikeView_leftlinear_contacts);
        leftLinearLayoutContacts.removeAllViews();

        List<MessageReference> list = fragment.getMessageReferences();

        TreeSet<Address> addressSet = new TreeSet<Address>(new Comparator<Address>() {
            @Override
            public int compare(Address lhs, Address rhs) {
                if (lhs.equals(rhs)) {
                    return 0;
                }

                return lhs.getAddress().compareTo(rhs.getAddress());
            }
        });


        for (MessageReference ref : list) {
            LocalMessage msg = ref.restoreToLocalMessage(this);
            msg.getRootId();
            Address[] addresses = msg.getFrom();

            for (Address address : addresses) {
                addressSet.add(address);
            }
        }

        final Button[] buttons = new Button[addressSet.size()];
        int i = 0;

        for (final Address address : addressSet) {
            final String senderAddress = address.getAddress();
            Button button = new Button(this);
            buttons[i++] = button;
            button.setText(senderAddress);
            button.setTag(address);
            leftLinearLayoutContacts.addView(button);
        }

        for (final Button button : buttons) {
            final String senderAddress = ((Address) button.getTag()).getAddress();
            button.setOnClickListener(new ButtonOnClickListener(senderAddress, button, buttons));
        }

        if (buttons.length > 0) {
            buttons[0].callOnClick();
        }
    }

    private void showMessageView() {
        mDisplayMode = DisplayMode.MESSAGE_VIEW;

        if (!mMessageListWasDisplayed) {
            mViewSwitcher.setAnimateFirstView(false);
        }

        mViewSwitcher.showSecondView();

        showMessageTitleView();
        configureMenu(mMenu);
    }

    private void switchMessageListFragment(int toViewID) {
        View vMessageList = mMessageListFragment.getView();

        if (vMessageList == null) {
            return;
        }

        ViewGroup parent = (ViewGroup) vMessageList.getParent();
        ViewGroup parentNew = (ViewGroup) findViewById(toViewID);

        if (parentNew != null) {
            if (parent != null && !parent.equals(parentNew)) {
                parent.removeView(vMessageList);
                parent.clearDisappearingChildren();
            }

            parentNew.removeAllViews();
            parentNew.addView(vMessageList, parentNew.getLayoutParams());
            parentNew.bringChildToFront(vMessageList);
        }
    }

    private void displayContactMessages(MessageListFragment fragment) {
        View vMessageList = mMessageListFragment.getView();

        if (vMessageList != null) {
            ViewGroup parent = (ViewGroup) vMessageList.getParent();
            if (parent != null) {
                parent.removeView(vMessageList);
            }
        }

        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction transaction = fm.beginTransaction();
        transaction.replace(R.id.sms_message_list_container, fragment);
        transaction.commit();
    }

    @Override
    public void updateMenu() {
        invalidateOptionsMenu();
    }

    @Override
    public void disableDeleteAction() {
        mMenu.findItem(R.id.delete).setEnabled(false);
    }

    private void showDefaultTitleView() {
        //mActionBarMessageView.setVisibility(View.GONE);
        //mActionBarMessageList.setVisibility(View.VISIBLE);

        if (mMessageListFragment != null) {
            mMessageListFragment.updateTitle();
        }

        //mActionBarSubject.setMessageHeader(null);
    }

    private void showMessageTitleView() {
        //mActionBarMessageList.setVisibility(View.GONE);
        //mActionBarMessageView.setVisibility(View.VISIBLE);

        if (mMessageViewFragment != null) {
            displayMessageSubject(null);
            mMessageViewFragment.updateTitle();
        }
    }

    @Override
    public void onSwitchComplete(int displayedChild) {
        if (displayedChild == 0) {
            removeMessageViewFragment();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (mMessageViewFragment != null) {
            mMessageViewFragment.handleCryptoResult(requestCode, resultCode, data);
        }
    }

    private class ButtonOnClickListener implements View.OnClickListener {

        private final String senderAddress;

        private final Button button;

        private final Button[] buttons;

        public ButtonOnClickListener(String senderAddress, Button button, Button[] buttons) {
            this.senderAddress = senderAddress;
            this.button = button;
            this.buttons = buttons;
        }

        private SmsListFragment getFragmentForSender() {
            LocalSearch tmpSearch = new LocalSearch();
            tmpSearch.addAccountUuids(mSearch.getAccountUuids());

            try {
                tmpSearch.or(new ConditionsTreeNode(new SearchCondition(SearchField.SENDER, Attribute.CONTAINS, senderAddress))
                        .and(new ConditionsTreeNode(new SearchCondition(SearchField.FOLDER, Attribute.EQUALS, mFolderName))));

                //         tmpSearch.or (new ConditionsTreeNode(new SearchCondition(SearchField.FOLDER, Attribute.EQUALS,   mAccount.getSentFolderName()))
                //                  .and(new ConditionsTreeNode(new SearchCondition(SearchField.TO,     Attribute.CONTAINS, senderAddress))));

            } catch (Exception e) {
            }

            SmsListFragment frag = SmsListFragment.newInstance(tmpSearch, false, true);
            return frag;

        }

        public void onClick(View v) {
            SmsListFragment fragment = getFragmentForSender();
            displayContactMessages(fragment);

            button.setBackgroundColor(getResources().getColor(android.R.color.holo_blue_dark));
            for (Button buttonOther : buttons) {
                if (!button.equals(buttonOther)) {
                    buttonOther.setBackgroundColor(getResources().getColor(android.R.color.holo_blue_light));
                }
            }
        }

    }

    /*
        MessageListFragmentListener impl
     */

    @Override
    public void enableActionBarProgress(boolean enable) {
        if (mMenuButtonCheckMail != null && mMenuButtonCheckMail.isVisible()) {
            mActionBarProgress.setVisibility(ProgressBar.GONE);

            if (enable) {
                mMenuButtonCheckMail
                        .setActionView(mActionButtonIndeterminateProgress);
            } else {
                mMenuButtonCheckMail.setActionView(null);
            }
        } else {
            if (mMenuButtonCheckMail != null) {
                mMenuButtonCheckMail.setActionView(null);
            }

            if (enable) {
                mActionBarProgress.setVisibility(ProgressBar.VISIBLE);
            } else {
                mActionBarProgress.setVisibility(ProgressBar.GONE);
            }
        }
    }

    @Override
    public void goBack() {
        Log.d(K9.LOG_TAG, "MessageList.goBack");

        FragmentManager fragmentManager = getSupportFragmentManager();

        if (mDisplayMode == DisplayMode.MESSAGE_VIEW) {
            showMessageList();
        } else if (fragmentManager.getBackStackEntryCount() > 0) {
            fragmentManager.popBackStack();
        } else if (mMessageListFragment.isManualSearch()) {
            finish();
        } else if (!mSingleFolderMode) {
            onAccounts();
        } else {
            onShowFolderList();
        }
    }

    @Override
    public void onCompose(Account account) {
        MessageCompose.actionCompose(this, account);
    }

    @Override
    public void onForward(LocalMessage message) {
        MessageCompose.actionForward(this, message, null);
    }

    @Override
    public void onReply(LocalMessage message) {
        MessageCompose.actionReply(this, message, false, null);
    }

    @Override
    public void onReplyAll(LocalMessage message) {
        MessageCompose.actionReply(this, message, true, null);
    }

    @Override
    public void onResendMessage(LocalMessage message) {
        MessageCompose.actionEditDraft(this, message.makeMessageReference());
    }

    @Override
    public void openMessage(MessageReference messageReference) {
        mDrawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
        mDrawerToggle.setDrawerIndicatorEnabled(false);
        if(actionBar != null) {
            //both not working -- click on back-button will not be resolved
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeButtonEnabled(true);
            mDrawerToggle.syncState();
        }

        Preferences prefs = Preferences.getPreferences(getApplicationContext());
        Account account = prefs.getAccount(messageReference.getAccountUuid());
        String folderName = messageReference.getFolderName();

        if (folderName.equals(account.getDraftsFolderName())) {
            MessageCompose.actionEditDraft(this, messageReference);
        } else {
            mMessageViewContainer.removeView(mMessageViewPlaceHolder);

            if (mMessageListFragment != null) {
                mMessageListFragment.setActiveMessage(messageReference);
            }

            MessageViewFragment fragment = MessageViewFragment.newInstance(messageReference);
            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            ft.replace(R.id.message_view_container, fragment);
            mMessageViewFragment = fragment;
            ft.commit();

            if (mDisplayMode != DisplayMode.SPLIT_VIEW) {
                showMessageView();
            }
        }
    }

    @Override
    public void remoteSearchStarted() {
        // Remove action button for remote search
        configureMenu(mMenu);
    }

    @Override
    public void setMessageListProgress(int progress) {
        setProgress(progress);
    }

    @Override
    public void setMessageListSubTitle(String subTitle) {
        setActionBarSubTitle(subTitle);
    }

    @Override
    public void setMessageListTitle(String title) {
        setActionBarTitle(title);
    }

    @Override
    public void setUnreadCount(int unread) {
        setActionBarUnread(unread);
    }

    @Override
    public void showMoreFromSameSender(String senderAddress) {
        LocalSearch tmpSearch = new LocalSearch("From " + senderAddress);
        tmpSearch.addAccountUuids(mSearch.getAccountUuids());
        tmpSearch.and(SearchField.SENDER, senderAddress, Attribute.CONTAINS);

        MessageListFragment fragment = MessageListFragment.newInstance(tmpSearch, false, false);

        addMessageListFragment(fragment, true);
    }

    @Override
    public void showSMS(Account account, String FolderName, long rootId, MessageReference messageReference) {

    }

    @Override
    public void showThread(Account account, String folderName, long threadRootId) {
        showMessageViewPlaceHolder();

        LocalSearch tmpSearch = new LocalSearch();
        tmpSearch.addAccountUuid(account.getUuid());
        tmpSearch.and(SearchField.THREAD_ID, String.valueOf(threadRootId), Attribute.EQUALS);

        MessageListFragment fragment = MessageListFragment.newInstance(tmpSearch, true, false);
        addMessageListFragment(fragment, true);
    }

    @Override
    public boolean startSearch(Account account, String folderName) {
        // If this search was started from a MessageList of a single folder, pass along that folder info
        // so that we can enable remote search.
        if (account != null && folderName != null) {
            final Bundle appData = new Bundle();
            appData.putString(EXTRA_SEARCH_ACCOUNT, account.getUuid());
            appData.putString(EXTRA_SEARCH_FOLDER, folderName);
            startSearch(null, false, appData, false);
        } else {
            // TODO Handle the case where we're searching from within a search result.
            startSearch(null, false, null, false);
        }

        return true;
    }
}
