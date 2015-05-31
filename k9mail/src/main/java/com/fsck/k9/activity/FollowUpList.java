package com.fsck.k9.activity;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.format.DateUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.fsck.k9.mail.FollowUp;
import com.fsck.k9.mailstore.LocalMessage;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import de.fau.cs.mad.smile.android.R;

public class FollowUpList extends K9ListActivity {
    private static final String EXTRA_MESSAGE_REFERENCE = "message_reference";

    public static Intent createFollowUp(Context context,
                                        LocalMessage message) {
        Intent i = new Intent(context, FollowUpList.class);
        i.putExtra(EXTRA_MESSAGE_REFERENCE, message.makeMessageReference());
        return i;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
                view.findViewById(R.id.active_icons).setVisibility(View.GONE);
                view.findViewById(R.id.folders).setVisibility(View.GONE);
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
            List<FollowUp> items = new ArrayList<FollowUp>();
            items.add(new FollowUp("Test 1", new Date()));
            items.add(new FollowUp("Test 2", new Date()));
            items.add(new FollowUp("Test 3", new Date()));
            return items;
        }

        @Override
        protected void onPostExecute(List<FollowUp> followUps) {
            super.onPostExecute(followUps);
            populateListView(followUps);
        }
    }
}
