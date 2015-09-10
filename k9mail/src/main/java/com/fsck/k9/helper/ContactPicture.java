package com.fsck.k9.helper;

import android.content.Context;
import android.util.TypedValue;

import com.fsck.k9.K9;
import com.fsck.k9.activity.misc.ContactPictureLoader;

import de.fau.cs.mad.smile.android.R;

public class ContactPicture {

    public static ContactPictureLoader getContactPictureLoader(Context context) {
        final int defaultBgColor;
        if (!K9.isColorizeMissingContactPictures()) {
            TypedValue outValue = new TypedValue();
            context.getTheme().resolveAttribute(R.attr.contactPictureFallbackDefaultBackgroundColor,
                    outValue, true);
            defaultBgColor = outValue.data;
        } else {
            defaultBgColor = 0;
        }

        return new ContactPictureLoader(context, defaultBgColor);
    }
}
