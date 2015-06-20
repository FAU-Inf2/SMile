package com.fsck.k9.activity;


import android.app.DatePickerDialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.widget.BaseAdapter;
import android.widget.DatePicker;
import android.widget.ListView;
import android.widget.TimePicker;

import com.fsck.k9.Account;
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
import java.util.Map;

import de.fau.cs.mad.smile.android.R;

public class FollowUpList extends K9ListActivity
        implements FollowUpDialog.NoticeDialogListener,
            TimePickerDialog.OnTimeSetListener,
            DatePickerDialog.OnDateSetListener  {
    public static final String EXTRA_MESSAGE_REFERENCE = "de.fau.cs.mad.smile.android.MESSAGE_REFERENCE";
    public static final String CREATE_FOLLOWUP = "de.fau.cs.mad.smile.android.CREATE_FOLLOWUP";
    public static final String FOLLOW_UP_FOLDERNAME = "RemindMe";

    private Account mAccount;
    private LocalFollowUp mLocalFollowUp;
    private FollowUp newFollowUp;

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

        getFragmentManager().addOnBackStackChangedListener(
                new FragmentManager.OnBackStackChangedListener() {
                    public void onBackStackChanged() {
                        for (int i = 0; i < getFragmentManager().getBackStackEntryCount(); i++) {
                            Log.d(K9.LOG_TAG, "BackStack changed, count " + getFragmentManager().getBackStackEntryCount());
                            Log.d(K9.LOG_TAG, "BackStack changed: " + getFragmentManager().getBackStackEntryAt(i).getName());
                        }
                    }
                });

        final Intent intent = getIntent();

        // TODO: this is ugly, search for better solution to expose onClick result and handling intents
        if(CREATE_FOLLOWUP.equals(intent.getAction())) {
            MessageReference reference = intent.getParcelableExtra(EXTRA_MESSAGE_REFERENCE);
            FollowUpDialog dialog = new FollowUpDialog();
            dialog.setReference(reference);
            dialog.show(getFragmentManager(), "mTimeValue");
        }

        setContentView(R.layout.followup_list);

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
    public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
        final String timePickerTag = "followUpTimePicker";
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.YEAR, year);
        calendar.set(Calendar.MONTH, monthOfYear);
        calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);

        Log.d(K9.LOG_TAG, "Selected date: " + calendar.getTime());
        newFollowUp.setRemindTime(calendar.getTime());
        FollowUpTimePickerDialog timePickerDialog = FollowUpTimePickerDialog.newInstance(this);
        timePickerDialog.show(getFragmentManager(), timePickerTag);
    }

    @Override
    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(newFollowUp.getRemindTime());
        calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
        calendar.set(Calendar.MINUTE, minute);
        Log.d(K9.LOG_TAG, "Selected time: " + calendar.getTime());
        newFollowUp.setRemindTime(calendar.getTime());
        new InsertFollowUp().execute(newFollowUp);
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
    public void onDialogClick(DialogFragment dialog) {
        Log.i(K9.LOG_TAG, "FollowUpList.onDialogClick");
        FollowUpDialog dlg = (FollowUpDialog)dialog;
        Preferences prefs = Preferences.getPreferences(this);
        newFollowUp = new FollowUp();

        Message msg = null;
        MessageReference reference = dlg.getReference();
        mAccount = prefs.getAccount(reference.getAccountUuid());
        long folderId = -1;

        try {
            msg = mAccount.getLocalStore().getFolder(reference.getFolderName()).getMessage(reference.getUid());
            folderId = ((LocalFolder) msg.getFolder()).getId();
        } catch (MessagingException e) {
            Log.e(K9.LOG_TAG, "error while retrieving message", e);
            return;
        }

        newFollowUp.setFolderId(folderId);
        newFollowUp.setReference(msg);
        newFollowUp.setTitle(msg.getSubject());

        if(dlg.getTimeValue() < 0) {
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
            newFollowUp.setRemindTime(addMinute(new Date(System.currentTimeMillis()), dlg.getTimeValue()));
            new InsertFollowUp().execute(newFollowUp);
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
                    mLocalFollowUp.add(followUp);

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
}
