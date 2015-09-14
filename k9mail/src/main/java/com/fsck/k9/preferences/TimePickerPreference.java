/* Sourced from http://www.ebessette.com/d/TimePickerPreference
 * on 2010-11-27 by jessev
 */

package com.fsck.k9.preferences;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.v7.preference.DialogPreference;
import android.support.v7.preference.PreferenceDialogFragmentCompat;
import android.text.format.DateFormat;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
import android.widget.TimePicker;

import org.joda.time.DateTime;
import org.joda.time.Period;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.util.Locale;

import de.fau.cs.mad.smile.android.R;

/**
 * A preference type that allows a user to choose a time
 */
public class TimePickerPreference extends DialogPreference {

    /**
     * The default value for this preference
     */
    private long defaultValue;

    /**
     * @param context
     * @param attrs
     */
    public TimePickerPreference(final Context context, final AttributeSet attrs) {
        super(context, attrs);
        setPersistent(true);
    }

    private void colorizeIcon() {
        final Context context = getContext();
        final Drawable icon = getIcon();

        if(icon != null) {
            TypedValue typedValue = new TypedValue();
            Resources.Theme theme = context.getTheme();
            theme.resolveAttribute(R.attr.color, typedValue, true);
            int color = Color.BLUE;
            icon.setColorFilter(color, PorterDuff.Mode.SRC_IN);
        }
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

    public void setTime(DateTime time) {
        persistLong(time.getMillis());
    }
}

