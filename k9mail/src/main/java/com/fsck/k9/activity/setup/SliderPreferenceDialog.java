package com.fsck.k9.activity.setup;

import android.content.Context;
import android.support.v7.preference.PreferenceDialogFragmentCompat;
import android.view.View;
import android.widget.SeekBar;

import de.fau.cs.mad.smile.android.R;

public class SliderPreferenceDialog extends PreferenceDialogFragmentCompat {
    protected final static int SEEKBAR_RESOLUTION = 10000;
    protected int mSeekBarValue;

    private SliderPreference getSliderPreference() {
        return (SliderPreference)getPreference();
    }

    @Override
    protected View onCreateDialogView(Context context) {
        final SliderPreference preference = getSliderPreference();
        mSeekBarValue = (int) (preference.getValue() * SEEKBAR_RESOLUTION);
        View view = super.onCreateDialogView(context);
        SeekBar seekbar = (SeekBar) view.findViewById(R.id.slider_preference_seekbar);
        seekbar.setMax(SEEKBAR_RESOLUTION);
        seekbar.setProgress(mSeekBarValue);
        seekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    mSeekBarValue = progress;
                    preference.callChangeListener((float) mSeekBarValue / SEEKBAR_RESOLUTION);
                }
            }
        });
        return view;
    }

    @Override
    public void onDialogClosed(boolean positiveResult) {
        final SliderPreference preference = getSliderPreference();
        final float newValue = (float) mSeekBarValue / SEEKBAR_RESOLUTION;
        if (positiveResult && preference.callChangeListener(newValue)) {
            preference.setValue(newValue);
        }
    }
}
