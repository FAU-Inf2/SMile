package com.fsck.k9.ui.crypto;

import android.app.Activity;
import android.content.Intent;
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
import com.fsck.k9.mailstore.CryptoResultAnnotation;
import com.fsck.k9.mailstore.LocalMessage;
import com.fsck.k9.mailstore.MessageHelper;
import com.fsck.k9.mailstore.SignatureResult;
import com.fsck.k9.mailstore.SignatureStatus;

import org.openintents.openpgp.util.OpenPgpApi;

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import de.fau.cs.mad.smime_api.ISMimeService;
import de.fau.cs.mad.smime_api.SMimeApi;
import de.fau.cs.mad.smime_api.SMimeServiceConnection;

public class SmimeMessageCryptoHelper extends MessageCryptoHelper {
    private volatile SMimeApi sMimeApi;
    private SMimeServiceConnection sMimeServiceConnection;
    private LocalMessage message;

    public SmimeMessageCryptoHelper(Activity activity, MessageCryptoCallback callback, String sMimeProvider, String openPgpProvider) {
        super(activity, callback, sMimeProvider, openPgpProvider);
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();

        if (this.sMimeServiceConnection != null) {
            this.sMimeServiceConnection.unbindFromService();
        }
    }

    @Override
    void findParts(LocalMessage message) {
        if (sMimeProvider == null || sMimeProvider.equals("")) {
            return;
        }

        this.message = message;

        List<Part> smimeParts = MessageDecryptVerifier.findSmimeEncryptedParts(message);
        processFoundParts(smimeParts, CryptoPartType.ENCRYPTED_SMIME, CryptoErrorType.ENCRYPTED_BUT_INCOMPLETE,
                MessageHelper.createEmptyPart());

        List<Part> smimeSignedParts = MessageDecryptVerifier.findSmimeSignedParts(message);
        processFoundParts(smimeSignedParts, CryptoPartType.SIGNED_SMIME, CryptoErrorType.SIGNED_BUT_INCOMPLETE, NO_REPLACEMENT_PART);
    }

    @Override
    boolean isBoundToCryptoProvider() {
        return sMimeApi != null || sMimeProvider == null;
    }

    @Override
    void connectToCryptoProvider(final CountDownLatch latch) {
        sMimeServiceConnection = new SMimeServiceConnection(getContext(), sMimeProvider,
                new SMimeServiceConnection.OnBound() {
                    @Override
                    public void onBound(ISMimeService service) {
                        sMimeApi = new SMimeApi(getContext(), service);
                        latch.countDown();
                        //decryptOrVerifyNextPart();
                    }

                    @Override
                    public void onError(Exception e) {
                        Log.e(K9.LOG_TAG, "Couldn't connect to SMimeService", e);
                        latch.countDown();
                    }
                });

        sMimeServiceConnection.bindToService();
    }

