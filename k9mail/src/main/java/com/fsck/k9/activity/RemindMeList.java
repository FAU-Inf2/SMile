package com.fsck.k9.activity;


import android.app.ActionBar;
import android.app.DatePickerDialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.BaseAdapter;
import android.widget.DatePicker;
import android.widget.ListView;
import android.widget.TimePicker;

import com.fsck.k9.Account;
import com.fsck.k9.activity.misc.SwipeGestureDetector.OnSwipeGestureListener;
import com.fsck.k9.controller.MessagingController;
import com.fsck.k9.fragment.RemindMeDatePickerDialog;
import com.fsck.k9.fragment.RemindMeDialog;
import com.fsck.k9.K9;
import com.fsck.k9.Preferences;
import com.fsck.k9.fragment.RemindMeTimePickerDialog;
import com.fsck.k9.mail.Folder;
import com.fsck.k9.mail.RemindMe;
import com.fsck.k9.mail.Message;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mailstore.LocalFolder;
import com.fsck.k9.mailstore.LocalRemindMe;
import com.fsck.k9.mailstore.LocalMessage;
import com.fsck.k9.mailstore.LocalStore;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import de.fau.cs.mad.smile.android.R;

public class RemindMeList extends K9ListActivity
        implements RemindMeDialog.NoticeDialogListener,
            TimePickerDialog.OnTimeSetListener,
            DatePickerDialog.OnDateSetListener,
            OnSwipeGestureListener {
    public static final String EXTRA_MESSAGE_REFERENCE = "de.fau.cs.mad.smile.android.MESSAGE_REFERENCE";
    public static final String CREATE_FOLLOWUP = "de.fau.cs.mad.smile.android.CREATE_FOLLOWUP";
    public static final String EDIT_FOLLOWUP = "de.fau.cs.mad.smile.android.EDIT_FOLLOWUP";
    public static final String DELETE_FOLLOWUP = "de.fau.cs.mad.smile.android.DELETE_FOLLOWUP";
    public static final String FOLLOW_UP_FOLDERNAME = "RemindMe";

    private Account mAccount;
    private LocalRemindMe mLocalRemindMe;
    private RemindMe currentRemindMe;
    private String folderName;

    public static Intent createFollowUp(Context context,
                                        LocalMessage message) {
        Intent i = new Intent(context, RemindMeList.class);
        i.putExtra(EXTRA_MESSAGE_REFERENCE, message.makeMessageReference());
        i.setAction(CREATE_FOLLOWUP);
        return i;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final Intent intent = getIntent();

        // TODO: this is ugly, search for better solution to expose onClick result and handling intents
        if(CREATE_FOLLOWUP.equals(intent.getAction())) {
            MessageReference reference = intent.getParcelableExtra(EXTRA_MESSAGE_REFERENCE);
            Message message = reference.restoreToLocalMessage(this);
            String accountUuid = ((LocalFolder) message.getFolder()).getAccountUuid();
            mAccount = Preferences.getPreferences(this).getAccount(accountUuid);
            folderName = message.getFolder().getName();

            RemindMeDialog dialog = RemindMeDialog.newInstance(message);
            dialog.show(getFragmentManager(), "mTimeValue");
        }

        setContentView(R.layout.remindme_list);

        // Enable gesture detection for RemindMeList
        setupGestureDetector(this);

        // enable up navigation in ActionBar
        ActionBar actionBar = getActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);

        ListView listView = getListView();
        listView.setItemsCanFocus(false);

        try {
            if(mAccount == null) {
                List<Account> accounts = Preferences.getPreferences(this).getAccounts();
                mAccount = accounts.get(0);
            }

            LocalStore store = LocalStore.getInstance(mAccount, this);
            mLocalRemindMe = new LocalRemindMe(store);
            LocalFolder folder = new LocalFolder(store, mAccount.getFollowUpFolderName());

            // FIXME: probably not the best place
            if (!folder.exists()) {
                folder.create(Folder.FolderType.HOLDS_MESSAGES);
                folder.open(LocalFolder.OPEN_MODE_RO);
            }
        } catch (MessagingException e) {
            Log.e(K9.LOG_TAG, "Unable to retrieve message", e);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.followup_list_actions, menu);
        return super.onCreateOptionsMenu(menu);
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
        final String timePickerTag = "followUpTimePicker";
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
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(currentRemindMe.getRemindTime());
        calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
        calendar.set(Calendar.MINUTE, minute);
        Log.d(K9.LOG_TAG, "Selected time: " + calendar.getTime());
        currentRemindMe.setRemindTime(calendar.getTime());
        new InsertFollowUp().execute(currentRemindMe);
        new LoadFollowUp().execute();
        ((BaseAdapter)getListView().getAdapter()).notifyDataSetChanged();
    }

    /**
     * Reload list of RemindMe when this activity is resumed.
     */
    @Override
    public void onResume() {
        super.onResume();
        new LoadFollowUp().execute();
    }

    @Override
    protected void onListItemClick(ListView listView, View view, int position, long id) {
        Object obj = listView.getItemAtPosition(position);

        if(obj instanceof RemindMe) {
            RemindMe remindMe = (RemindMe)obj;
            Log.d(K9.LOG_TAG, "listItem is instanceof RemindMe: " + remindMe);
        }

        super.onListItemClick(listView, view, position, id);
    }

    @Override
    public void onDialogClick(DialogFragment dialog) {
        Log.i(K9.LOG_TAG, "RemindMeList.onDialogClick");
        RemindMeDialog dlg = (RemindMeDialog)dialog;
        currentRemindMe = dlg.getRemindMe();

        if(currentRemindMe.getRemindInterval() == RemindMe.RemindInterval.CUSTOM) {
            final String datePickerTag = "followUpDatePicker";
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

            new InsertFollowUp().execute(currentRemindMe);
            new LoadFollowUp().execute();
            ((BaseAdapter)getListView().getAdapter()).notifyDataSetChanged();
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
        RemindMe remindMe = getFollowUpFromListSwipe(e1, e2);

        if(remindMe != null) {
            Log.d(K9.LOG_TAG, "RightToLeftSwipe, Object: " + remindMe);
            RemindMeDialog dialog = RemindMeDialog.newInstance(remindMe);
            dialog.show(getFragmentManager(), "mTimeValue");
        }
    }

    @Override
    public void onSwipeLeftToRight(MotionEvent e1, MotionEvent e2) {
        // delete
        RemindMe remindMe = getFollowUpFromListSwipe(e1, e2);

        if(remindMe != null) {
            Log.d(K9.LOG_TAG, "LeftToRightSwipe, Object: " + remindMe);
            new DeleteFollowUp().execute(remindMe);
            ((RemindMeAdapter)getListView().getAdapter()).remove(remindMe);
        }
    }

    private RemindMe getFollowUpFromListSwipe(MotionEvent e1, MotionEvent e2) {
        int x = (int) e1.getRawX();
        int y = (int) e1.getRawY();

        ListView listView = getListView();
        Rect rect = new Rect();
        listView.getGlobalVisibleRect(rect);

        if (rect.contains(x, y)) {
            int[] listPosition = new int[2];
            listView.getLocationOnScreen(listPosition);

            int listX = x - listPosition[0];
            int listY = y - listPosition[1];

            int listViewPosition = listView.pointToPosition(listX, listY);
            return (RemindMe) listView.getAdapter().getItem(listViewPosition);
        }

        return  null;
    }

    private void populateListView(List<RemindMe> items) {
        RemindMeAdapter adapter = new RemindMeAdapter(this, items);
        ListView listView = getListView();
        listView.setAdapter(adapter);
        listView.invalidate();
    }

    class LoadFollowUp extends AsyncTask<Void, Void, List<RemindMe>> {

        @Override
        protected List<RemindMe> doInBackground(Void... params) {
            try {
                return mLocalRemindMe.getAllFollowUps();
            } catch (MessagingException e) {
                Log.e(K9.LOG_TAG, "Unable to retrieve FollowUps", e);
            }
            return null;
        }

        @Override
        protected void onPostExecute(List<RemindMe> remindMes) {
            super.onPostExecute(remindMes);
            populateListView(remindMes);
        }
    }

    class InsertFollowUp extends AsyncTask<RemindMe, Void, Void> {

        @Override
        protected Void doInBackground(RemindMe... params) {
            for(RemindMe remindMe : params) {
                try {
                    LocalStore store = LocalStore.getInstance(mAccount, getApplication());
                    LocalFolder folder = new LocalFolder(store, mAccount.getFollowUpFolderName());
                    folder.open(Folder.OPEN_MODE_RW);

                    remindMe.setFolderId(folder.getId());

                    Log.d(K9.LOG_TAG, "Inserting remindMe: " + remindMe);
                    MessagingController messagingController = MessagingController.getInstance(getApplication());
                    messagingController.moveMessages(mAccount,
                            remindMe.getReference().getFolder().getName(),
                            new ArrayList<LocalMessage>(Arrays.asList((LocalMessage) remindMe.getReference())),
                            mAccount.getFollowUpFolderName(), null);

                    if(remindMe.getId() > 0) {
                        mLocalRemindMe.update(remindMe);
                    } else {
                        mLocalRemindMe.add(remindMe);
                    }
                } catch (Exception e) {
                    Log.e(K9.LOG_TAG, "Unable to insert followup", e);
                }
            }
            return null;
        }
    }

    class DeleteFollowUp extends AsyncTask<RemindMe, Void, Void> {

        @Override
        protected Void doInBackground(RemindMe... params) {
            for(RemindMe remindMe : params) {
                try {
                    mLocalRemindMe.delete(remindMe);
                } catch (MessagingException e) {
                    Log.e(K9.LOG_TAG, "Unable to delete RemindMe", e);
                }
                try {
                    //move back to inbox
                    MessagingController messagingController = MessagingController.getInstance(getApplication());
                    messagingController.moveMessages(mAccount,
                            remindMe.getReference().getFolder().getName(),
                            new ArrayList<LocalMessage>(Arrays.asList((LocalMessage) remindMe.getReference())),
                            mAccount.getInboxFolderName(), null);
                } catch (Exception e) {
                    Log.e(K9.LOG_TAG, "Moving back deleted RemindMe failed: " + e.getMessage());
                }
            }
            return null;
        }
    }
}
