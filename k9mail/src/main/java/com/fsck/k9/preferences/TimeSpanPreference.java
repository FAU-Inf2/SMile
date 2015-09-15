package com.fsck.k9.preferences;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v7.preference.DialogPreference;
import android.support.v7.preference.PreferenceDialogFragmentCompat;
import android.util.AttributeSet;
import android.util.TypedValue;

import com.fsck.k9.fragment.SmileDialogPreference;

import org.joda.time.Period;

import de.fau.cs.mad.smile.android.R;

public class TimeSpanPreference extends DialogPreference implements SmileDialogPreference {

    private static final int DEFAULT_VALUE = 15;
    private int timeSpan;
    private TimeUnit timeUnit;

    public TimeSpanPreference(Context context, AttributeSet attrs) {
        super(context, attrs);

        setDialogLayoutResource(R.layout.timespan_preference);
        setPositiveButtonText(android.R.string.ok);
        setNegativeButtonText(android.R.string.cancel);
        timeUnit = TimeUnit.MINUTE;
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
        myState.timeSpan = timeSpan;
        myState.timeUnit = timeUnit.name();

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
        timeUnit = TimeUnit.valueOf(myState.timeUnit);
        timeSpan = myState.timeSpan;
    }

    public void setTimeSpan(int timeSpan) {
        this.timeSpan = timeSpan;
        persistInt(timeSpan);
    }

    public Period getPeriod() {
        if(timeUnit == TimeUnit.MINUTE) {
            return Period.minutes(timeSpan);
        } else if (timeUnit == TimeUnit.HOUR) {
            return Period.hours(timeSpan);
        } else if (timeUnit == TimeUnit.DAY) {
            return Period.days(timeSpan);
        }

        return null;
    }

    public void setTimeUnit(TimeUnit timeUnit) {
        this.timeUnit = timeUnit;
        persistString(timeUnit.toString());
    }

    @Override
    public PreferenceDialogFragmentCompat getDialogInstance() {
        return new TimeSpanPreferenceDialog();
    }

    public TimeUnit getTimeUnit() {
        return timeUnit;
    }

    public int getTimeSpan() {
        return timeSpan;
    }

    public enum TimeUnit {
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