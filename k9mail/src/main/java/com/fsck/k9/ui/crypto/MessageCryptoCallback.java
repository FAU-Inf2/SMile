package com.fsck.k9.ui.crypto;


import com.fsck.k9.mailstore.OpenPgpResultAnnotation;

public interface MessageCryptoCallback {
    void onCryptoOperationsFinished(MessageCryptoAnnotations<OpenPgpResultAnnotation> annotations);
}
