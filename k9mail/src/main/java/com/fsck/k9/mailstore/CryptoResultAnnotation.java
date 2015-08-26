package com.fsck.k9.mailstore;


import android.app.PendingIntent;

import com.fsck.k9.mail.internet.MimeBodyPart;

import org.openintents.openpgp.OpenPgpDecryptionResult;


public final class CryptoResultAnnotation {
    private SignatureResult signatureResult;
    private OpenPgpDecryptionResult decryptionResult;
    private CryptoError error;
    private PendingIntent pendingIntent;
    private MimeBodyPart outputData;

    public CryptoResultAnnotation() {
        this.error = new CryptoError();
    }

    public void setDecryptionResult(OpenPgpDecryptionResult decryptionResult) {
        this.decryptionResult = decryptionResult;
    }

    public OpenPgpDecryptionResult getDecryptionResult() {
        return decryptionResult;
    }

    public void setSignatureResult(SignatureResult signatureResult) {
        this.signatureResult = signatureResult;
    }

    public SignatureResult getSignatureResult() {
        return signatureResult;
    }

    public void setPendingIntent(PendingIntent pendingIntent) {
        this.pendingIntent = pendingIntent;
    }

    public PendingIntent getPendingIntent() {
        return pendingIntent;
    }

    public CryptoError getError() {
        return error;
    }

    public void setError(CryptoError error) {
        this.error = error;
        setErrorType(CryptoErrorType.CRYPTO_API_RETURNED_ERROR);
    }

    public CryptoErrorType getErrorType() {
        return error.getErrorType();
    }

    public void setErrorType(CryptoErrorType errorType) {
        this.error.setErrorType(errorType);
    }

    public boolean hasOutputData() {
        return outputData != null;
    }

    public void setOutputData(MimeBodyPart outputData) {
        this.outputData = outputData;
    }

    public MimeBodyPart getOutputData() {
        return outputData;
    }
}
