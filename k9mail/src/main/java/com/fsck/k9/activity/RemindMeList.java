package com.fsck.k9.activity;


import android.app.ActionBar;
import android.app.DatePickerDialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.DatePicker;
import android.widget.ListView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.fsck.k9.Account;
import com.fsck.k9.activity.misc.SwipeGestureDetector.OnSwipeGestureListener;
import com.fsck.k9.fragment.RemindMeDatePickerDialog;
import com.fsck.k9.fragment.RemindMeDialog;
import com.fsck.k9.K9;
import com.fsck.k9.Preferences;
import com.fsck.k9.fragment.RemindMeFragment;
import com.fsck.k9.fragment.RemindMeTimePickerDialog;
import com.fsck.k9.mail.Folder;
import com.fsck.k9.mail.RemindMe;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mailstore.LocalFolder;
import com.fsck.k9.mailstore.LocalRemindMe;
import com.fsck.k9.mailstore.LocalMessage;
import com.fsck.k9.mailstore.LocalStore;
import com.fsck.k9.service.RemindMeService;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

import de.fau.cs.mad.smile.android.R;

public class RemindMeList extends K9Activity
        implements RemindMeDialog.NoticeDialogListener,
            TimePickerDialog.OnTimeSetListener,
            DatePickerDialog.OnDateSetListener,
            OnSwipeGestureListener {

    public static final String EXTRA_MESSAGE_REFERENCE = "de.fau.cs.mad.smile.android.MESSAGE_REFERENCE";
    public static final String CREATE_REMINDME = "de.fau.cs.mad.smile.android.CREATE_REMINDME";
    public static final String EDIT_REMINDME = "de.fau.cs.mad.smile.android.EDIT_REMINDME";
    public static final String DELETE_REMINDME = "de.fau.cs.mad.smile.android.DELETE_REMINDME";

    private Account mAccount;
    private LocalRemindMe mLocalRemindMe;
    private RemindMe currentRemindMe;
    private String folderName;
    private ListView mDrawerList;
    private DrawerLayout mDrawerLayout;
    private ActionBarDrawerToggle mDrawerToggle;
    private boolean onTimeSetCalled = false;
    private boolean onDateSetCalled = false;

    private ArrayAdapter<String> mAdapter;
    private RemindMeFragment remindMeFragment;

    public static Intent createRemindMe(Context context,
                                        LocalMessage message) {
        Intent i = new Intent(context, RemindMeList.class);
        i.putExtra(EXTRA_MESSAGE_REFERENCE, message.makeMessageReference());
        i.setAction(CREATE_REMINDME);
        return i;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final Intent intent = getIntent();

        setContentView(R.layout.remindme_list);

        // TODO: this is ugly, search for better solution to expose onClick result and handling intents
        if(CREATE_REMINDME.equals(intent.getAction())) {
            MessageReference reference = intent.getParcelableExtra(EXTRA_MESSAGE_REFERENCE);
            LocalMessage message = reference.restoreToLocalMessage(this);
            String accountUuid = reference.getAccountUuid();

            remindMeFragment = RemindMeFragment.newInstance(accountUuid);
            FragmentManager fragmentManager = getFragmentManager();
            fragmentManager.beginTransaction()
                    .replace(R.id.content_frame, remindMeFragment)
                    .commit();

            mAccount = Preferences.getPreferences(this).getAccount(accountUuid);
            folderName = message.getFolder().getName();

            RemindMeDialog dialog = RemindMeDialog.newInstance(message);
            dialog.show(getFragmentManager(), "mTimeValue");
        }

        // Enable gesture detection for RemindMeList
        setupGestureDetector(this);

        // enable up navigation in ActionBar
        ActionBar actionBar = getActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);

        mDrawerList = (ListView)findViewById(R.id.navList);
        mDrawerLayout = (DrawerLayout)findViewById(R.id.drawer_layout);

        addDrawerItems();
        setupDrawer();

        try {
            if(mAccount == null) {
                List<Account> accounts = Preferences.getPreferences(this).getAccounts();
                mAccount = accounts.get(0);
            }

            LocalStore store = LocalStore.getInstance(mAccount, this);
            mLocalRemindMe = new LocalRemindMe(store);
            LocalFolder folder = new LocalFolder(store, mAccount.getRemindMeFolderName());

            // FIXME: probably not the best place
            if (!folder.exists()) {
                folder.create(Folder.FolderType.HOLDS_MESSAGES);
                folder.open(LocalFolder.OPEN_MODE_RO);
            }
        } catch (MessagingException e) {
            Log.e(K9.LOG_TAG, "Unable to retrieve message", e);
        }
    }

    private void addDrawerItems() {
        String[] osArray = { "Android", "iOS", "Windows", "OS X", "Linux" };
        mAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, osArray);
        mDrawerList.setAdapter(mAdapter);

        mDrawerList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Toast.makeText(RemindMeList.this, "Time for an upgrade!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupDrawer() {
        mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout, R.string.drawer_open, R.string.drawer_close) {

            /** Called when a drawer has settled in a completely open state. */
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                getActionBar().setTitle("Navigation!");
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }

            /** Called when a drawer has settled in a completely closed state. */
            public void onDrawerClosed(View view) {
                super.onDrawerClosed(view);
                getActionBar().setTitle(getTitle());
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }
        };

        mDrawerToggle.setDrawerIndicatorEnabled(true);
        mDrawerLayout.setDrawerListener(mDrawerToggle);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
        mDrawerToggle.syncState();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.remindme_list_actions, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mDrawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Nullable
    @Override
    public Intent getParentActivityIntent() {
        Intent intent = super.getParentActivityIntent();
        intent.putExtra("account", mAccount.getUuid());
        intent.putExtra("folder", folderName);
        return intent;
    }

    @Override
    public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
        if(onDateSetCalled) {
            return;
        }

        onDateSetCalled = true;

        final String timePickerTag = "remindMeTimePicker";
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.YEAR, year);
        calendar.set(Calendar.MONTH, monthOfYear);
        calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);

        Log.d(K9.LOG_TAG, "Selected date: " + calendar.getTime());
        currentRemindMe.setRemindTime(calendar.getTime());
        RemindMeTimePickerDialog timePickerDialog = RemindMeTimePickerDialog.newInstance(this);
        timePickerDialog.show(getFragmentManager(), timePickerTag);
    }

    @Override
    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
        if(onTimeSetCalled) {
            return;
        }

        onTimeSetCalled = true;

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(currentRemindMe.getRemindTime());
        calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
        calendar.set(Calendar.MINUTE, minute);
        Log.d(K9.LOG_TAG, "Selected time: " + calendar.getTime());
        currentRemindMe.setRemindTime(calendar.getTime());
        remindMeFragment.add(currentRemindMe);
    }

    @Override
    public void onDialogClick(DialogFragment dialog) {
        Log.i(K9.LOG_TAG, "RemindMeList.onDialogClick");
        RemindMeDialog dlg = (RemindMeDialog)dialog;
        currentRemindMe = dlg.getRemindMe();

        if(currentRemindMe.getRemindInterval() == RemindMe.RemindInterval.CUSTOM) {
            onDateSetCalled = false;
            onTimeSetCalled = false;

            final String datePickerTag = "remindMeDatePicker";
            FragmentTransaction ft = getFragmentManager().beginTransaction();
            Fragment prev = getFragmentManager().findFragmentByTag(datePickerTag);

            if (prev != null) {
                ft.remove(prev);
            }

            ft.addToBackStack(datePickerTag);

            RemindMeDatePickerDialog datePickerDialog = RemindMeDatePickerDialog.newInstance(this);
            datePickerDialog.show(ft, datePickerTag);
        } else {
            switch (currentRemindMe.getRemindInterval()) {
                case TEN_MINUTES:
                    currentRemindMe.setRemindTime(addMinute(new Date(System.currentTimeMillis()), 10));
                    break;
                case THIRTY_MINUTES:
                    currentRemindMe.setRemindTime(addMinute(new Date(System.currentTimeMillis()), 30));
                    break;
                case TOMORROW:
                    currentRemindMe.setRemindTime(addMinute(new Date(System.currentTimeMillis()), 24*60));
                    break;
            }

            remindMeFragment.add(currentRemindMe);
        }
    }

    private Date addMinute(Date date, int minute) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.add(Calendar.MINUTE, minute);
        return calendar.getTime();
    }

    @Override
    public void onSwipeRightToLeft(MotionEvent e1, MotionEvent e2) {
        // edit
        remindMeFragment.onSwipeRightToLeft(e1, e2);
    }

    @Override
    public void onSwipeLeftToRight(MotionEvent e1, MotionEvent e2) {
        // delete
        remindMeFragment.onSwipeLeftToRight(e1, e2);
    }
}
