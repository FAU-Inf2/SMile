package com.fsck.k9.crypto;

import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentSender;
import android.util.Log;

import com.fsck.k9.K9;
import com.fsck.k9.activity.MessageCompose;
import com.fsck.k9.activity.MessageComposeHandler;

import org.openintents.openpgp.OpenPgpError;
import org.openintents.openpgp.util.OpenPgpApi;

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;

/**
 * Called on successful encrypt/verify
 */
public final class OpenPgpSignEncryptCallback implements OpenPgpApi.IOpenPgpCallback {
    final ByteArrayOutputStream os;
    final int requestCode;
    final PgpData pgpData;
    final MessageComposeHandler messageComposeHandler;

    public  OpenPgpSignEncryptCallback(final ByteArrayOutputStream os, final int requestCode, PgpData pgpData, MessageComposeHandler messageComposeHandler) {
        this.os = os;
        this.requestCode = requestCode;
        this.messageComposeHandler = messageComposeHandler;
        this.pgpData = pgpData;
    }

    @Override
    public void onReturn(Intent result) {
        final int resultCode = result.getIntExtra(OpenPgpApi.RESULT_CODE, OpenPgpApi.RESULT_CODE_ERROR);
        switch (resultCode) {
            case OpenPgpApi.RESULT_CODE_SUCCESS: {
                try {
                    final String output = os.toString("UTF-8");

                    if (K9.DEBUG) {
                        Log.d(OpenPgpApi.TAG, "result: " + os.toByteArray().length +
                                " str=" + output);
                    }

                    pgpData.setEncryptedData(output);
                    messageComposeHandler.onSend();
                } catch (UnsupportedEncodingException e) {
                    Log.e(K9.LOG_TAG, "UnsupportedEncodingException", e);
                }

                break;
            }
            case OpenPgpApi.RESULT_CODE_USER_INTERACTION_REQUIRED: {
                messageComposeHandler.startIntentSenderForResult(result, requestCode);
                break;
            }
            case OpenPgpApi.RESULT_CODE_ERROR: {
                OpenPgpError error = result.getParcelableExtra(OpenPgpApi.RESULT_ERROR);
                messageComposeHandler.handleOpenPgpErrors(error);
                break;
            }
        }
    }
}
