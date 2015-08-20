package com.fsck.k9.mailstore;


import android.app.PendingIntent;

import com.fsck.k9.mail.internet.MimeBodyPart;

import java.util.List;


public final class CryptoResultAnnotation {
    public enum CryptoErrorType {
        NONE,
        CRYPTO_API_RETURNED_ERROR,
        SIGNED_BUT_INCOMPLETE,
        ENCRYPTED_BUT_INCOMPLETE
    }

    public enum SignatureStatus {
        SUCCESS,
        SUCCESS_UNCERTIFIED,
        ERROR,
        KEY_MISSING,
        KEY_EXPIRED,
        KEY_REVOKED
    }

    public static class SignatureResult {
        private SignatureStatus status;
        private String primaryUserId;
        private List<String> userIds;

        public SignatureResult(final SignatureStatus status, final String primaryUserId, final List<String> userIds) {
            this.status = status;
            this.primaryUserId = primaryUserId;
            this.userIds = userIds;
        }

        public String getPrimaryUserId() {
            return primaryUserId;
        }

        public SignatureStatus getStatus() {
            return status;
        }
    }

    public static class CryptoError {
        private CryptoErrorType errorType;
        private String message;

        CryptoError() {
            errorType = CryptoErrorType.NONE;
            message = null;
        }

        public CryptoErrorType getErrorType() {
            return errorType;
        }

        public void setErrorType(CryptoErrorType errorType) {
            this.errorType = errorType;
        }

        public String getMessage() {
            return message;
        }
    }

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
