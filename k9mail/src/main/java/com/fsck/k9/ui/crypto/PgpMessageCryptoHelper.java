package com.fsck.k9.ui.crypto;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.util.Log;

import com.fsck.k9.K9;
import com.fsck.k9.crypto.MessageDecryptVerifier;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.Part;
import com.fsck.k9.mail.internet.MimeBodyPart;
import com.fsck.k9.mail.internet.TextBody;
import com.fsck.k9.mailstore.CryptoError;
import com.fsck.k9.mailstore.CryptoErrorType;
import com.fsck.k9.mailstore.CryptoResultAnnotation;
import com.fsck.k9.mailstore.LocalMessage;
import com.fsck.k9.mailstore.MessageHelper;
import com.fsck.k9.mailstore.SignatureResult;
import com.fsck.k9.mailstore.SignatureStatus;

import org.openintents.openpgp.IOpenPgpService;
import org.openintents.openpgp.OpenPgpDecryptionResult;
import org.openintents.openpgp.OpenPgpError;
import org.openintents.openpgp.OpenPgpSignatureResult;
import org.openintents.openpgp.util.OpenPgpApi;
import org.openintents.openpgp.util.OpenPgpServiceConnection;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.List;
import java.util.concurrent.CountDownLatch;

public class PgpMessageCryptoHelper extends MessageCryptoHelper {
    private OpenPgpServiceConnection openPgpServiceConnection;
    private volatile OpenPgpApi openPgpApi;

    public PgpMessageCryptoHelper(Activity activity, MessageCryptoCallback callback, String sMimeProvider, String openPgpProvider) {
        super(activity, callback, sMimeProvider, openPgpProvider);
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();

        if (this.openPgpServiceConnection != null) {
            this.openPgpServiceConnection.unbindFromService();
        }
    }

    @Override
    void findParts(final LocalMessage message) {
        if(openPgpProvider == null || openPgpProvider.equals("")) {
            return;
        }

        List<Part> encryptedParts = MessageDecryptVerifier.findPgpEncryptedParts(message);
        processFoundParts(encryptedParts, CryptoPartType.ENCRYPTED_PGP, CryptoErrorType.ENCRYPTED_BUT_INCOMPLETE,
                MessageHelper.createEmptyPart());

        List<Part> pgpSignedParts = MessageDecryptVerifier.findPgpSignedParts(message);
        processFoundParts(pgpSignedParts, CryptoPartType.SIGNED_PGP, CryptoErrorType.SIGNED_BUT_INCOMPLETE, NO_REPLACEMENT_PART);

        List<Part> inlineParts = MessageDecryptVerifier.findPgpInlineParts(message);
        addFoundInlinePgpParts(inlineParts);
    }

    private void addFoundInlinePgpParts(final List<Part> foundParts) {
        for (Part part : foundParts) {
            final CryptoPart cryptoPart = new CryptoPart(CryptoPartType.INLINE_PGP, part);
            addPart(cryptoPart);
        }
    }

    @Override
    boolean isBoundToCryptoProvider() {
        return openPgpApi != null || openPgpProvider == null;
    }

    @Override
    void connectToCryptoProvider(final CountDownLatch latch) {
        openPgpServiceConnection = new OpenPgpServiceConnection(getContext(), openPgpProvider,
                new OpenPgpServiceConnection.OnBound() {
                    @Override
                    public void onBound(IOpenPgpService service) {
                        openPgpApi = new OpenPgpApi(getContext(), service);
                        latch.countDown();
                        //decryptOrVerifyNextPart();
                    }

                    @Override
                    public void onError(Exception e) {
                        Log.e(K9.LOG_TAG, "Couldn't connect to OpenPgpService", e);
                        latch.countDown();
                    }
                });
        openPgpServiceConnection.bindToService();
    }

    @Override
    protected void decryptOrVerifyPart(CryptoPart cryptoPart) {
        final Intent intent = new Intent();
        intent.setAction(OpenPgpApi.ACTION_DECRYPT_VERIFY);

        try {
            CryptoPartType cryptoPartType = cryptoPart.type;
            switch (cryptoPartType) {
                case SIGNED_PGP: {
                    callAsyncDetachedVerify(intent);
                    return;
                }
                case ENCRYPTED_PGP: {
                    callAsyncDecrypt(intent);
                    return;
                }
                case INLINE_PGP: {
                    callAsyncInlineOperation(intent);
                    return;
                }

            }

            throw new IllegalStateException("Unknown crypto part type: " + cryptoPartType);
        } catch (IOException e) {
            Log.e(K9.LOG_TAG, "IOException", e);
        } catch (MessagingException e) {
            Log.e(K9.LOG_TAG, "MessagingException", e);
        }
    }

