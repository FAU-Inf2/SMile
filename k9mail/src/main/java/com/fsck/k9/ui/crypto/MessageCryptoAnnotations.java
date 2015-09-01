package com.fsck.k9.ui.crypto;


import com.fsck.k9.mail.Part;
import com.fsck.k9.mailstore.CryptoResultAnnotation;

import java.io.Serializable;
import java.util.HashMap;


public class MessageCryptoAnnotations implements Serializable {
    private final HashMap<Part, CryptoResultAnnotation> annotations;

    public MessageCryptoAnnotations() {
        annotations = new HashMap<>();
    }

    void put(Part part, CryptoResultAnnotation annotation) {
        annotations.put(part, annotation);
    }

    public CryptoResultAnnotation get(final Part part) {
        return annotations.get(part);
    }

    public boolean has(final Part part) {
        return annotations.containsKey(part);
    }
}
