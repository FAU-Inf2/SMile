/* Sourced from http://www.ebessette.com/d/TimePickerPreference
 * on 2010-11-27 by jessev
 */

package com.fsck.k9.preferences;

import android.content.Context;
import android.preference.DialogPreference;
import android.support.annotation.NonNull;
import android.text.format.DateFormat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.TimePicker;

import com.fsck.k9.K9;

import org.joda.time.DateTime;
import org.joda.time.Period;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.util.Locale;

/**
 * A preference type that allows a user to choose a time
 */
public class TimePickerPreference extends DialogPreference implements
    TimePicker.OnTimeChangedListener {

    /**
     * The default value for this preference
     */
    private long defaultValue;
    private DateTime originalValue;

    /**
     * @param context
     * @param attrs
     */
    public TimePickerPreference(final Context context, final AttributeSet attrs) {
        super(context, attrs);
        initialize();
    }

    /**
     * @param context
     * @param attrs
     * @param defStyle
     */
    public TimePickerPreference(final Context context, final AttributeSet attrs,
                                final int defStyle) {
        super(context, attrs, defStyle);
        initialize();
    }

    /**
     * Initialize this preference
     */
    private void initialize() {
        setPersistent(true);
    }

    /*
     * (non-Javadoc)
     *
     * @see android.preference.DialogPreference#onCreateDialogView()
     */
    @Override
    protected View onCreateDialogView() {
        TimePicker tp = new TimePicker(getContext());
        tp.setIs24HourView(DateFormat.is24HourFormat(getContext()));
        originalValue = getTime();
        if(originalValue != null) {
            final int hour = originalValue.getHourOfDay();
            final int minute = originalValue.getMinuteOfHour();
            tp.setCurrentHour(hour);
            tp.setCurrentMinute(minute);
        }

        tp.setOnTimeChangedListener(this);

        return tp;
    }

    /**
     * @see
     * android.widget.TimePicker.OnTimeChangedListener#onTimeChanged(android.widget.TimePicker, int, int)
     */
    @Override
    public void onTimeChanged(final TimePicker view, final int hour, final int minute) {
        DateTime newDate = getTime();
        newDate = newDate.withHourOfDay(hour).withMinuteOfHour(minute);
        persistTime(newDate);
    }

    /**
     * If not a positive result, restore the original value
     * before going to super.onDialogClosed(positiveResult).
     */
    @Override
    protected void onDialogClosed(boolean positiveResult) {
        if (!positiveResult) {
            persistTime(originalValue);
        }

        super.onDialogClosed(positiveResult);
    }

    /**
     * @see android.preference.Preference#setDefaultValue(java.lang.Object)
     */
    @Override
    public void setDefaultValue(final Object defaultValue) {
        // BUG this method is never called if you use the 'android:defaultValue' attribute in your XML preference file, not sure why it isn't

        super.setDefaultValue(defaultValue);

        if (defaultValue instanceof Long) {
            this.defaultValue = (long)defaultValue;
        }

        if (defaultValue instanceof Period) {
            Period period = (Period)defaultValue;
            DateTime javaEpoche = new DateTime(0);
            this.defaultValue = javaEpoche.withPeriodAdded(period, 1).getMillis();
        }

    }

    public DateTime getTime() {
        final long time = getPersistedLong(this.defaultValue);
        return new DateTime(time);
    }

    @NonNull
    public Period getPeriod() {
        final long time = getPersistedLong(this.defaultValue);
        DateTime javaEpoche = new DateTime(0);
        DateTime savedTime = new DateTime(time);
        return new Period(javaEpoche, savedTime);
    }

    public static DateTimeFormatter getDateTimeFormatter() {
        return DateTimeFormat.forPattern("H:m").withLocale(Locale.US);
    }

    private void persistTime(DateTime time) {
        persistLong(time.getMillis());
        callChangeListener(time);
    }
}

