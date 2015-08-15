package com.fsck.k9.ui.crypto;


import java.util.HashMap;

import com.fsck.k9.mail.Part;
import com.fsck.k9.mailstore.OpenPgpResultAnnotation;


public class MessageCryptoAnnotations<T> {
    private HashMap<Part, T> annotations = new HashMap<Part, T>();

    MessageCryptoAnnotations() {
        // Package-private constructor
    }

    void put(Part part, T annotation) {
        annotations.put(part, annotation);
    }

    public T get(Part part) {
        return annotations.get(part);
    }

    public boolean has(Part part) {
        return annotations.containsKey(part);
    }
}
