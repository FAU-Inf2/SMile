package com.fsck.k9.activity;

import android.content.Context;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.fsck.k9.mail.FollowUp;

import java.util.List;

import de.fau.cs.mad.smile.android.R;

class FollowUpAdapter extends ArrayAdapter<FollowUp> {
    public FollowUpAdapter(Context context, List<FollowUp> followUps) {
        super(context, R.layout.followup_list_item, followUps);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        FollowUp item = getItem(position);
        LayoutInflater inflater = LayoutInflater.from(getContext());

        final View view;
        if (convertView != null) {
            view = convertView;
        } else {
            view = inflater.inflate(R.layout.followup_list_item, parent, false);
        }

        TextView subject = (TextView) view.findViewById(R.id.subject);
        TextView date = (TextView) view.findViewById(R.id.date);
        subject.setText(item.getTitle());
        CharSequence formatedDate = DateUtils.getRelativeTimeSpanString(getContext(), item.getRemindTime().getTime());
        date.setText(formatedDate);

        return view;
    }
}
