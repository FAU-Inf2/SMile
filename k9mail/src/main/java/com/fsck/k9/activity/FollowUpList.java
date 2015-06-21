package com.fsck.k9.activity;


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
import com.fsck.k9.fragment.FollowUpDatePickerDialog;
import com.fsck.k9.fragment.FollowUpDialog;
import com.fsck.k9.K9;
import com.fsck.k9.Preferences;
import com.fsck.k9.fragment.FollowUpTimePickerDialog;
import com.fsck.k9.mail.FollowUp;
import com.fsck.k9.mail.Message;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mailstore.LocalFolder;
import com.fsck.k9.mailstore.LocalFollowUp;
import com.fsck.k9.mailstore.LocalMessage;
import com.fsck.k9.mailstore.LocalStore;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import de.fau.cs.mad.smile.android.R;

public class FollowUpList extends K9ListActivity
        implements FollowUpDialog.NoticeDialogListener,
            TimePickerDialog.OnTimeSetListener,
            DatePickerDialog.OnDateSetListener,
            OnSwipeGestureListener {
    public static final String EXTRA_MESSAGE_REFERENCE = "de.fau.cs.mad.smile.android.MESSAGE_REFERENCE";
    public static final String CREATE_FOLLOWUP = "de.fau.cs.mad.smile.android.CREATE_FOLLOWUP";
    public static final String EDIT_FOLLOWUP = "de.fau.cs.mad.smile.android.EDIT_FOLLOWUP";
    public static final String DELETE_FOLLOWUP = "de.fau.cs.mad.smile.android.DELETE_FOLLOWUP";
    public static final String FOLLOW_UP_FOLDERNAME = "RemindMe";

    private Account mAccount;
    private LocalFollowUp mLocalFollowUp;
    private FollowUp currentFollowUp;

    public static Intent createFollowUp(Context context,
                                        LocalMessage message) {
        Intent i = new Intent(context, FollowUpList.class);
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
            FollowUpDialog dialog = FollowUpDialog.newInstance(message);
            dialog.show(getFragmentManager(), "mTimeValue");
        }

        setContentView(R.layout.followup_list);
        // Enable gesture detection for FollowUpList
        setupGestureDetector(this);

        ListView listView = getListView();
        listView.setItemsCanFocus(false);

        List<Account> accounts = Preferences.getPreferences(this).getAccounts();
        try {
            mAccount = accounts.get(0);
            LocalStore store = LocalStore.getInstance(mAccount, this);
            mLocalFollowUp = new LocalFollowUp(store);
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

    @Override
    public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
        final String timePickerTag = "followUpTimePicker";
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.YEAR, year);
        calendar.set(Calendar.MONTH, monthOfYear);
        calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);

        Log.d(K9.LOG_TAG, "Selected date: " + calendar.getTime());
        currentFollowUp.setRemindTime(calendar.getTime());
        FollowUpTimePickerDialog timePickerDialog = FollowUpTimePickerDialog.newInstance(this);
        timePickerDialog.show(getFragmentManager(), timePickerTag);
    }

    @Override
    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(currentFollowUp.getRemindTime());
        calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
        calendar.set(Calendar.MINUTE, minute);
        Log.d(K9.LOG_TAG, "Selected time: " + calendar.getTime());
        currentFollowUp.setRemindTime(calendar.getTime());
        new InsertFollowUp().execute(currentFollowUp);
        new LoadFollowUp().execute();
        ((BaseAdapter)getListView().getAdapter()).notifyDataSetChanged();
    }

    public void populateListView(List<FollowUp> items) {
        FollowUpAdapter adapter = new FollowUpAdapter(this, items);
        ListView listView = getListView();
        listView.setAdapter(adapter);
        listView.invalidate();
    }

    /**
     * Reload list of FollowUp when this activity is resumed.
     */
    @Override
    public void onResume() {
        super.onResume();
        new LoadFollowUp().execute();
    }

    @Override
    protected void onListItemClick(ListView listView, View view, int position, long id) {
        Object obj = listView.getItemAtPosition(position);
        if(obj instanceof FollowUp) {
            FollowUp followUp = (FollowUp)obj;
            Log.d(K9.LOG_TAG, "listItem is instanceof FollowUp: " + followUp);
        }
        super.onListItemClick(listView, view, position, id);
    }

    @Override
    public void onDialogClick(DialogFragment dialog) {
        Log.i(K9.LOG_TAG, "FollowUpList.onDialogClick");
        FollowUpDialog dlg = (FollowUpDialog)dialog;
        currentFollowUp = dlg.getFollowUp();

        if(currentFollowUp.getRemindInterval() == FollowUp.RemindInterval.CUSTOM) {
            final String datePickerTag = "followUpDatePicker";
            FragmentTransaction ft = getFragmentManager().beginTransaction();
            Fragment prev = getFragmentManager().findFragmentByTag(datePickerTag);

            if (prev != null) {
                ft.remove(prev);
            }

            ft.addToBackStack(datePickerTag);

            FollowUpDatePickerDialog datePickerDialog = FollowUpDatePickerDialog.newInstance(this);
            datePickerDialog.show(ft, datePickerTag);
        } else {
            switch (currentFollowUp.getRemindInterval()) {
                case TEN_MINUTES:
                    currentFollowUp.setRemindTime(addMinute(new Date(System.currentTimeMillis()), 10));
                    break;
                case THIRTY_MINUTES:
                    currentFollowUp.setRemindTime(addMinute(new Date(System.currentTimeMillis()), 30));
                    break;
                case TOMORROW:
                    currentFollowUp.setRemindTime(addMinute(new Date(System.currentTimeMillis()), 24*60));
                    break;
            }

            new InsertFollowUp().execute(currentFollowUp);
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
        FollowUp followUp = getFollowUpFromListSwipe(e1, e2);

        if(followUp != null) {
            Log.d(K9.LOG_TAG, "RightToLeftSwipe, Object: " + followUp);
            FollowUpDialog dialog = FollowUpDialog.newInstance(followUp);
            dialog.show(getFragmentManager(), "mTimeValue");
        }
    }

    @Override
    public void onSwipeLeftToRight(MotionEvent e1, MotionEvent e2) {
        // delete
        FollowUp followUp = getFollowUpFromListSwipe(e1, e2);

        if(followUp != null) {
            Log.d(K9.LOG_TAG, "LeftToRightSwipe, Object: " + followUp);
            new DeleteFollowUp().execute(followUp);
            ((FollowUpAdapter)getListView().getAdapter()).remove(followUp);
        }
    }

    private FollowUp getFollowUpFromListSwipe(MotionEvent e1, MotionEvent e2) {
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
            return (FollowUp) listView.getAdapter().getItem(listViewPosition);
        }

        return  null;
    }

    class LoadFollowUp extends AsyncTask<Void, Void, List<FollowUp>> {

        @Override
        protected List<FollowUp> doInBackground(Void... params) {
            try {
                return mLocalFollowUp.getAllFollowUps();
            } catch (MessagingException e) {
                Log.e(K9.LOG_TAG, "Unable to retrieve FollowUps", e);
            }
            return null;
        }

        @Override
        protected void onPostExecute(List<FollowUp> followUps) {
            super.onPostExecute(followUps);
            populateListView(followUps);
        }
    }

    class InsertFollowUp extends AsyncTask<FollowUp, Void, Void> {

        @Override
        protected Void doInBackground(FollowUp... params) {
            for(FollowUp followUp : params) {
                try {
                    if(followUp.getId() > 0) {
                        mLocalFollowUp.update(followUp);
                    } else {
                        mLocalFollowUp.add(followUp);
                    }

                    MessagingController messagingController = MessagingController.getInstance(getApplication());
                    messagingController.moveMessages(mAccount,
                            ((LocalFolder) followUp.getReference().getFolder()).getName(),
                            new ArrayList<LocalMessage>(Arrays.asList((LocalMessage) followUp.getReference())),
                            mAccount.getFollowUpFolderName(), null);
                } catch (MessagingException e) {
                    Log.e(K9.LOG_TAG, "Unable to insert followup", e);
                }
            }
            return null;
        }
    }

    class DeleteFollowUp extends AsyncTask<FollowUp, Void, Void> {

        @Override
        protected Void doInBackground(FollowUp... params) {
            for(FollowUp followUp : params) {
                try {
                    mLocalFollowUp.delete(followUp);
                } catch (MessagingException e) {
                    Log.e(K9.LOG_TAG, "Unable to insert followup", e);
                }
            }
            return null;
        }
    }
}
