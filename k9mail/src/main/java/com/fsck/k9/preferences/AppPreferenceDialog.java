package com.fsck.k9.preferences;

import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.preference.PreferenceDialogFragmentCompat;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.TextView;

import java.util.List;

/*
 Taken from https://github.com/open-keychain/openpgp-api/blob/master/openpgp-api/src/main/java/org/openintents/openpgp/util/OpenPgpAppPreference.java
 uses PreferenceDialogFragmentCompat instead of DialogPreference
 */
public class AppPreferenceDialog extends PreferenceDialogFragmentCompat {
    public static AppPreferenceDialog newInstance(String key) {
        AppPreferenceDialog dialog = new AppPreferenceDialog();
        Bundle args = new Bundle();
        args.putString("key", key);
        dialog.setArguments(args);
        return dialog;
    }

    private AppPreference getAppPreference() {
        return (AppPreference)getPreference();
    }

    @Override
    protected void onPrepareDialogBuilder(AlertDialog.Builder builder) {
        final AppPreference preference = getAppPreference();
        preference.populateAppList();
        final List<AppEntry> entries = preference.getEntries();
        ListAdapter listAdapter = new AppEntryArrayAdapter(getContext(), entries);
        builder.setSingleChoiceItems(listAdapter, getIndexOfProviderList(preference.getSelectedPackage()),
                new AppPreferenceDialogOnClickListener(getContext(), entries, preference));

        /*
         * The typical interaction for list-based dialogs is to have click-on-an-item dismiss the
         * dialog instead of the user having to press 'Ok'.
         */
        builder.setPositiveButton(null, null);
    }

    private int getIndexOfProviderList(String packageName) {
        List<AppEntry> entries = getAppPreference().getEntries();
        for (AppEntry app : entries) {
            if (app.getPackageName().equals(packageName)) {
                return entries.indexOf(app);
            }
        }

        // default is "none"
        return 0;
    }

    @Override
    public void onDialogClosed(boolean positiveResult) {
        if (positiveResult && (getAppPreference().getSelectedPackage() != null)) {
            save();
        }
    }

    private void save() {
        // Give the client a chance to ignore this change if they deem it invalid
        AppPreference preference = getAppPreference();
        if (!preference.callChangeListener(preference.getSelectedPackage())) {
            // They don't want the value to be set
            return;
        }

        preference.setValue(preference.getSelectedPackage());
    }

    private static class AppEntryArrayAdapter extends ArrayAdapter<AppEntry> {
        private final static int ICON_SIZE = 50;
        private final Resources resources;

        public AppEntryArrayAdapter(Context context, List<AppEntry> entries) {
            super(context, android.R.layout.select_dialog_singlechoice, android.R.id.text1, entries);
            resources = getContext().getResources();
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            // User super class to create the View
            View v = super.getView(position, convertView, parent);
            TextView tv = (TextView) v.findViewById(android.R.id.text1);

            // Put the image on the TextView
            Drawable icon = getItem(position).getIcon();
            DisplayMetrics displayMetrics = resources.getDisplayMetrics();
            float scale = displayMetrics.density;
            int iconSizeInPx = (int) (ICON_SIZE * scale);
            icon.setBounds(0, 0, iconSizeInPx, iconSizeInPx);
            tv.setCompoundDrawables(icon, null, null, null);

            // Add margin between image and text (support various screen densities)
            int dp10 = (int) (10 * displayMetrics.density + 0.5f);
            tv.setCompoundDrawablePadding(dp10);

            return v;
        }
    }

    private class AppPreferenceDialogOnClickListener implements DialogInterface.OnClickListener {
        private final Context context;
        private final List<AppEntry> entries;
        private final AppPreference preference;

        public AppPreferenceDialogOnClickListener(Context context, List<AppEntry> entries, AppPreference preference) {
            this.context = context;
            this.entries = entries;
            this.preference = preference;
        }

        @Override
        public void onClick(DialogInterface dialog, int which) {
            AppEntry entry = entries.get(which);

            if (entry.getIntent() != null) {
                /*
                 * Intents are called as activity
                 *
                 * Current approach is to assume the user installed the app.
                 * If he does not, the selected package is not valid.
                 *
                 * However  applications should always consider this could happen,
                 * as the user might remove the currently used OpenPGP app.
                 */
                context.startActivity(entry.getIntent());
                return;
            }

            preference.setSelectedPackage(entry.getPackageName());

            /*
             * Clicking on an item simulates the positive button click, and dismisses
             * the dialog.
             */
            AppPreferenceDialog.this.onClick(dialog, DialogInterface.BUTTON_POSITIVE);
            dialog.dismiss();
        }
    }
}
