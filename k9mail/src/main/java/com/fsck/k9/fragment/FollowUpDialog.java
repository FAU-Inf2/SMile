package com.fsck.k9.fragment;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.TimePickerDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.widget.DatePicker;
import android.widget.TimePicker;

import com.fsck.k9.activity.MessageReference;

import java.util.Calendar;
import java.util.Date;

import de.fau.cs.mad.smile.android.R;

public class FollowUpDialog extends DialogFragment implements TimePickerDialog.OnTimeSetListener, DatePickerDialog.OnDateSetListener {

    public interface NoticeDialogListener {
        void onDialogClick(DialogFragment dialog);
    }

    // Use this instance of the interface to deliver action events
    NoticeDialogListener mListener;
    private int mTimeValue = 0;
    private MessageReference reference;
    private Date mRemindTime;

    public FollowUpDialog() {
        this.setRemindTime(new Date());
    }

    @Override
    public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(getRemindTime());
        calendar.set(Calendar.YEAR, year);
        calendar.set(Calendar.MONTH, monthOfYear);
        calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
        setRemindTime(calendar.getTime());
        FollowUpTimePickerDialog timePickerDialog = FollowUpTimePickerDialog.newInstance(this);
        timePickerDialog.show(getFragmentManager(), "followUpTimePicker");
    }

    @Override
    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(getRemindTime());
        calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
        calendar.set(Calendar.MINUTE, minute);
        setRemindTime(calendar.getTime());
        mListener.onDialogClick(FollowUpDialog.this);
    }

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
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setItems(R.array.follow_up_default_time_values, new AlertDialogOnClickListener());
        return builder.create();
    }

    public MessageReference getReference() {
        return reference;
    }

    public void setReference(MessageReference reference) {
        this.reference = reference;
    }

    public Date getRemindTime() {
        return mRemindTime;
    }

    private void setRemindTime(Date remindTime) {
        this.mRemindTime = remindTime;
    }

    private void addMinute(int minute) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(getRemindTime());
        calendar.add(Calendar.MINUTE, minute);
    }

    private class AlertDialogOnClickListener implements DialogInterface.OnClickListener {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            boolean finishedProcessing = false;

            switch(which) {
                case 1: {
                    addMinute(10);
                    finishedProcessing = true;
                    break;
                }
                case 2: {
                    addMinute(30);
                    finishedProcessing = true;
                    break;
                }
                case 3: {
                    FollowUpDatePickerDialog datePickerDialog = FollowUpDatePickerDialog.newInstance(FollowUpDialog.this);
                    datePickerDialog.show(getFragmentManager(), "followUpDatePicker");
                    break;
                }
            }

            if(finishedProcessing) {
                mListener.onDialogClick(FollowUpDialog.this);
            }
        }
    }
}

