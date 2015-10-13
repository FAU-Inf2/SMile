package com.fsck.k9.fragment;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.widget.DatePicker;
import android.widget.TimePicker;

import com.fsck.k9.K9;
import com.fsck.k9.mail.RemindMe;

import org.joda.time.DateTime;

import java.lang.ref.WeakReference;
import java.util.Calendar;
import java.util.Date;

public class RemindMeListener implements RemindMeDialog.NoticeDialogListener,
        TimePickerDialog.OnTimeSetListener,
        DatePickerDialog.OnDateSetListener {

    final WeakReference<MessageListFragment> weakReference;

    public RemindMeListener(MessageListFragment fragment) {
        this.weakReference = new WeakReference<>(fragment);
    }

    @Override
    public void onDialogClick(RemindMeDialog dialog) {
        MessageListFragment fragment = weakReference.get();
        if(fragment == null) {
            return;
        }

        Log.i(K9.LOG_TAG, "RemindMeList.onDialogClick");
        currentRemindMe = dialog.getRemindMe();

        if(currentRemindMe.getRemindMeInterval() == RemindMe.RemindMeInterval.CUSTOM) {
            onDateSetCalled = false;
            onTimeSetCalled = false;

            final String datePickerTag = "remindMeDatePicker";
            FragmentManager fragmentManager = fragment.getFragmentManager();
            FragmentTransaction ft = fragmentManager.beginTransaction();
            Fragment prev = fragmentManager.findFragmentByTag(datePickerTag);

            if (prev != null) {
                ft.remove(prev);
            }

            ft.addToBackStack(datePickerTag);

            RemindMeDatePickerDialog datePickerDialog = RemindMeDatePickerDialog.newInstance(this);
            datePickerDialog.show(ft, datePickerTag);
        } else {
            currentRemindMe.setRemindTime(getDelay(currentRemindMe.getRemindMeInterval()));
            fragment.add(currentRemindMe);
        }
    }

    private Date getDelay(RemindMe.RemindMeInterval interval) {
        DateTime delay = DateTime.now();

        switch (interval) {
            case LATER:
                delay.plusMinutes(10);
                break;
            case EVENING:
                delay.plusMinutes(30);
                break;
            case TOMORROW:
                delay.plusDays(1);
                break;
        }

        return delay.toDate();
    }

    private boolean onTimeSetCalled = false;
    private boolean onDateSetCalled = false;
    private RemindMe currentRemindMe;

    @Override
    public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
        MessageListFragment fragment = weakReference.get();
        if(fragment == null) {
            return;
        }

        if(onDateSetCalled) {
            return;
        }

        onDateSetCalled = true;

        final String timePickerTag = "remindMeTimePicker";
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.YEAR, year);
        calendar.set(Calendar.MONTH, monthOfYear);
        calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);

        Log.d(K9.LOG_TAG, "Selected date: " + calendar.getTime());
        currentRemindMe.setRemindTime(calendar.getTime());
        RemindMeTimePickerDialog timePickerDialog = RemindMeTimePickerDialog.newInstance(this);
        timePickerDialog.show(fragment.getFragmentManager(), timePickerTag);
    }

    @Override
    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
        MessageListFragment fragment = weakReference.get();
        if(fragment == null) {
            return;
        }

        if(onTimeSetCalled) {
            return;
        }

        onTimeSetCalled = true;

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(currentRemindMe.getRemindTime());
        calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
        calendar.set(Calendar.MINUTE, minute);
        Log.d(K9.LOG_TAG, "Selected time: " + calendar.getTime());
        Date minDate = new Date(System.currentTimeMillis() + 15 * 1000l);

        // do not accept dates in the past -- earliest is 15 seconds in the future
        if(calendar.getTime().before(minDate)) {
            Log.d(K9.LOG_TAG, "Selected date was before min date -- new date: " + minDate);
            currentRemindMe.setRemindTime(minDate);
        } else {
            currentRemindMe.setRemindTime(calendar.getTime());
        }

        fragment.add(currentRemindMe);
    }
}
