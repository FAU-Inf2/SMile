package com.fsck.k9.activity;

import android.content.Context;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.fsck.k9.mail.RemindMe;

import java.util.List;

import de.fau.cs.mad.smile.android.R;

class RemindMeAdapter extends ArrayAdapter<RemindMe> {
    public RemindMeAdapter(Context context, List<RemindMe> remindMes) {
        super(context, R.layout.remindme_list_item, remindMes);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        RemindMe item = (RemindMe) getItem(position);
        LayoutInflater inflater = LayoutInflater.from(getContext());

        final View view;
        if (convertView != null) {
            view = convertView;
        } else {
            view = inflater.inflate(R.layout.remindme_list_item, parent, false);
        }

        TextView subject = (TextView) view.findViewById(R.id.subject);
        TextView date = (TextView) view.findViewById(R.id.date);
        subject.setText(item.getTitle());
        CharSequence formatedDate = DateUtils.getRelativeTimeSpanString(getContext(), item.getRemindTime().getTime());
        date.setText(formatedDate);

        return view;
    }
}
