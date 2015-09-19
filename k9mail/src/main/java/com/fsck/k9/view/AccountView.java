package com.fsck.k9.view;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import de.fau.cs.mad.smile.android.R;

import static butterknife.ButterKnife.findById;

public class AccountView extends RelativeLayout {
    private TextView name;
    private TextView mail;

    public AccountView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        name = findById(this, R.id.name);
        mail = findById(this, R.id.email);
    }

    public TextView getName() {
        return name;
    }

    public TextView getMail() {
        return mail;
    }
}
