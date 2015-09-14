package com.fsck.k9.preferences;

import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Parcel;
import android.os.Parcelable;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.NumberPicker;
import android.widget.Spinner;

import org.joda.time.Period;

import java.util.ArrayList;

import de.fau.cs.mad.smile.android.R;

public class TimeSpanPreference extends DialogPreference {

    private static final int DEFAULT_VALUE = 15;
    private int mTimeSpan;
    private TimeUnit mTimeUnit;
    private NumberPicker numberPicker;
    private Spinner timeUnitSpinner;

    public TimeSpanPreference(Context context, AttributeSet attrs) {
        super(context, attrs);

        setDialogLayoutResource(R.layout.timespan_preference);
        setPositiveButtonText(android.R.string.ok);
        setNegativeButtonText(android.R.string.cancel);
        mTimeUnit = TimeUnit.MINUTE;
    }

    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);
        final Context context = getContext();

        numberPicker = (NumberPicker) view.findViewById(R.id.pref_number_picker);
        timeUnitSpinner = (Spinner) view.findViewById(R.id.pref_timeunit_spinner);

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
                mTimeUnit = (TimeUnit) parent.getItemAtPosition(position);
                populateNumberPicker();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                mTimeUnit = TimeUnit.MINUTE;
            }
        });

        numberPicker.setMinValue(1);

        populateNumberPicker();
        timeUnitSpinner.setSelection(timeUnits.indexOf(mTimeUnit));
        timeUnitSpinner.invalidate();
    }

    @Override
    protected void onBindView(View view) {
        super.onBindView(view);
        colorizeIcon();
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

    private void populateNumberPicker() {
        switch (mTimeUnit) {
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

        numberPicker.setValue(mTimeSpan);
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        super.onClick(dialog, which);
        if (which == DialogInterface.BUTTON_POSITIVE) {
            mTimeSpan = numberPicker.getValue();
            mTimeUnit = (TimeUnit) timeUnitSpinner.getSelectedItem();
        }
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        if (positiveResult) {
            persistInt(mTimeSpan);
            persistString(mTimeUnit.toString());
        }
    }

    @Override
    protected void onSetInitialValue(boolean restorePersistedValue, Object defaultValue) {
        super.onSetInitialValue(restorePersistedValue, defaultValue);
    }

    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        return a.getInteger(index, DEFAULT_VALUE);
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        final Parcelable superState = super.onSaveInstanceState();
        // Check whether this Preference is persistent (continually saved)
        if (isPersistent()) {
            // No need to save instance state since it's persistent,
            // use superclass state
            return superState;
        }

        // Create instance of custom BaseSavedState
        final SavedState myState = new SavedState(superState);
        // Set the state's value with the class member that holds current
        // setting value
        myState.timeSpan = mTimeSpan;
        myState.timeUnit = mTimeUnit.name();

        return myState;

    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        if (state == null || !state.getClass().equals(SavedState.class)) {
            // Didn't save the state, so call superclass
            super.onRestoreInstanceState(state);
            return;
        }

        // Cast state to custom BaseSavedState and pass to superclass
        SavedState myState = (SavedState) state;
        super.onRestoreInstanceState(myState.getSuperState());

        // Set this Preference's widget to reflect the restored state
        numberPicker.setValue(myState.timeSpan);
        TimeUnit savedTimeUnit = TimeUnit.valueOf(myState.timeUnit);

        for(int i = 0; i < timeUnitSpinner.getCount(); i++) {
            TimeUnit tmp = (TimeUnit) timeUnitSpinner.getItemAtPosition(i);
            if(tmp == savedTimeUnit) {
                timeUnitSpinner.setSelection(i);
                break;
            }
        }

        mTimeSpan = myState.timeSpan;
    }

    public Period getPeriod() {
        if(mTimeUnit == TimeUnit.MINUTE) {
            return Period.minutes(mTimeSpan);
        } else if (mTimeUnit == TimeUnit.HOUR) {
            return Period.hours(mTimeSpan);
        } else if (mTimeUnit == TimeUnit.DAY) {
            return Period.days(mTimeSpan);
        }

        return null;
    }

    enum TimeUnit {
        HOUR("hour"),
        MINUTE("minute"),
        DAY("day");

        private String description;

        TimeUnit(String description) {
            this.description = description;
        }

        @Override
        public String toString() {
            return description;
        }
    }

    private static class SavedState extends BaseSavedState {
        // Standard creator object using an instance of this class
        public static final Parcelable.Creator<SavedState> CREATOR =
                new Parcelable.Creator<SavedState>() {

                    public SavedState createFromParcel(Parcel in) {
                        return new SavedState(in);
                    }

                    public SavedState[] newArray(int size) {
                        return new SavedState[size];
                    }
                };
        // Member that holds the setting's value
        // Change this data type to match the type saved by your Preference
        int timeSpan;
        String timeUnit;

        public SavedState(Parcelable superState) {
            super(superState);
        }

        public SavedState(Parcel source) {
            super(source);
            // Get the current preference's value
            timeSpan = source.readInt();  // Change this to read the appropriate data type
            timeUnit = source.readString();
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            // Write the preference's value
            dest.writeInt(timeSpan);  // Change this to write the appropriate data type
            dest.writeString(timeUnit);
        }
    }
}
