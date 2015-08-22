package com.fsck.k9.ui.crypto;


import com.fsck.k9.mail.Part;
import com.fsck.k9.mailstore.CryptoResultAnnotation;

import java.io.Serializable;
import java.util.HashMap;


public class MessageCryptoAnnotations implements Serializable {
    private HashMap<Part, CryptoResultAnnotation> annotations = new HashMap<Part, CryptoResultAnnotation>();

    MessageCryptoAnnotations() {
        // Package-private constructor
    }

    void put(Part part, CryptoResultAnnotation annotation) {
        annotations.put(part, annotation);
    }

    public CryptoResultAnnotation get(Part part) {
        return annotations.get(part);
    }

    public boolean has(Part part) {
        return annotations.containsKey(part);
    }
}
