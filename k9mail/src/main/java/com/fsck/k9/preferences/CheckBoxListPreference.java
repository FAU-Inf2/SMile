package com.fsck.k9.preferences;

import android.content.Context;
import android.util.AttributeSet;

public class CheckBoxListPreference extends android.support.v7.preference.DialogPreference {

    private CharSequence[] mItems;
    private boolean[] mCheckedItems;

    /**
     * @param context
     * @param attrs
     * @param defStyle
     */
    public CheckBoxListPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    /**
     * @param context
     * @param attrs
     */
    public CheckBoxListPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setItems(final CharSequence[] items) {
        mItems = items;
    }

    public CharSequence[] getItems() {
        return mItems;
    }

    public void setCheckedItems(final boolean[] items) {
        mCheckedItems = items;
    }

    public boolean[] getCheckedItems() {
        return mCheckedItems;
    }
}