    private void callAsyncInlineOperation(final Intent intent) throws IOException {
        PipedInputStream pipedInputStream = getPipedInputStream();
        final ByteArrayOutputStream decryptedOutputStream = new ByteArrayOutputStream();

        openPgpApi.executeApiAsync(intent, pipedInputStream, decryptedOutputStream, new OpenPgpApi.IOpenPgpCallback() {
            @Override
            public void onReturn(Intent result) {
                currentCryptoResult = result;

                MimeBodyPart decryptedPart = null;
                try {
                    TextBody body = new TextBody(new String(decryptedOutputStream.toByteArray()));
                    decryptedPart = new MimeBodyPart(body, "text/plain");
                } catch (MessagingException e) {
                    Log.e(K9.LOG_TAG, "MessagingException", e);
                }

                onOpenPgpCryptoOperationReturned(decryptedPart);
            }
        });
    }

    private void callAsyncDecrypt(final Intent intent) throws IOException {
        final CountDownLatch latch = new CountDownLatch(1);

        PipedInputStream pipedInputStream = getPipedInputStream();
        final PipedOutputStream decryptedOutputStream = new PipedOutputStream();
        new DecryptedPgpDataAsyncTask(getContext(), decryptedOutputStream, latch).execute();

        openPgpApi.executeApiAsync(intent, pipedInputStream, decryptedOutputStream, new OpenPgpApi.IOpenPgpCallback() {
            @Override
            public void onReturn(Intent result) {
                currentCryptoResult = result;
                latch.countDown();
            }
        });
    }

    private void callAsyncDetachedVerify(Intent intent) throws IOException, MessagingException {
        PipedInputStream pipedInputStream = getPipedInputStream();

        byte[] signatureData = MessageDecryptVerifier.getSignatureData(currentCryptoPart.part);
        intent.putExtra(OpenPgpApi.EXTRA_DETACHED_SIGNATURE, signatureData);

        openPgpApi.executeApiAsync(intent, pipedInputStream, null, new OpenPgpApi.IOpenPgpCallback() {
            @Override
            public void onReturn(Intent result) {
                currentCryptoResult = result;
                onOpenPgpCryptoOperationReturned(null);
            }
        });
    }

    private final void onOpenPgpCryptoOperationReturned(final MimeBodyPart outputPart) {
        if (currentCryptoResult == null) {
            Log.e(K9.LOG_TAG, "Internal error: we should have a result here!");
            return;
        }

        try {
            handleOpenPgpCryptoOperationResult(outputPart);
        } finally {
            currentCryptoResult = null;
        }
    }

    private void handleOpenPgpCryptoOperationResult(final MimeBodyPart outputPart) {
        int resultCode = currentCryptoResult.getIntExtra(OpenPgpApi.RESULT_CODE, INVALID_RESULT_CODE);
        if (K9.DEBUG) {
            Log.d(K9.LOG_TAG, "OpenPGP API decryptVerify result code: " + resultCode);
        }

        switch (resultCode) {
            case INVALID_RESULT_CODE: {
                Log.e(K9.LOG_TAG, "Internal error: no result code!");
                break;
            }
            case OpenPgpApi.RESULT_CODE_USER_INTERACTION_REQUIRED: {
                handleUserInteractionRequest();
                break;
            }
            case OpenPgpApi.RESULT_CODE_ERROR: {
                handleOpenPgpCryptoOperationError();
                break;
            }
            case OpenPgpApi.RESULT_CODE_SUCCESS: {
                handleOpenPgpCryptoOperationSuccess(outputPart);
                break;
            }
        }
    }

    private void handleUserInteractionRequest() {
        PendingIntent pendingIntent = currentCryptoResult.getParcelableExtra(OpenPgpApi.RESULT_INTENT);
        if (pendingIntent == null) {
            throw new AssertionError("Expecting PendingIntent on USER_INTERACTION_REQUIRED!");
        }

        try {
            getActivity().startIntentSenderForResult(pendingIntent.getIntentSender(), REQUEST_CODE_CRYPTO, null, 0, 0, 0);
        } catch (IntentSender.SendIntentException e) {
            Log.e(K9.LOG_TAG, "Internal error on starting pendingintent!", e);
        }
    }

