package com.fsck.k9.ui.messageview;


import android.app.PendingIntent;

interface CryptoHeaderViewCallback {
    void onSignatureButtonClick(PendingIntent pendingIntent);
}
