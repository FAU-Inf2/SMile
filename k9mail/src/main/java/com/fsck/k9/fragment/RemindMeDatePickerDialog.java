package com.fsck.k9.fragment;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Build;
import android.os.Bundle;
import android.widget.DatePicker;

import java.util.Calendar;

public class RemindMeDatePickerDialog extends DialogFragment {
    private DatePickerDialog.OnDateSetListener onDateSetListener;

    public static RemindMeDatePickerDialog newInstance(DatePickerDialog.OnDateSetListener onDateSetListener) {
        RemindMeDatePickerDialog datePickerDialog = new RemindMeDatePickerDialog();
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
        final DatePickerDialog datePickerDialog = new DatePickerDialog(getActivity(), this.getOnDateSetListener(), year, month, day);
        if (hasJellyBeanAndAbove()) {
            datePickerDialog.setButton(DialogInterface.BUTTON_POSITIVE,
                    getActivity().getString(android.R.string.ok),
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            DatePicker dp = datePickerDialog.getDatePicker();
                            getOnDateSetListener().onDateSet(dp,
                                    dp.getYear(), dp.getMonth(), dp.getDayOfMonth());
                        }
                    });
            datePickerDialog.setButton(DialogInterface.BUTTON_NEGATIVE,
                    getActivity().getString(android.R.string.cancel),
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {}
                    });
        }
        return datePickerDialog;
    }

    private static boolean hasJellyBeanAndAbove() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN;
    }
}
