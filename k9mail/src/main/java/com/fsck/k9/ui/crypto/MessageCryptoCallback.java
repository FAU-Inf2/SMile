package com.fsck.k9.ui.crypto;

import android.app.PendingIntent;

public interface MessageCryptoCallback {
    void onCryptoOperationsFinished(MessageCryptoAnnotations annotations);
}
