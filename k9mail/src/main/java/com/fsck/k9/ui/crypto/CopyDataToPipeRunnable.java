package com.fsck.k9.ui.crypto;

import android.util.Log;

import com.fsck.k9.K9;
import com.fsck.k9.mail.Body;
import com.fsck.k9.mail.BodyPart;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.Multipart;
import com.fsck.k9.mail.Part;
import com.fsck.k9.mail.internet.MessageExtractor;

import java.io.IOException;
import java.io.PipedOutputStream;

class CopyDataToPipeRunnable implements Runnable {
    private final PipedOutputStream outputStream;
    private final CryptoPart cryptoPart;

    public CopyDataToPipeRunnable(final PipedOutputStream outputStream, final CryptoPart cryptoPart) {
        this.outputStream = outputStream;
        this.cryptoPart = cryptoPart;
    }

    @Override
    public void run() {
        try {
            Part part = cryptoPart.part;

            switch (cryptoPart.type) {
                case INLINE_PGP:{
                    String text = MessageExtractor.getTextFromPart(part);
                    outputStream.write(text.getBytes());
                    break;
                }
                case ENCRYPTED_PGP: {
                    Multipart multipartEncryptedMultipart = (Multipart) part.getBody();
                    BodyPart encryptionPayloadPart = multipartEncryptedMultipart.getBodyPart(1);
                    Body encryptionPayloadBody = encryptionPayloadPart.getBody();
                    encryptionPayloadBody.writeTo(outputStream);
                    break;
                }
                case ENCRYPTED_SMIME: {
                    part.writeTo(outputStream);
                    break;
                }
                case SIGNED_PGP: {
                    Multipart multipartSignedMultipart = (Multipart) part.getBody();
                    BodyPart signatureBodyPart = multipartSignedMultipart.getBodyPart(0);
                    Log.d(K9.LOG_TAG, "signed data type: " + signatureBodyPart.getMimeType());
                    signatureBodyPart.writeTo(outputStream);
                    break;
                }
                case SIGNED_SMIME:
                    part.writeTo(outputStream);
                    break;
                default:
                    Log.wtf(K9.LOG_TAG, "No suitable data to stream found!");
                    break;
            }
        } catch (IOException | MessagingException e) {
            Log.e(K9.LOG_TAG, "Exception while writing message to crypto provider", e);
        } finally {
            try {
                if (outputStream != null) {
                    outputStream.close();
                }
            } catch (IOException e) {
                // don't care
            }
        }
    }
}
