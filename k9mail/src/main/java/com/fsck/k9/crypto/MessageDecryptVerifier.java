package com.fsck.k9.crypto;


import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import com.fsck.k9.mail.Body;
import com.fsck.k9.mail.BodyPart;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.Multipart;
import com.fsck.k9.mail.Part;
import com.fsck.k9.mail.internet.MessageExtractor;
import org.openintents.openpgp.util.OpenPgpUtils;


public class MessageDecryptVerifier {
    private static final String MULTIPART_ENCRYPTED = "multipart/encrypted";
    private static final String MULTIPART_SIGNED = "multipart/signed";
    private static final String SMIME_ENCRYPTED = "application/pkcs7-mime";
    private static final String PROTOCOL_PARAMETER = "protocol";
    private static final String APPLICATION_PGP_ENCRYPTED = "application/pgp-encrypted";
    private static final String APPLICATION_PGP_SIGNATURE = "application/pgp-signature";
    private static final String TEXT_PLAIN = "text/plain";

    private static List<Part> findParts(final Part startPart, final String mimeType) {
        List<Part> parts = new ArrayList<Part>();
        Stack<Part> partsToCheck = new Stack<Part>();
        partsToCheck.push(startPart);

        while (!partsToCheck.isEmpty()) {
            Part part = partsToCheck.pop();
            String partMimeType = part.getMimeType();
            Body body = part.getBody();

            if (mimeType.equals(partMimeType)) {
                parts.add(part);
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

    public static List<Part> findEncryptedParts(final Part startPart) {
        return findParts(startPart, MULTIPART_ENCRYPTED);
    }

    public static List<Part> findSignedParts(final Part startPart) {
        return findParts(startPart, MULTIPART_SIGNED);
    }

    public static List<Part> findSmimeParts(final Part startPart) {
        return findParts(startPart, SMIME_ENCRYPTED);
    }

    public static List<Part> findPgpInlineParts(Part startPart) {
        List<Part> inlineParts = new ArrayList<Part>();
        Stack<Part> partsToCheck = new Stack<Part>();
        partsToCheck.push(startPart);

        while (!partsToCheck.isEmpty()) {
            Part part = partsToCheck.pop();
            String mimeType = part.getMimeType();
            Body body = part.getBody();

            if (TEXT_PLAIN.equalsIgnoreCase(mimeType)) {
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

    public static byte[] getSignatureData(Part part) throws IOException, MessagingException {

        if (MULTIPART_SIGNED.equals(part.getMimeType())) {
            Body body = part.getBody();

            if (body instanceof Multipart) {
                Multipart multi = (Multipart) body;
                BodyPart signatureBody = multi.getBodyPart(1);

                if (APPLICATION_PGP_SIGNATURE.equals(signatureBody.getMimeType())) {
                    ByteArrayOutputStream bos = new ByteArrayOutputStream();
                    signatureBody.getBody().writeTo(bos);
                    return bos.toByteArray();
                }
            }
        }

        return null;
    }

    public static boolean isPgpMimeSignedPart(Part part) {
        return MULTIPART_SIGNED.equals(part.getMimeType());
    }

    public static boolean isPgpMimeEncryptedPart(Part part) {
        //FIXME: Doesn't work right now because LocalMessage.getContentType() doesn't load headers from database
//        String contentType = part.getContentType();
//        String protocol = MimeUtility.getHeaderParameter(contentType, PROTOCOL_PARAMETER);
//        return APPLICATION_PGP_ENCRYPTED.equals(protocol);
        return MULTIPART_ENCRYPTED.equals(part.getMimeType());
    }
}
