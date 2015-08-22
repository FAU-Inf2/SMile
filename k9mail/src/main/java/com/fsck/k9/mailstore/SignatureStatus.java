package com.fsck.k9.mailstore;

public enum SignatureStatus {
    SUCCESS,
    SUCCESS_UNCERTIFIED,
    ERROR,
    KEY_MISSING,
    KEY_EXPIRED,
    KEY_REVOKED
}
