package com.fsck.k9.ui.crypto;

import com.fsck.k9.mail.Part;

class CryptoPart {
    public final CryptoPartType type;
    public final Part part;

    CryptoPart(CryptoPartType type, Part part) {
        this.type = type;
        this.part = part;
    }
}
