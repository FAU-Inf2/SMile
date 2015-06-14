package com.fsck.k9.activity;


import android.app.DialogFragment;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.TimePicker;

import com.fsck.k9.Account;
import com.fsck.k9.fragment.FollowUpDialog;
import com.fsck.k9.K9;
import com.fsck.k9.Preferences;
import com.fsck.k9.mail.FollowUp;
import com.fsck.k9.mail.Message;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mailstore.LocalFolder;
import com.fsck.k9.mailstore.LocalFollowUp;
import com.fsck.k9.mailstore.LocalMessage;
import com.fsck.k9.mailstore.LocalStore;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

import de.fau.cs.mad.smile.android.R;

public class FollowUpList extends K9ListActivity
        implements FollowUpDialog.NoticeDialogListener {
    private LocalFollowUp mLocalFollowUp;
    public static final String EXTRA_MESSAGE_REFERENCE = "de.fau.cs.mad.smile.android.MESSAGE_REFERENCE";
    public static final String CREATE_FOLLOWUP = "de.fau.cs.mad.smile.android.CREATE_FOLLOWUP";

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
            FollowUpDialog dialog = new FollowUpDialog();
            dialog.setReference(reference);
            dialog.show(getFragmentManager(), "mTimeValue");
        }

        setContentView(R.layout.followup_list);

        ListView listView = getListView();
        listView.setItemsCanFocus(false);

        List<Account> accounts = Preferences.getPreferences(getApplicationContext()).getAccounts();
        try {
            LocalStore store = LocalStore.getInstance(accounts.get(0), getApplicationContext());
            mLocalFollowUp = new LocalFollowUp(store);
        } catch (MessagingException e) {
            Log.e(K9.LOG_TAG, "Unable to retrieve message", e);
        }
    }

    public void populateListView(List<FollowUp> items) {
        FollowUpAdapter adapter = new FollowUpAdapter(items);
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
        Preferences prefs = Preferences.getPreferences(getApplicationContext());
        Message msg = null;
        MessageReference reference = dlg.getReference();
        Account acc = prefs.getAccount(reference.getAccountUuid());
        long folderId = -1;
        try {
            msg = acc.getLocalStore().getFolder(reference.getFolderName()).getMessage(reference.getUid());
            folderId = ((LocalFolder) msg.getFolder()).getId();
        } catch (MessagingException e) {
            e.printStackTrace();
            return;
        }

        FollowUp followUp = new FollowUp(msg.getSubject(), dlg.getRemindTime(), msg, folderId);
        new InsertFollowUp().execute(followUp);
        new LoadFollowUp().execute();
        ((BaseAdapter)getListView().getAdapter()).notifyDataSetChanged();
    }

    class FollowUpAdapter extends ArrayAdapter<FollowUp> {
        public FollowUpAdapter(List<FollowUp> followUps) {
            super(FollowUpList.this, 0, followUps);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            FollowUp item = getItem(position);
            final View view;
            if (convertView != null) {
                view = convertView;
            } else {
                view = getLayoutInflater().inflate(R.layout.followup_list_item, parent, false);
            }

            TextView subject = (TextView) view.findViewById(R.id.subject);
            TextView date = (TextView) view.findViewById(R.id.date);
            subject.setText(item.getTitle());
            CharSequence formatedDate = DateUtils.getRelativeTimeSpanString(getApplication(), item.getRemindTime().getTime());
            date.setText(formatedDate);

            return view;
        }
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
                } catch (MessagingException e) {
                    Log.e(K9.LOG_TAG, "Unable to insert followup", e);
                }
            }
            return null;
        }
    }
}
