package com.fsck.k9.ui.crypto;


import com.fsck.k9.mail.Part;

import java.io.Serializable;
import java.util.HashMap;


public class MessageCryptoAnnotations<T> implements Serializable {
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
