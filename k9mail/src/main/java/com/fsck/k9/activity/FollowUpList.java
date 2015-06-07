package com.fsck.k9.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.format.DateUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.fsck.k9.Account;
import com.fsck.k9.FollowUpDialog;
import com.fsck.k9.Preferences;
import com.fsck.k9.mail.FollowUp;
import com.fsck.k9.mail.Message;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mailstore.LocalMessage;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import de.fau.cs.mad.smile.android.R;

public class FollowUpList extends K9ListActivity implements FollowUpDialog.NoticeDialogListener {
    private List<FollowUp> items = new ArrayList<FollowUp>();
    public static final String EXTRA_MESSAGE_REFERENCE = "de.fau.cs.mad.smile.android.MESSAGE_REFERENCE";
    public static final String CREATE_FOLLOWUP = "de.fau.cs.mad.smile.android.CREATE_FOLLOWUP";
    private static final int CREATE_FOLLOWUP_DIALOG = 1;

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

        items.add(new FollowUp("Test 1", new Date()));
        items.add(new FollowUp("Test 2", new Date()));
        items.add(new FollowUp("Test 3", new Date()));

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
    }

    public void populateListView(List<FollowUp> items) {
        FollowUpAdapter adapter = new FollowUpAdapter(items);
        ListView listView = getListView();
        listView.setAdapter(adapter);
        listView.invalidate();
    }

    /**
     * Reload list of accounts when this activity is resumed.
     */
    @Override
    public void onResume() {
        super.onResume();
        new LoadFollowUp().execute();
    }

    @Override
    public void onDialogClick(DialogFragment dialog) {
        FollowUpDialog dlg = (FollowUpDialog)dialog;
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());
        calendar.add(Calendar.MINUTE, dlg.getTimeValue());
        Preferences prefs = Preferences.getPreferences(getApplicationContext());
        Message msg = null;
        MessageReference reference = dlg.getReference();
        Account acc = prefs.getAccount(reference.getAccountUuid());

        try {
            msg = acc.getLocalStore().getFolder(reference.getFolderName()).getMessage(reference.getUid());
        } catch (MessagingException e) {
            e.printStackTrace();
        }

        items.add(new FollowUp(msg.getSubject(), calendar.getTime()));
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
            // TODO: query sqlite db
            return items;
        }

        @Override
        protected void onPostExecute(List<FollowUp> followUps) {
            super.onPostExecute(followUps);
            populateListView(followUps);
        }
    }


}
