package com.fsck.k9.activity;

import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentSender;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.util.Log;
import android.widget.Toast;

import com.fsck.k9.K9;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.internet.MimeMessage;

import org.openintents.openpgp.OpenPgpError;
import org.openintents.openpgp.util.OpenPgpApi;

import java.lang.ref.WeakReference;

import de.fau.cs.mad.smile.android.R;

public class MessageComposeHandler extends Handler {
    private final WeakReference<MessageCompose> reference;
    private final int MSG_SEND = 7;
    private final int MSG_INTENT_SENDER_FOR_RESULT = 8;
    private final int MSG_OPEN_PGP_ERRORS = 9;
    private final int MSG_SMIME_ERRORS = 10;
    private final String INTENT_KEY = "PENDING_INTENT";
    private final String REQUEST_CODE_KEY = "REQUEST_CODE";
    private final String ERROR_KEY = "PGP_ERROR";
    private final String SMIME_ERROR_KEY = "SMIME_ERROR";

    public MessageComposeHandler(final MessageCompose messageViewFragment) {
        reference = new WeakReference<>(messageViewFragment);
    }

    public void onSend() {
        android.os.Message msg = obtainMessage(MSG_SEND);
        msg.sendToTarget();
    }

    public void startIntentSenderForResult(Intent result, int requestCode) {
        PendingIntent pendingIntent = result.getParcelableExtra(OpenPgpApi.RESULT_INTENT);
        android.os.Message msg = obtainMessage(MSG_INTENT_SENDER_FOR_RESULT);
        Bundle data = new Bundle();
        data.putParcelable(INTENT_KEY, pendingIntent);
        data.putInt(REQUEST_CODE_KEY, requestCode);
        msg.setData(data);
        msg.sendToTarget();
    }

    public void handleOpenPgpErrors(OpenPgpError error) {
        android.os.Message msg = obtainMessage(MSG_OPEN_PGP_ERRORS);
        Bundle data = new Bundle();
        data.putParcelable(ERROR_KEY, error);
        msg.setData(data);
        msg.sendToTarget();
    }

    public void sendSmime(MimeMessage currentMessage) {
        MessageCompose compose = reference.get();
        if (compose == null) {
            return;
        }
        compose.setMessage(currentMessage);
        android.os.Message msg = obtainMessage(MSG_SEND);
        msg.sendToTarget();
    }

    public MimeMessage createMimeMessage() {
        MessageCompose compose = reference.get();
        if (compose == null) {
            return null;
        }
        try {
            return compose.createMessage();
        } catch (MessagingException e) {
            Log.e(K9.LOG_TAG, "Error creating Message", e);
            return null;
        }
    }

    public void smimeError(String message) {
        android.os.Message msg = obtainMessage(MSG_SMIME_ERRORS);
        Bundle data = new Bundle();
        data.putString(SMIME_ERROR_KEY, message);
        msg.setData(data);
        msg.sendToTarget();
    }

    @Override
    public void handleMessage(android.os.Message msg) {
        MessageCompose compose = reference.get();
        if (compose == null) {
            return;
        }

        Bundle data = msg.getData();

        switch (msg.what) {
            case MessageCompose.MSG_PROGRESS_ON:
                compose.setProgressBarIndeterminateVisibility(true);
                break;
            case MessageCompose.MSG_PROGRESS_OFF:
                compose.setProgressBarIndeterminateVisibility(false);
                break;
            case MessageCompose.MSG_SKIPPED_ATTACHMENTS:
                Toast.makeText(
                        compose,
                        compose.getString(R.string.message_compose_attachments_skipped_toast),
                        Toast.LENGTH_LONG).show();
                break;
            case MessageCompose.MSG_SAVED_DRAFT:
                Toast.makeText(
                        compose,
                        compose.getString(R.string.message_saved_toast),
                        Toast.LENGTH_LONG).show();
                break;
            case MessageCompose.MSG_DISCARDED_DRAFT:
                Toast.makeText(
                        compose,
                        compose.getString(R.string.message_discarded_toast),
                        Toast.LENGTH_LONG).show();
                break;
            case MessageCompose.MSG_PERFORM_STALLED_ACTION:
                compose.performStalledAction();
                break;
            case MSG_SEND:
                compose.onSend();
                break;
            case MSG_INTENT_SENDER_FOR_RESULT:
                try {
                    PendingIntent pendingIntent = (PendingIntent) data.get(INTENT_KEY);
                    if(pendingIntent == null) {
                        return;
                    }
                    int requestCode = data.getInt(REQUEST_CODE_KEY);
                    compose.startIntentSenderForResult(pendingIntent.getIntentSender(),
                            requestCode, null, 0, 0, 0);
                } catch (IntentSender.SendIntentException e) {
                    Log.e(K9.LOG_TAG, "SendIntentException", e);
                }
                break;
            case MSG_OPEN_PGP_ERRORS:
                OpenPgpError error = (OpenPgpError) data.get(ERROR_KEY);
                if (error == null) {
                    return;
                }
                Log.e(K9.LOG_TAG, "OpenPGP Error ID:" + error.getErrorId());
                Log.e(K9.LOG_TAG, "OpenPGP Error Message:" + error.getMessage());

                Toast.makeText(compose,
                        compose.getString(R.string.openpgp_error, error.getMessage()),
                        Toast.LENGTH_LONG).show();
                break;
            case MSG_SMIME_ERRORS:
                String message = data.getString(SMIME_ERROR_KEY);
                Toast.makeText(compose,
                        compose.getString(R.string.openpgp_error, message),
                        Toast.LENGTH_LONG).show();
            default:
                super.handleMessage(msg);
                break;
        }
    }
}
