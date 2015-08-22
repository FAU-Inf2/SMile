package com.fsck.k9.mailstore;

public class CryptoError {
    private CryptoErrorType errorType;
    private String message;

    public CryptoError(CryptoErrorType errorType, String errorMessage) {
        setErrorType(errorType);
        setMessage(errorMessage);
    }

    public CryptoError() {
        this(CryptoErrorType.NONE, null);
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

    public void setMessage(String message) {
        this.message = message;
    }
}
