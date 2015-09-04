package com.fsck.k9.ui.crypto;


import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender.SendIntentException;
import android.support.annotation.NonNull;
import android.util.Log;

import com.fsck.k9.K9;
import com.fsck.k9.crypto.MessageDecryptVerifier;
import com.fsck.k9.mail.Address;
import com.fsck.k9.mail.Message;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.Part;
import com.fsck.k9.mail.internet.MimeBodyPart;
import com.fsck.k9.mailstore.CryptoError;
import com.fsck.k9.mailstore.CryptoErrorType;
import com.fsck.k9.mailstore.LocalMessage;
import com.fsck.k9.mailstore.MessageHelper;
import com.fsck.k9.mailstore.CryptoResultAnnotation;
import com.fsck.k9.mailstore.SignatureResult;
import com.fsck.k9.mailstore.SignatureStatus;

import org.openintents.openpgp.OpenPgpDecryptionResult;
import org.openintents.openpgp.OpenPgpError;
import org.openintents.openpgp.OpenPgpSignatureResult;
import org.openintents.openpgp.util.OpenPgpApi;

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import de.fau.cs.mad.smile.android.R;
import de.fau.cs.mad.smime_api.ISMimeService;
import de.fau.cs.mad.smime_api.SMimeApi;
import de.fau.cs.mad.smime_api.SMimeServiceConnection;


public abstract class MessageCryptoHelper {
    protected static final int REQUEST_CODE_CRYPTO = 1000;
    protected static final int INVALID_RESULT_CODE = -1;
    protected static final MimeBodyPart NO_REPLACEMENT_PART = null;

    private final Context context;
    private final Activity activity;
    private final MessageCryptoCallback callback;

    private final Deque<CryptoPart> partsToDecryptOrVerify = new ArrayDeque<CryptoPart>();
    protected CryptoPart currentCryptoPart;
    protected Intent currentCryptoResult;

    private MessageCryptoAnnotations messageAnnotations;
    protected final String sMimeProvider;
    protected final String openPgpProvider;

    protected MessageCryptoHelper(final Activity activity, @NonNull final MessageCryptoCallback callback,
                               final String sMimeProvider, final String openPgpProvider) {
        this.context = activity.getApplicationContext();
        this.activity = activity;
        this.callback = callback;

        this.messageAnnotations = new MessageCryptoAnnotations();
        this.sMimeProvider = sMimeProvider;
        this.openPgpProvider = openPgpProvider;
    }

    public Context getContext() {
        return context;
    }

    public Activity getActivity() {
        return activity;
    }

    public void decryptOrVerifyMessagePartsIfNecessary(final LocalMessage message) {
        if (openPgpProvider == null && sMimeProvider == null) {
            returnResultToFragment();
            return;
        }

        if(callback != null) {
            callback.setProgress(true);
        }

        findParts(message);
        decryptOrVerifyNextPart();
    }

    abstract void findParts(final LocalMessage message);

    protected void addPart(CryptoPart part) {
        partsToDecryptOrVerify.add(part);
    }

    protected void processFoundParts(final List<Part> foundParts, final CryptoPartType cryptoPartType,
                                   final CryptoErrorType errorIfIncomplete, final MimeBodyPart replacementPart) {
        for (Part part : foundParts) {
            if (MessageHelper.isCompletePartAvailable(part)) {
                CryptoPart cryptoPart = new CryptoPart(cryptoPartType, part);
                addPart(cryptoPart);
            } else {
                addErrorAnnotation(part, errorIfIncomplete, replacementPart);
            }
        }
    }

    private void addErrorAnnotation(final Part part, final CryptoErrorType error,
                                          final MimeBodyPart outputData) {
        CryptoResultAnnotation annotation = new CryptoResultAnnotation();
        annotation.setErrorType(error);
        annotation.setOutputData(outputData);
        messageAnnotations.put(part, annotation);
    }

    private void decryptOrVerifyNextPart() {
        if (partsToDecryptOrVerify.isEmpty()) {
            returnResultToFragment();
            return;
        }

        final CryptoPart cryptoPart = partsToDecryptOrVerify.peekFirst();
        startDecryptingOrVerifyingPart(cryptoPart);
    }

    private void startDecryptingOrVerifyingPart(final CryptoPart cryptoPart) {
        final CountDownLatch latch = new CountDownLatch(1);

        new Thread(new Runnable() {
            @Override
            public void run() {
                if (!isBoundToCryptoProvider()) {
                    connectToCryptoProvider(latch);
                }
            }
        }).start();

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    latch.await();

                    if (isBoundToCryptoProvider()) {
                        decryptOrVerifyPart(cryptoPart);
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    abstract boolean isBoundToCryptoProvider();

    abstract void connectToCryptoProvider(final CountDownLatch latch);

    abstract void decryptOrVerifyPart(final CryptoPart cryptoPart);

    protected PipedInputStream getPipedInputStream() throws IOException {
        PipedInputStream pipedInputStream = new PipedInputStream();
        PipedOutputStream pipedOutputStream = new PipedOutputStream(pipedInputStream);
        new Thread(new CopyDataToPipeRunnable(pipedOutputStream, currentCryptoPart)).start();
        return pipedInputStream;
    }


    public void handleCryptoResult(int requestCode, int resultCode, Intent data) {
        if (requestCode != REQUEST_CODE_CRYPTO) {
            return;
        }

        if (resultCode == Activity.RESULT_OK) {
            decryptOrVerifyNextPart();
        } else {
            onCryptoFailed(new CryptoError(CryptoErrorType.CLIENT_SIDE_ERROR, context.getString(R.string.openpgp_canceled_by_user)));
        }
    }

    protected void onCryptoSuccess(CryptoResultAnnotation resultAnnotation) {
        addCryptoResultPartToMessage(resultAnnotation);
        onCryptoFinished();
    }

    private void addCryptoResultPartToMessage(CryptoResultAnnotation resultAnnotation) {
        Part part = currentCryptoPart.part;
        messageAnnotations.put(part, resultAnnotation);
    }

    protected void onCryptoFailed(CryptoError error) {
        CryptoResultAnnotation errorPart = new CryptoResultAnnotation();
        errorPart.setError(error);
        addCryptoResultPartToMessage(errorPart);
        onCryptoFinished();
    }

    private void onCryptoFinished() {
        if (partsToDecryptOrVerify.size() > 0) {
            partsToDecryptOrVerify.removeFirst();
        }

        decryptOrVerifyNextPart();
    }

    private void returnResultToFragment() {
        if(callback != null) {
            callback.setProgress(true);
            callback.onCryptoOperationsFinished(messageAnnotations);
        }
    }
}
