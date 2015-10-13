package com.fsck.k9.fragment;

import android.app.Dialog;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.ArrayRes;
import android.support.annotation.DrawableRes;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;

import com.fsck.k9.K9;
import com.fsck.k9.mail.Message;
import com.fsck.k9.mail.RemindMe;

import de.fau.cs.mad.smile.android.R;

public class RemindMeDialog extends DialogFragment {

    public interface NoticeDialogListener {
        void onDialogClick(RemindMeDialog dialog);
    }

    public static RemindMeDialog newInstance(Message message, NoticeDialogListener dialogListener) {
        RemindMeDialog dialog = new RemindMeDialog();
        dialog.setRemindMe(new RemindMe());
        dialog.getRemindMe().setReference(message);
        dialog.listener = dialogListener;
        return dialog;
    }

    public static RemindMeDialog newInstance(RemindMe remindMe) {
        RemindMeDialog dialog = new RemindMeDialog();
        dialog.setRemindMe(remindMe);
        return dialog;
    }

    // Use this instance of the interface to deliver action events
    private NoticeDialogListener listener;
    private RemindMe remindMe;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Context context = getActivity();
        GridView view = (GridView) LayoutInflater.from(context).inflate(R.layout.remindme_dialog, null);
        view.setAdapter(new RemindMeIconAdapter(context, R.array.remindme_time_icons, R.array.remindme_time_icon_labels));

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setView(view);
        AlertDialog dialog = builder.create();

        view.setOnItemClickListener(new AlertDialogOnClickListener(dialog));
        return dialog;
    }

    private void setRemindMe(RemindMe remindMe) {
        this.remindMe = remindMe;
    }

    public RemindMe getRemindMe() {
        return remindMe;
    }

    private class AlertDialogOnClickListener implements AdapterView.OnItemClickListener{
        private final AlertDialog dialog;

        public AlertDialogOnClickListener(AlertDialog dialog) {
            this.dialog = dialog;
        }

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            int iconResourceId = (int) parent.getItemAtPosition(position);
            RemindMe remindMe = getRemindMe();
            switch (iconResourceId) {
                case R.drawable.ic_remindme_later_today_black:
                    Log.d(K9.LOG_TAG, "later today was clicked");
                    remindMe.setRemindMeInterval(RemindMe.RemindMeInterval.LATER);
                    break;
                case R.drawable.ic_remindme_this_evening_black:
                    Log.d(K9.LOG_TAG, "this evening was clicked");
                    remindMe.setRemindMeInterval(RemindMe.RemindMeInterval.EVENING);
                    break;
                case R.drawable.ic_remindme_tomorrow_black:
                    Log.d(K9.LOG_TAG, "tomorrow was clicked");
                    remindMe.setRemindMeInterval(RemindMe.RemindMeInterval.TOMORROW);
                    break;
                case R.drawable.ic_remindme_next_week:
                    Log.d(K9.LOG_TAG, "next week was clicked");
                    remindMe.setRemindMeInterval(RemindMe.RemindMeInterval.NEXT_WEEK);
                    break;
                case R.drawable.ic_remindme_next_month_black:
                    Log.d(K9.LOG_TAG, "next month was clicked");
                    remindMe.setRemindMeInterval(RemindMe.RemindMeInterval.NEXT_MONTH);
                    break;
                case R.drawable.ic_remindme_custom_black:
                    Log.d(K9.LOG_TAG, "custom was clicked");
                    remindMe.setRemindMeInterval(RemindMe.RemindMeInterval.CUSTOM);
                    break;
            }

            listener.onDialogClick(RemindMeDialog.this);
            dialog.dismiss();
        }
    }

    static class RemindMeIconAdapter extends BaseAdapter {
        private final int[] icons;
        private final CharSequence[] labels;
        private final Context context;

        public RemindMeIconAdapter(Context context, @ArrayRes int icons, @ArrayRes int labels) {
            this.context = context;
            TypedArray iconArray = context.getResources().obtainTypedArray(icons);
            TypedArray labelArray = context.getResources().obtainTypedArray(labels);
            int iconCount = iconArray.length();
            this.icons = new int[iconCount];
            this.labels = new CharSequence[iconCount];

            for(int i = 0; i < iconCount; i++) {
                this.icons[i] = iconArray.getResourceId(i, R.drawable.ic_remindme_later_today_black);
                this.labels[i] = labelArray.getString(i);
            }

            iconArray.recycle();
            labelArray.recycle();
        }

        @Override
        public int getCount() {
            return icons.length;
        }

        @Override
        public Object getItem(int position) {
            return icons[position];
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            final LayoutInflater inflater = LayoutInflater.from(context);
            ImageView imageView;
            TextView label;
            View itemView = convertView;

            if (convertView == null) {
                itemView = inflater.inflate(R.layout.remindme_dialog_item, null, false);
            }

            imageView = (ImageView) itemView.findViewById(R.id.icon);
            label = (TextView) itemView.findViewById(R.id.icon_label);

            imageView.setImageResource(icons[position]);
            label.setText(labels[position]);

            return itemView;
        }
    }

}

