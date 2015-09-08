package com.fsck.k9.crypto;


import android.support.annotation.NonNull;

import com.fsck.k9.mail.Body;
import com.fsck.k9.mail.BodyPart;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.Multipart;
import com.fsck.k9.mail.Part;
import com.fsck.k9.mail.internet.MessageExtractor;
import com.fsck.k9.mail.internet.MimeUtility;
import com.fsck.k9.mailstore.LocalMessage;

import org.openintents.openpgp.util.OpenPgpUtils;

import static com.fsck.k9.mail.internet.MimeUtility.isSameMimeType;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;


public class MessageDecryptVerifier {
    public static final String MULTIPART_ENCRYPTED = "multipart/encrypted";
    public static final String MULTIPART_SIGNED = "multipart/signed";
    public static final String SMIME_ENCRYPTED = "application/pkcs7-mime";
    public static final String PROTOCOL_PARAMETER = "protocol";
    public static final String APPLICATION_PGP_ENCRYPTED = "application/pgp-encrypted";
    public static final String APPLICATION_PGP_SIGNATURE = "application/pgp-signature";
    public static final String APPLICATION_SMIME_SIGNATURE = "application/pkcs7-signature";
    public static final String TEXT_PLAIN = "text/plain";

    @NonNull
    private static List<Part> findParts(final Part startPart, final String mimeType, final String protocol) {
        List<Part> parts = new ArrayList<>();
        Stack<Part> partsToCheck = new Stack<>();
        partsToCheck.push(startPart);

        while (!partsToCheck.isEmpty()) {
            Part part = partsToCheck.pop();
            String partMimeType = part.getMimeType();
            Body body = part.getBody();

            if (isSameMimeType(partMimeType, mimeType)) {
                if(checkProtocolParameter(part, protocol)) {
                    parts.add(part);
                }
            } else if (body instanceof Multipart) {
                Multipart multipart = (Multipart) body;
                for (int i = multipart.getCount() - 1; i >= 0; i--) {
                    BodyPart bodyPart = multipart.getBodyPart(i);
                    partsToCheck.push(bodyPart);
                }
            }
        }

        return parts;
    }

    private static boolean checkProtocolParameter(final Part part, final String protocol) {
        if(protocol != null) {
            String partProtocol = null;
            String[] contentTypes = new String[0];
            try {
                contentTypes = part.getHeader("Content-Type");
            } catch (MessagingException e) {
                e.printStackTrace();
            }

            for (String contentType : contentTypes) {
                partProtocol = MimeUtility.getHeaderParameter(contentType, PROTOCOL_PARAMETER);
                if(partProtocol != null && partProtocol.equalsIgnoreCase(protocol)) {
                    return true;
                }
            }

            return false;
        }

        return true;
    }

    public static List<Part> findPgpEncryptedParts(final Part startPart) {
        return findParts(startPart, MULTIPART_ENCRYPTED, null);
    }

    public static List<Part> findPgpSignedParts(final Part startPart) {
        return findParts(startPart, MULTIPART_SIGNED, APPLICATION_PGP_SIGNATURE);
    }

    public static List<Part> findPgpInlineParts(Part startPart) {
        List<Part> inlineParts = new ArrayList<Part>();
        Stack<Part> partsToCheck = new Stack<Part>();
        partsToCheck.push(startPart);

        while (!partsToCheck.isEmpty()) {
            Part part = partsToCheck.pop();
            String mimeType = part.getMimeType();
            Body body = part.getBody();

            if (isSameMimeType(mimeType, TEXT_PLAIN)) {
                String text = MessageExtractor.getTextFromPart(part);
                switch (OpenPgpUtils.parseMessage(text)) {
                    case OpenPgpUtils.PARSE_RESULT_MESSAGE:
                    case OpenPgpUtils.PARSE_RESULT_SIGNED_MESSAGE:
                        inlineParts.add(part);
                }
            } else if (body instanceof Multipart) {
                Multipart multipart = (Multipart) body;
                for (int i = multipart.getCount() - 1; i >= 0; i--) {
                    BodyPart bodyPart = multipart.getBodyPart(i);
                    partsToCheck.push(bodyPart);
                }
            }
        }

        return inlineParts;
    }

    public static List<Part> findSmimeSignedParts(final Part startPart) {
        return findParts(startPart, MULTIPART_SIGNED, APPLICATION_SMIME_SIGNATURE);
    }

    public static List<Part> findSmimeEncryptedParts(final Part startPart) {
        return findParts(startPart, SMIME_ENCRYPTED, null);
    }

    public static byte[] getSignatureData(Part part) throws IOException, MessagingException {
        if (isSameMimeType(part.getMimeType(), MULTIPART_SIGNED)) {
            Body body = part.getBody();

            if (body instanceof Multipart) {
                Multipart multi = (Multipart) body;
                BodyPart signatureBody = multi.getBodyPart(1);
                if (isSameMimeType(signatureBody.getMimeType(), APPLICATION_PGP_SIGNATURE)) {
                    ByteArrayOutputStream bos = new ByteArrayOutputStream();
                    signatureBody.getBody().writeTo(bos);
                    return bos.toByteArray();
                }
            }
        }

        return null;
    }

    public static boolean isMimeSignedPart(Part part) {
        return isSameMimeType(part.getMimeType(), MULTIPART_SIGNED);
    }

    public static boolean isPgpMimeEncryptedPart(Part part) {
        return isSameMimeType(part.getMimeType(), MULTIPART_ENCRYPTED);
    }

    public static boolean isSmimePart(Part part) {
        return findSmimeEncryptedParts(part).size() > 0 || findSmimeSignedParts(part).size() > 0;
    }

    public static boolean isSmimeEncryptedPart(Part part) {
        return isSameMimeType(part.getMimeType(), SMIME_ENCRYPTED);
    }

    public static boolean isEncryptedPart(Part part) {
        return isSmimeEncryptedPart(part) || isPgpMimeEncryptedPart(part);
    }
}
