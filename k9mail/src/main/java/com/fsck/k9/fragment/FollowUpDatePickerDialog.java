package com.fsck.k9.fragment;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.os.Bundle;

import java.util.Calendar;

public class FollowUpDatePickerDialog extends DialogFragment {
    private DatePickerDialog.OnDateSetListener onDateSetListener;

    public static FollowUpDatePickerDialog newInstance(DatePickerDialog.OnDateSetListener onDateSetListener) {
        FollowUpDatePickerDialog datePickerDialog = new FollowUpDatePickerDialog();
        datePickerDialog.setOnDateSetListener(onDateSetListener);
        return datePickerDialog;
    }

    public DatePickerDialog.OnDateSetListener getOnDateSetListener() {
        return onDateSetListener;
    }

    private void setOnDateSetListener(DatePickerDialog.OnDateSetListener onDateSetListener) {
        this.onDateSetListener = onDateSetListener;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Use the current date as the default date in the picker
        final Calendar c = Calendar.getInstance();
        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH);
        int day = c.get(Calendar.DAY_OF_MONTH);

        // Create a new instance of DatePickerDialog and return it
        return new DatePickerDialog(getActivity(), this.getOnDateSetListener(), year, month, day);
    }

}
