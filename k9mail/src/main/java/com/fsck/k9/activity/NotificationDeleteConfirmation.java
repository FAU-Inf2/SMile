package com.fsck.k9.activity;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.fsck.k9.Account;
import com.fsck.k9.K9;
import com.fsck.k9.Preferences;
import com.fsck.k9.service.NotificationActionService;

import java.io.Serializable;
import java.util.ArrayList;

import de.fau.cs.mad.smile.android.R;

public class NotificationDeleteConfirmation extends AppCompatActivity {
    private final static String EXTRA_ACCOUNT = "account";
    private final static String EXTRA_MESSAGE_LIST = "messages";

    private Account mAccount;
    private ArrayList<MessageReference> mMessageRefs;

    public static PendingIntent getIntent(Context context, final Account account, final Serializable refs) {
        Intent i = new Intent(context, NotificationDeleteConfirmation.class);
        i.putExtra(EXTRA_ACCOUNT, account.getUuid());
        i.putExtra(EXTRA_MESSAGE_LIST, refs);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        return PendingIntent.getActivity(context, account.getAccountNumber(), i, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        setTheme(K9.getK9Theme() == K9.Theme.LIGHT ?
                R.style.Theme_K9_Dialog_Translucent_Light : R.style.Theme_K9_Dialog_Translucent_Dark);

        final Preferences preferences = Preferences.getPreferences(this);
        final Intent intent = getIntent();

        mAccount = preferences.getAccount(intent.getStringExtra(EXTRA_ACCOUNT));
        mMessageRefs = intent.getParcelableArrayListExtra(EXTRA_MESSAGE_LIST);

        if (mAccount == null || mMessageRefs == null || mMessageRefs.isEmpty()) {
            finish();
        } else if (!K9.confirmDeleteFromNotification()) {
            triggerDelete();
            finish();
        } else {
            final int messageCount = mMessageRefs.size();
            Resources resources = getResources();
            ConfirmationDialog dialog = ConfirmationDialog.create(
                R.string.dialog_confirm_delete_title, resources.getQuantityString(
                            R.plurals.dialog_confirm_delete_messages, messageCount, messageCount),
                R.string.dialog_confirm_delete_confirm_button,
                R.string.dialog_confirm_delete_cancel_button,
                new Runnable() {
                    @Override
                    public void run() {
                        triggerDelete();
                        finish();
                    }
                },
                new Runnable() {
                    @Override
                    public void run() {
                        finish();
                    }
                });
            dialog.show(getSupportFragmentManager(), null);
        }
    }

    private void triggerDelete() {
        Intent i = NotificationActionService.getDeleteAllMessagesIntent(this, mAccount, mMessageRefs);
        startService(i);
    }
}
