package com.fsck.k9.preferences;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RectShape;
import android.preference.Preference;
import android.view.View;
import android.widget.ImageView;

import com.fsck.k9.Account;

import de.fau.cs.mad.smile.android.R;

public class AccountPreference extends Preference {
    private final Account account;

    public AccountPreference(Context context, Account account, OnPreferenceClickListener listener) {
        super(context);
        this.account = account;
        setPersistent(false);
        setTitle(account.getDescription());
        setOnPreferenceClickListener(listener);
    }

    public Account getAccount() {
        return account;
    }
}
