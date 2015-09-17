package com.fsck.k9.preferences;

import android.support.v7.preference.PreferenceDialogFragmentCompat;
import android.view.View;
import android.widget.NumberPicker;

import org.joda.time.Period;

import butterknife.Bind;
import butterknife.ButterKnife;
import de.fau.cs.mad.smile.android.R;

public class TimeSpanPreferenceDialog extends PreferenceDialogFragmentCompat {
    @Bind(R.id.pref_hour_picker)
    NumberPicker hourPicker;
    @Bind(R.id.pref_minute_picker)
    NumberPicker minutePicker;

    private TimeSpanPreference getTimeSpanPreference() {
        return (TimeSpanPreference)getPreference();
    }

    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);
        ButterKnife.bind(this, view);
        hourPicker.setMaxValue(24);
        hourPicker.setMinValue(0);
        minutePicker.setMinValue(1);
        minutePicker.setMaxValue(60);
        TimeSpanPreference preference = getTimeSpanPreference();
        Period period = preference.getPeriod();
        hourPicker.setValue(period.getHours());
        minutePicker.setValue(period.getMinutes());
    }

    @Override
    public void onDialogClosed(boolean positiveResult) {
        TimeSpanPreference preference = getTimeSpanPreference();
        if (positiveResult) {
            Period period = Period.hours(hourPicker.getValue());
            period = period.plusMinutes(minutePicker.getValue());
            if(preference.callChangeListener(period)) {
                preference.setPeriod(period);
            }
        }
    }
}
