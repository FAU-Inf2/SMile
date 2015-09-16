package com.fsck.k9.fragment;

import android.os.Bundle;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.Preference.OnPreferenceChangeListener;
import android.support.v7.preference.PreferenceDialogFragmentCompat;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.support.v7.preference.SwitchPreferenceCompat;
import android.util.Log;

import com.fsck.k9.K9;

public abstract class SmilePreferenceFragment extends PreferenceFragmentCompat {

    /**
     * Set up the {@link ListPreference} instance identified by {@code key}.
     *
     * @param key   The key of the {@link ListPreference} object.
     * @param value Initial value for the {@link ListPreference} object.
     * @return The {@link ListPreference} instance identified by {@code key}.
     */
    protected ListPreference setupListPreference(final String key, final String value) {
        final ListPreference prefView = (ListPreference) findPreference(key);
        if(prefView == null) {
            return null;
        }

        prefView.setValue(value);
        prefView.setSummary(prefView.getEntry());
        prefView.setOnPreferenceChangeListener(new PreferenceChangeListener(prefView));
        return prefView;
    }

    /**
     * Initialize a given {@link ListPreference} instance.
     *
     * @param prefView    The {@link ListPreference} instance to initialize.
     * @param value       Initial value for the {@link ListPreference} object.
     * @param entries     Sets the human-readable entries to be shown in the list.
     * @param entryValues The array to find the value to save for a preference when an
     *                    entry from entries is selected.
     */
    protected void initListPreference(final ListPreference prefView, final String value,
                                      final CharSequence[] entries, final CharSequence[] entryValues) {
        if(prefView == null) {
            return;
        }

        prefView.setEntries(entries);
        prefView.setEntryValues(entryValues);
        prefView.setValue(value);
        prefView.setSummary(prefView.getEntry());
        prefView.setOnPreferenceChangeListener(new PreferenceChangeListener(prefView));
    }

    protected SwitchPreferenceCompat setupSwitchPreference(CharSequence key, boolean checked) {
        SwitchPreferenceCompat preference = (SwitchPreferenceCompat) findPreference(key);
        if(preference == null) {
            return null;
        }

        preference.setChecked(checked);
        return preference;
    }

    @Override
    public void onDisplayPreferenceDialog(Preference preference) {
        if (preference instanceof SmileDialogPreference) {
            SmileDialogPreference dialogPreference = (SmileDialogPreference) preference;
            PreferenceDialogFragmentCompat dialogFragment = dialogPreference.getDialogInstance();
            if (dialogFragment != null) {
                Bundle args = new Bundle();
                args.putString("key", preference.getKey());
                dialogFragment.setArguments(args);
                dialogFragment.setTargetFragment(this, 0);
                dialogFragment.show(getFragmentManager(), null);
                return;
            }
        }

        super.onDisplayPreferenceDialog(preference);
    }

    public abstract SmilePreferenceFragment openPreferenceScreen();

    @Override
    public void onCreatePreferences(Bundle bundle, String s) {
        Log.d(K9.LOG_TAG, "onCreatePreferences called: " + s + ", id: " + getId());
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(K9.LOG_TAG, "SmilePreferenceFragment.onCreate called");
    }

    public static class PreferenceChangeListener implements OnPreferenceChangeListener {
        private ListPreference mPrefView;

        public PreferenceChangeListener(final ListPreference prefView) {
            this.mPrefView = prefView;
        }

        /**
         * Show the preference value in the preference summary field.
         */
        @Override
        public boolean onPreferenceChange(final Preference preference, final Object newValue) {
            final String summary = newValue.toString();
            final int index = mPrefView.findIndexOfValue(summary);
            mPrefView.setSummary(mPrefView.getEntries()[index]);
            mPrefView.setValue(summary);
            return false;
        }
    }
}
