package com.fsck.k9.fragment;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.ArrayRes;
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

import com.fsck.k9.K9;
import com.fsck.k9.mail.Message;
import com.fsck.k9.mail.RemindMe;

import de.fau.cs.mad.smile.android.R;

public class RemindMeDialog extends DialogFragment {

    public interface NoticeDialogListener {
        void onDialogClick(RemindMeDialog dialog);
    }

    public static RemindMeDialog newInstance(Message message) {
        RemindMeDialog dlg = new RemindMeDialog();
        dlg.setRemindMe(new RemindMe());
        dlg.getRemindMe().setReference(message);
        return dlg;
    }

    public static RemindMeDialog newInstance(RemindMe remindMe) {
        RemindMeDialog dlg = new RemindMeDialog();
        dlg.setRemindMe(remindMe);
        return dlg;
    }

    // Use this instance of the interface to deliver action events
    private NoticeDialogListener mListener;
    private RemindMe remindMe;

    // Override the Fragment.onAttach() method to instantiate the NoticeDialogListener
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        // Verify that the host activity implements the callback interface
        try {
            // Instantiate the NoticeDialogListener so we can send events to the host
            mListener = (NoticeDialogListener) activity;
        } catch (ClassCastException e) {
            // The activity doesn't implement the interface, throw exception
            throw new ClassCastException(activity.toString()
                    + " must implement NoticeDialogListener");
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Context context = getActivity();
        GridView view = (GridView) LayoutInflater.from(context).inflate(R.layout.remindme_dialog, null);
        view.setAdapter(new RemindMeIconAdapter(context, R.array.remindme_time_icons));

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
                case R.drawable.ic_remindme_later_today:
                    Log.d(K9.LOG_TAG, "later today was clicked");
                    remindMe.setRemindMeInterval(RemindMe.RemindMeInterval.LATER);
                    break;
                case R.drawable.ic_remindme_this_evening:
                    Log.d(K9.LOG_TAG, "this evening was clicked");
                    remindMe.setRemindMeInterval(RemindMe.RemindMeInterval.EVENING);
                    break;
                case R.drawable.ic_remindme_tomorrow:
                    Log.d(K9.LOG_TAG, "tomorrow was clicked");
                    remindMe.setRemindMeInterval(RemindMe.RemindMeInterval.TOMORROW);
                    break;
                case R.drawable.ic_remindme_next_week:
                    Log.d(K9.LOG_TAG, "next week was clicked");
                    remindMe.setRemindMeInterval(RemindMe.RemindMeInterval.NEXT_WEEK);
                    break;
                case R.drawable.ic_remindme_next_month:
                    Log.d(K9.LOG_TAG, "next month was clicked");
                    remindMe.setRemindMeInterval(RemindMe.RemindMeInterval.NEXT_MONTH);
                    break;
                case R.drawable.ic_remindme_custom:
                    Log.d(K9.LOG_TAG, "custom was clicked");
                    remindMe.setRemindMeInterval(RemindMe.RemindMeInterval.CUSTOM);
                    break;
            }

            mListener.onDialogClick(RemindMeDialog.this);
            dialog.dismiss();
        }
    }

    static class RemindMeIconAdapter extends BaseAdapter {
        private final TypedArray icons;
        private final Context context;

        public RemindMeIconAdapter(Context context, @ArrayRes int icons) {
            this.context = context;
            this.icons = context.getResources().obtainTypedArray(icons);
        }

        @Override
        public int getCount() {
            return icons.length();
        }

        @Override
        public Object getItem(int position) {
            return icons.getResourceId(position, -1);
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ImageView imageView;
            if (convertView == null) {
                // if it's not recycled, initialize some attributes
                imageView = new ImageView(context);
                imageView.setBackgroundColor(Color.BLACK);
                imageView.setColorFilter(Color.RED);
                Resources resources = context.getResources();
                DisplayMetrics metrics = resources.getDisplayMetrics();
                int width = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 80, metrics);
                imageView.setLayoutParams(new GridView.LayoutParams(width, width));
                imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
                int padding = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8, metrics);
                imageView.setPadding(padding, padding, padding, padding);
            } else {
                imageView = (ImageView) convertView;
            }

            final int iconId = icons.getResourceId(position, R.drawable.ic_remindme_tomorrow);
            imageView.setImageResource(iconId);
            return imageView;

        }
    }

}