    private void handleOpenPgpCryptoOperationError() {
        OpenPgpError error = currentCryptoResult.getParcelableExtra(OpenPgpApi.RESULT_ERROR);
        if (K9.DEBUG) {
            Log.w(K9.LOG_TAG, "OpenPGP API error: " + error.getMessage());
        }

        CryptoError cryptoError = new CryptoError();
        cryptoError.setMessage(error.getMessage());

        switch (error.getErrorId()) {
            case OpenPgpError.CLIENT_SIDE_ERROR:
                cryptoError.setErrorType(CryptoErrorType.CLIENT_SIDE_ERROR);
                break;
            case OpenPgpError.GENERIC_ERROR:
                cryptoError.setErrorType(CryptoErrorType.GENERIC_ERROR);
                break;
            case OpenPgpError.INCOMPATIBLE_API_VERSIONS:
                cryptoError.setErrorType(CryptoErrorType.API_VERSION_MISMATCH);
                break;
            case OpenPgpError.NO_OR_WRONG_PASSPHRASE:
                cryptoError.setErrorType(CryptoErrorType.NO_OR_WRONG_PASSPHRASE);
                break;
            case OpenPgpError.NO_USER_IDS:
                cryptoError.setErrorType(CryptoErrorType.NO_USER_ID);
                break;
        }

        onCryptoFailed(cryptoError);
    }

    private void handleOpenPgpCryptoOperationSuccess(final MimeBodyPart outputPart) {
        final CryptoResultAnnotation resultAnnotation = new CryptoResultAnnotation();
        OpenPgpDecryptionResult decryptionResult =
                currentCryptoResult.getParcelableExtra(OpenPgpApi.RESULT_DECRYPTION);
        OpenPgpSignatureResult signatureResult =
                currentCryptoResult.getParcelableExtra(OpenPgpApi.RESULT_SIGNATURE);

        SignatureStatus signatureStatus = SignatureStatus.ERROR;

        switch (signatureResult.getResult()) {
            case OpenPgpSignatureResult.RESULT_INVALID_SIGNATURE:
                signatureStatus = SignatureStatus.INVALID_SIGNATURE;
                break;
            case OpenPgpSignatureResult.RESULT_VALID_CONFIRMED:
                signatureStatus = SignatureStatus.SUCCESS;
                break;
            case OpenPgpSignatureResult.RESULT_KEY_MISSING:
                signatureStatus = SignatureStatus.KEY_MISSING;
                break;
            case OpenPgpSignatureResult.RESULT_VALID_UNCONFIRMED:
                signatureStatus = SignatureStatus.SUCCESS_UNCERTIFIED;
                break;
            case OpenPgpSignatureResult.RESULT_INVALID_KEY_REVOKED:
                signatureStatus = SignatureStatus.KEY_REVOKED;
                break;
            case OpenPgpSignatureResult.RESULT_INVALID_KEY_EXPIRED:
                signatureStatus = SignatureStatus.KEY_EXPIRED;
                break;
            case OpenPgpSignatureResult.RESULT_NO_SIGNATURE:
                signatureStatus = SignatureStatus.UNSIGNED;
                break;
            case OpenPgpSignatureResult.RESULT_INVALID_INSECURE:
                signatureStatus = SignatureStatus.ERROR;
                break;
        }

        switch (decryptionResult.getResult()) {
            case OpenPgpDecryptionResult.RESULT_NOT_ENCRYPTED: {
                resultAnnotation.setEncrypted(false);
                break;
            }
            case OpenPgpDecryptionResult.RESULT_ENCRYPTED: {
                resultAnnotation.setEncrypted(true);
                break;
            }
            case OpenPgpDecryptionResult.RESULT_INSECURE: {
                resultAnnotation.setEncrypted(false);
                break;
            }
            default:
                throw new RuntimeException("OpenPgpDecryptionResult result not handled!");
        }

        SignatureResult signatureRes = new SignatureResult(signatureStatus, signatureResult.getPrimaryUserId(), signatureResult.getUserIds());
        PendingIntent pendingIntent = currentCryptoResult.getParcelableExtra(OpenPgpApi.RESULT_INTENT);
        resultAnnotation.setPendingIntent(pendingIntent);
        resultAnnotation.setSignatureResult(signatureRes);
        resultAnnotation.setOutputData(outputPart);

        onCryptoSuccess(resultAnnotation);
    }

    private class DecryptedPgpDataAsyncTask extends DecryptedDataAsyncTask {
        public DecryptedPgpDataAsyncTask(Context context, PipedOutputStream decryptedOutputStream, CountDownLatch latch) throws IOException {
            super(context, decryptedOutputStream, latch);
        }

        @Override
        protected void onPostExecute(MimeBodyPart decryptedPart) {
            onOpenPgpCryptoOperationReturned(decryptedPart);
        }
    }
}