    @Override
    void decryptOrVerifyPart(final CryptoPart cryptoPart) {
        currentCryptoPart = cryptoPart;
        final Intent intent = new Intent();
        intent.setAction(OpenPgpApi.ACTION_DECRYPT_VERIFY);

        try {
            CryptoPartType cryptoPartType = currentCryptoPart.type;
            switch (cryptoPartType) {
                case SIGNED_SMIME: {
                    callAsyncVerifySmime();
                    return;
                }
                case ENCRYPTED_SMIME: {
                    // Note: the actual intent is set in callAsyncDecryptSmime
                    callAsyncDecryptSmime();
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

    private void callAsyncDecryptSmime() throws IOException {
        final CountDownLatch latch = new CountDownLatch(1);
        final String sender = this.message.getFrom()[0].getAddress();
        String recipient;

        try {
            recipient = this.message.getRecipients(Message.RecipientType.TO)[0].getAddress();
        } catch (MessagingException e) {
            recipient = null;
        }

        PipedInputStream pipedInputStream = getPipedInputStream();
        final PipedOutputStream decryptedOutputStream = new PipedOutputStream();
        new DecryptedSmimeDataAsyncTask(decryptedOutputStream, latch).execute();
        Intent data = SMimeApi.decryptAndVerifyMessage(sender, recipient);

        sMimeApi.executeApiAsync(data, pipedInputStream, decryptedOutputStream, new SMimeApi.ISMimeCallback() {
            @Override
            public void onReturn(Intent result) {
                currentCryptoResult = result;
                latch.countDown();
            }
        });
    }

    private void callAsyncVerifySmime() throws IOException, MessagingException {
        PipedInputStream pipedInputStream = getPipedInputStream();
        String[] fromHeader = currentCryptoPart.part.getHeader("From");
        String sender = new Address(fromHeader[0]).getAddress();
        Intent intent = SMimeApi.verifyMessage(sender);
        sMimeApi.executeApiAsync(intent, pipedInputStream, null, new SMimeApi.ISMimeCallback() {
            @Override
            public void onReturn(Intent result) {
                currentCryptoResult = result;
                onSmimeCryptoOperationReturned(null);
                Log.d(K9.LOG_TAG, "verify smime message");
            }
        });
    }

    private final void onSmimeCryptoOperationReturned(final MimeBodyPart outputPart) {
        if (currentCryptoResult == null) {
            Log.e(K9.LOG_TAG, "Internal error: we should have a result here!");
            return;
        }

        try {
            handleSmimeCryptoOperationResult(outputPart);
        } finally {
            currentCryptoResult = null;
        }
    }

    private void handleSmimeCryptoOperationResult(final MimeBodyPart outputPart) {
        int resultCode = currentCryptoResult.getIntExtra(SMimeApi.EXTRA_RESULT_CODE, INVALID_RESULT_CODE);
        switch (resultCode) {
            case INVALID_RESULT_CODE:
                Log.e(K9.LOG_TAG, "no result code!");
                break;
            case SMimeApi.RESULT_CODE_ERROR:
                handleSmimeCryptoOperationError();
                break;
            case SMimeApi.RESULT_CODE_SUCCESS:
                handleSmimeCryptoOperationSuccess(outputPart);
                break;
        }
    }

    private void handleSmimeCryptoOperationError() {
        onCryptoFailed(new CryptoError(CryptoErrorType.GENERIC_ERROR, "dummy"));
    }

    private void handleSmimeCryptoOperationSuccess(MimeBodyPart outputPart) {
        final CryptoResultAnnotation resultAnnotation = new CryptoResultAnnotation();
        resultAnnotation.setOutputData(outputPart);
        int resultType = currentCryptoResult.getIntExtra(SMimeApi.RESULT_TYPE, SMimeApi.RESULT_TYPE_UNENCRYPTED_UNSIGNED);

        if ((resultType & SMimeApi.RESULT_TYPE_ENCRYPTED) == SMimeApi.RESULT_TYPE_ENCRYPTED) {
            resultAnnotation.setEncrypted(true);
        } else {
            resultAnnotation.setEncrypted(false);
        }

        if ((resultType & SMimeApi.RESULT_TYPE_SIGNED) == SMimeApi.RESULT_TYPE_SIGNED) {
            int signatureStatus = currentCryptoResult.getIntExtra(SMimeApi.RESULT_SIGNATURE, SMimeApi.RESULT_SIGNATURE_UNSIGNED);
            SignatureResult signatureResult = new SignatureResult(SignatureStatus.SUCCESS, null, null);
            switch (signatureStatus) {
                case SMimeApi.RESULT_SIGNATURE_SIGNED:
                    signatureResult.setStatus(SignatureStatus.SUCCESS);
                    break;
                case SMimeApi.RESULT_SIGNATURE_SIGNED_UNCOFIRMED:
                    signatureResult.setStatus(SignatureStatus.SUCCESS_UNCERTIFIED);
                    break;
                default:
                    signatureResult.setStatus(SignatureStatus.INVALID_SIGNATURE);
                    break;
            }

            resultAnnotation.setSignatureResult(signatureResult);
        }

        onCryptoSuccess(resultAnnotation);
    }

    private class DecryptedSmimeDataAsyncTask extends DecryptedDataAsyncTask {
        public DecryptedSmimeDataAsyncTask(final PipedOutputStream decryptedOutputStream, final CountDownLatch latch) throws IOException {
            super(getContext(), decryptedOutputStream, latch);
        }

        @Override
        protected void onPostExecute(MimeBodyPart decryptedPart) {
            onSmimeCryptoOperationReturned(decryptedPart);
        }
    }
}
