package com.fsck.k9.preferences;

import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;
import android.support.v7.preference.PreferenceDialogFragmentCompat;

public class CheckBoxListPreferenceDialog extends PreferenceDialogFragmentCompat {
    boolean[] mPendingItems;

    private CheckBoxListPreference getCheckBoxListPreference() {
        return (CheckBoxListPreference) getPreference();
    }

    @Override
    protected void onPrepareDialogBuilder(final AlertDialog.Builder builder) {
        super.onPrepareDialogBuilder(builder);
        CheckBoxListPreference preference = getCheckBoxListPreference();
        CharSequence[] items = preference.getItems();
        boolean[] checkedItems = preference.getCheckedItems();
        mPendingItems = new boolean[items.length];

        System.arraycopy(checkedItems, 0, mPendingItems, 0, checkedItems.length);

        builder.setMultiChoiceItems(items, mPendingItems,
                new DialogInterface.OnMultiChoiceClickListener() {
                    @Override
                    public void onClick(final DialogInterface dialog, final int which,
                                        final boolean isChecked) {
                        mPendingItems[which] = isChecked;
                    }
                });
    }

    @Override
    public void onDialogClosed(boolean positiveResult) {
        if (positiveResult) {
            CheckBoxListPreference preference = getCheckBoxListPreference();
            preference.setCheckedItems(mPendingItems);
        }

        mPendingItems = null;
    }
}
