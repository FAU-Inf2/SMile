package com.fsck.k9.preferences;

import android.content.Context;
import android.support.v7.preference.PreferenceDialogFragmentCompat;
import android.text.format.DateFormat;
import android.view.View;
import android.widget.TimePicker;

import org.joda.time.DateTime;

public class TimePickerPreferenceDialog extends PreferenceDialogFragmentCompat implements TimePicker.OnTimeChangedListener {
    DateTime time;

    private TimePickerPreference getTimePickerPreference() {
        return (TimePickerPreference)getPreference();
    }

    @Override
    protected View onCreateDialogView(Context context) {
        TimePickerPreference preference = getTimePickerPreference();
        TimePicker tp = new TimePicker(context);
        tp.setIs24HourView(DateFormat.is24HourFormat(context));
        time = preference.getTime();
        if(time != null) {
            final int hour = time.getHourOfDay();
            final int minute = time.getMinuteOfHour();
            tp.setCurrentHour(hour);
            tp.setCurrentMinute(minute);
        }

        tp.setOnTimeChangedListener(this);

        return tp;
    }

    @Override
    public void onDialogClosed(boolean positiveResult) {
        TimePickerPreference preference = getTimePickerPreference();
        if (positiveResult) {
            DateTime newValue = getTime();
            if(preference.callChangeListener(newValue)) {
                preference.setTime(newValue);
            }
        }
    }

    /**
     * @see
     * TimePicker.OnTimeChangedListener#onTimeChanged(TimePicker, int, int)
     */
    @Override
    public void onTimeChanged(final TimePicker view, final int hour, final int minute) {
        DateTime newDate = getTime();
        time = newDate.withHourOfDay(hour).withMinuteOfHour(minute);
    }

    public DateTime getTime() {
        return time;
    }
}
