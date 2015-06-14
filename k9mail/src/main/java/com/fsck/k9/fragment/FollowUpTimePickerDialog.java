package com.fsck.k9.fragment;

import android.app.Dialog;
import android.app.DialogFragment;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.text.format.DateFormat;

import java.util.Calendar;

public class FollowUpTimePickerDialog extends DialogFragment {
    TimePickerDialog.OnTimeSetListener mOnTimeSetListener;

    public static FollowUpTimePickerDialog newInstance(TimePickerDialog.OnTimeSetListener onTimeSetListener) {
        FollowUpTimePickerDialog followUpTimePickerDialog = new FollowUpTimePickerDialog();
        followUpTimePickerDialog.setOnTimeSetListener(onTimeSetListener);
        return  followUpTimePickerDialog;
    }

    public TimePickerDialog.OnTimeSetListener getOnTimeSetListener() {
        return mOnTimeSetListener;
    }

    public void setOnTimeSetListener(TimePickerDialog.OnTimeSetListener onTimeSetListener) {
        this.mOnTimeSetListener = onTimeSetListener;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Use the current time as the default values for the picker
        final Calendar c = Calendar.getInstance();
        int hour = c.get(Calendar.HOUR_OF_DAY);
        int minute = c.get(Calendar.MINUTE);

        // Create a new instance of TimePickerDialog and return it
        return new TimePickerDialog(getActivity(), this.getOnTimeSetListener(), hour, minute,
                DateFormat.is24HourFormat(getActivity()));
    }
}