package com.fsck.k9.mailstore;


import android.app.PendingIntent;

import com.fsck.k9.mail.internet.MimeBodyPart;


public final class CryptoResultAnnotation {

    private boolean wasEncrypted;
    private SignatureResult signatureResult;
    private CryptoError error;
    private PendingIntent pendingIntent;
    private MimeBodyPart outputData;

    public SignatureResult getSignatureResult() {
        return signatureResult;
    }

    public PendingIntent getPendingIntent() {
        return pendingIntent;
    }

    public void setSignatureResult(SignatureResult signatureResult) {
        this.signatureResult = signatureResult;
    }

    public void setPendingIntent(PendingIntent pendingIntent) {
        this.pendingIntent = pendingIntent;
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

    public boolean wasEncrypted() {
        return wasEncrypted;
    }

    public void setWasEncrypted(boolean wasEncrypted) {
        this.wasEncrypted = wasEncrypted;
    }
}
