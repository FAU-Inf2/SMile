package com.fsck.k9.adapter;

import android.support.v7.widget.RecyclerView;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.fsck.k9.mail.RemindMe;

import java.util.List;

import de.fau.cs.mad.smile.android.R;

public class RemindMeAdapter extends RecyclerView.Adapter<RemindMeAdapter.RemindMeViewHolder> {
    public static class RemindMeViewHolder extends RecyclerView.ViewHolder {
        private final TextView subject;
        private final TextView date;

        public RemindMeViewHolder(View itemView) {
            super(itemView);
            subject = (TextView) itemView.findViewById(R.id.subject);
            date = (TextView) itemView.findViewById(R.id.date);
        }

        public final TextView getSubject() {
            return subject;
        }

        public final TextView getDate() {
            return date;
        }
    }

    private List<RemindMe> mRemindMes;

    public RemindMeAdapter(List<RemindMe> remindMes) {
        mRemindMes = remindMes;
    }

    @Override
    public RemindMeViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View view = inflater.inflate(R.layout.remindme_list_item, parent, false);
        return new RemindMeViewHolder(view);
    }

    @Override
    public void onBindViewHolder(RemindMeViewHolder holder, int position) {
        RemindMe item = mRemindMes.get(position);
        holder.getSubject().setText(item.getTitle());
        CharSequence formattedDate = DateUtils.getRelativeTimeSpanString(item.getRemindTime().getTime());
        holder.getDate().setText(formattedDate);
    }

    @Override
    public int getItemCount() {
        return mRemindMes.size();
    }
}
