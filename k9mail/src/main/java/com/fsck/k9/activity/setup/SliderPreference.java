/*
 * Copyright 2012 Jay Weisskopf
 *
 * Licensed under the MIT License (see LICENSE.txt)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
 * of the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies
 * or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
 * PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE
 * FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,
 * ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 * Source: https://github.com/jayschwa/AndroidSliderPreference
 */

package com.fsck.k9.activity.setup;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.ArrayRes;
import android.support.v7.preference.DialogPreference;
import android.support.v7.preference.PreferenceDialogFragmentCompat;
import android.util.AttributeSet;
import android.view.View;
import android.widget.SeekBar;

import de.fau.cs.mad.smile.android.R;


/**
 * @author Jay Weisskopf
 */
public class SliderPreference extends DialogPreference {
    private static final String STATE_KEY_SEEK_BAR_VALUE = "value";

    protected float mValue;
    protected CharSequence[] mSummaries;

    /**
     * @param context
     * @param attrs
     */
    public SliderPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setDialogLayoutResource(R.layout.slider_preference_dialog);
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.SliderPreference);
        try {
            setSummary(a.getTextArray(R.styleable.SliderPreference_android_summary));
        } catch (Exception e) {
            // Do nothing
        }
        a.recycle();
    }

    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        return a.getFloat(index, 0);
    }

    @Override
    protected void onSetInitialValue(boolean restoreValue, Object defaultValue) {
        setValue(restoreValue ? getPersistedFloat(mValue) : (Float) defaultValue);
    }

    @Override
    public CharSequence getSummary() {
        if (mSummaries != null && mSummaries.length > 0) {
            int index = (int) (mValue * mSummaries.length);
            index = Math.min(index, mSummaries.length - 1);
            return mSummaries[index];
        } else {
            return super.getSummary();
        }
    }

    public void setSummary(CharSequence[] summaries) {
        mSummaries = summaries;
    }

    @Override
    public void setSummary(CharSequence summary) {
        super.setSummary(summary);
        mSummaries = null;
    }

    @Override
    public void setSummary(@ArrayRes int summaryResId) {
        try {
            setSummary(getContext().getResources().getStringArray(summaryResId));
        } catch (Exception e) {
            super.setSummary(summaryResId);
        }
    }

    public float getValue() {
        return mValue;
    }

    public void setValue(float value) {
        value = Math.max(0, Math.min(value, 1)); // clamp to [0, 1]
        if (shouldPersist()) {
            persistFloat(value);
        }
        if (value != mValue) {
            mValue = value;
            notifyChanged();
        }
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
        myState.sliderValue = mValue;

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
        mValue = myState.sliderValue;
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
        float sliderValue;

        public SavedState(Parcelable superState) {
            super(superState);
        }

        public SavedState(Parcel source) {
            super(source);
            // Get the current preference's value
            sliderValue = source.readFloat();
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            // Write the preference's value
            dest.writeFloat(sliderValue);
        }
    }
}

