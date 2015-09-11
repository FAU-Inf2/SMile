package com.fsck.k9.fragment;

import android.preference.ListPreference;
import android.preference.PreferenceFragment;

import com.fsck.k9.activity.K9PreferenceActivity.PreferenceChangeListener;

public class SmilePreferenceFragment extends PreferenceFragment {


    /**
     * Set up the {@link ListPreference} instance identified by {@code key}.
     *
     * @param key
     *         The key of the {@link ListPreference} object.
     * @param value
     *         Initial value for the {@link ListPreference} object.
     *
     * @return The {@link ListPreference} instance identified by {@code key}.
     */
    protected ListPreference setupListPreference(final String key, final String value) {
        final ListPreference prefView = (ListPreference) findPreference(key);
        prefView.setValue(value);
        prefView.setSummary(prefView.getEntry());
        prefView.setOnPreferenceChangeListener(new PreferenceChangeListener(prefView));
        return prefView;
    }

    /**
     * Initialize a given {@link ListPreference} instance.
     *
     * @param prefView
     *         The {@link ListPreference} instance to initialize.
     * @param value
     *         Initial value for the {@link ListPreference} object.
     * @param entries
     *         Sets the human-readable entries to be shown in the list.
     * @param entryValues
     *         The array to find the value to save for a preference when an
     *         entry from entries is selected.
     */
    protected void initListPreference(final ListPreference prefView, final String value,
                                      final CharSequence[] entries, final CharSequence[] entryValues) {
        prefView.setEntries(entries);
        prefView.setEntryValues(entryValues);
        prefView.setValue(value);
        prefView.setSummary(prefView.getEntry());
        prefView.setOnPreferenceChangeListener(new PreferenceChangeListener(prefView));
    }
}
