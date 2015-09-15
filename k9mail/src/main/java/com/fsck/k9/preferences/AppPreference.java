package com.fsck.k9.preferences;

import android.content.Context;
import android.support.v7.preference.DialogPreference;
import android.support.v7.preference.PreferenceDialogFragmentCompat;
import android.util.AttributeSet;

import com.fsck.k9.fragment.SmileDialogPreference;

public class AppPreference extends DialogPreference implements SmileDialogPreference {
    public AppPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public PreferenceDialogFragmentCompat getDialogInstance() {
        return null;
    }
}
