package com.fsck.k9.crypto;

import android.content.Intent;
import android.util.Log;

import com.fsck.k9.Identity;
import com.fsck.k9.K9;
import com.fsck.k9.activity.MessageComposeHandler;
import com.fsck.k9.mail.Address;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.internet.MimeMessage;

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import de.fau.cs.mad.smime_api.SMimeApi;

/**
 * Smime logic for message compose
 */
public class SmimeMessageCompose {

    private MimeMessage currentMessage;
    private boolean signChecked;
    private boolean encryptChecked;
    private List<Address> addresses;
    private Identity sender;
    private SMimeApi sMimeApi;
    private MessageComposeHandler messageComposeHandler;

    public SmimeMessageCompose(boolean singChecked, boolean encryptChecked, List<Address> addresses, Identity sender, SMimeApi sMimeApi, MessageComposeHandler messageComposeHandler) {
        this.signChecked = singChecked;
        this.encryptChecked = encryptChecked;
        this.addresses = addresses;
        this.sender = sender;
        this.sMimeApi = sMimeApi;
        this.messageComposeHandler = messageComposeHandler;
    }

    public void handleSmime() {
        if (signChecked && encryptChecked) {
            handleSmimeSignAndEncrypt();
            return;
        }

        if (signChecked) {
            handleSmimeSign();
            return;
        }

        if (encryptChecked) {
            handleSmimeEncrypt();
        }
    }

    private void handleSmimeEncrypt() {
        List<String> recipients = new ArrayList<>();

        for (Address recipient : addresses) {
            recipients.add(recipient.getAddress());
        }

        final Intent intent = SMimeApi.encryptMessage(recipients);
        executeSmimeMethod(intent);
    }

    private void handleSmimeSignAndEncrypt() {
        List<String> recipients = new ArrayList<>();

        for (Address recipient : addresses) {
            recipients.add(recipient.getAddress());
        }

        final Intent intent = SMimeApi.signAndEncryptMessage(sender.getEmail(), recipients);
        executeSmimeMethod(intent);
    }

    private void handleSmimeSign() {
        final Intent intent = SMimeApi.signMessage(sender.getEmail());
        executeSmimeMethod(intent);
    }

    private PipedInputStream getSmimeInputStream() {
        final PipedInputStream pipedInputStream = new PipedInputStream();
        PipedOutputStream outPipe = null;

        try {
            outPipe = new PipedOutputStream(pipedInputStream);
        } catch (IOException e) {
            Log.e(K9.LOG_TAG, "Failed to create pipe: ", e);
            return null;
        }

        final PipedOutputStream out = outPipe;

        // TODO: async task/runnable class?
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    currentMessage = messageComposeHandler.createMimeMessage();
                    if(currentMessage != null) {
                        currentMessage.writeTo(out); // TODO: only send body part
                    }
                } catch (Exception e) {
                    Log.e(K9.LOG_TAG, "Failed to write: ", e);
                }
            }
        }).start();

        return pipedInputStream;
    }

    private void executeSmimeMethod(final Intent intent) {
        final PipedInputStream pipedInputStream = getSmimeInputStream();
        final PipedOutputStream pipedOutputStream = new PipedOutputStream();
        final CountDownLatch latch = new CountDownLatch(1);
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    PipedInputStream inputStream = new PipedInputStream(pipedOutputStream);
                    MimeMessage resultMessage = new MimeMessage(inputStream, true);
                    latch.await();
                    if(currentMessage != null) {
                        currentMessage = resultMessage;
                        messageComposeHandler.sendSmime(currentMessage);
                    }
                } catch (IOException | MessagingException | InterruptedException e) {
                    Log.e(K9.LOG_TAG, "error retrieving processed message from smime service", e);
                }
            }
        }).start();

        if(sMimeApi != null) {
            sMimeApi.executeApiAsync(intent, pipedInputStream, pipedOutputStream, new SmimeSignEncryptCallback(latch, currentMessage));
        } else {
            messageComposeHandler.smimeError("SMIME-API was null");
        }
    }
}
