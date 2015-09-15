package com.fsck.k9.preferences;

import android.content.Context;
import android.support.v7.preference.PreferenceDialogFragmentCompat;
import android.support.v7.preference.PreferenceViewHolder;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.NumberPicker;
import android.widget.Spinner;

import com.fsck.k9.preferences.TimeSpanPreference.TimeUnit;

import java.util.ArrayList;

import de.fau.cs.mad.smile.android.R;

public class TimeSpanPreferenceDialog extends PreferenceDialogFragmentCompat {
    private NumberPicker numberPicker;
    private Spinner timeUnitSpinner;

    private TimeSpanPreference getTimeSpanPreference() {
        return (TimeSpanPreference)getPreference();
    }

    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);
        final Context context = getContext();

        numberPicker = (NumberPicker) view.findViewById(R.id.pref_number_picker);
        timeUnitSpinner = (Spinner) view.findViewById(R.id.pref_timeunit_spinner);
        final TimeSpanPreference preference = getTimeSpanPreference();

        ArrayList<TimeUnit> timeUnits = new ArrayList<>();
        timeUnits.add(TimeUnit.MINUTE);
        timeUnits.add(TimeUnit.HOUR);
        timeUnits.add(TimeUnit.DAY);
        ArrayAdapter<TimeUnit> adapter = new ArrayAdapter<>(context, android.R.layout.simple_spinner_item, timeUnits);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        timeUnitSpinner.setAdapter(adapter);
        timeUnitSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                TimeUnit timeUnit = (TimeUnit) parent.getItemAtPosition(position);
                preference.setTimeUnit(timeUnit);
                populateNumberPicker();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                preference.setTimeUnit(TimeUnit.MINUTE);
            }
        });

        numberPicker.setMinValue(1);

        populateNumberPicker();
        timeUnitSpinner.setSelection(timeUnits.indexOf(preference.getTimeUnit()));
        timeUnitSpinner.invalidate();
    }

    private void populateNumberPicker() {
        final TimeSpanPreference preference = getTimeSpanPreference();
        switch (preference.getTimeUnit()) {
            case MINUTE:
                final int maxValue = 360;
                final int step = 5;
                final int numberOfEntries = maxValue/step;
                numberPicker.setMaxValue(numberOfEntries);
                String[] values = new String[numberOfEntries];

                for(int i = 5; i <= maxValue; i += step) {
                    values[(i/step) - 1] = String.valueOf(i);
                }

                numberPicker.setDisplayedValues(values);
                break;
            case HOUR:
                numberPicker.setDisplayedValues(null);
                numberPicker.setMaxValue(24);
                break;
            case DAY:
                numberPicker.setDisplayedValues(null);
                numberPicker.setMaxValue(7);
        }

        numberPicker.setValue(preference.getTimeSpan());
    }

    @Override
    public void onDialogClosed(boolean positiveResult) {
        TimeSpanPreference preference = getTimeSpanPreference();
        if (positiveResult) {
            preference.setTimeSpan(numberPicker.getValue());
            preference.setTimeUnit((TimeUnit) timeUnitSpinner.getSelectedItem());
        }
    }
}
