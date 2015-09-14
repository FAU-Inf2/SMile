package com.fsck.k9.preferences;

import android.support.v7.preference.PreferenceDialogFragmentCompat;

public class TimeSpanPreferenceDialog extends PreferenceDialogFragmentCompat {

    private TimeSpanPreference getTimeSpanPreference() {
        return (TimeSpanPreference)getPreference();
    }

    @Override
    public void onDialogClosed(boolean positiveResult) {
        TimeSpanPreference preference = getTimeSpanPreference();
        if (positiveResult) {
            preference.setTimeSpan(preference.getNumberPicker().getValue());
            preference.setTimeUnit((TimeSpanPreference.TimeUnit) preference.getTimeUnitSpinner().getSelectedItem());
        }
    }
}
