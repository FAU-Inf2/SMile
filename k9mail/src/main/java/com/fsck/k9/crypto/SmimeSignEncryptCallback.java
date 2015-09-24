package com.fsck.k9.crypto;

import android.content.Intent;

import com.fsck.k9.K9;
import com.fsck.k9.mail.internet.MimeMessage;

import java.util.concurrent.CountDownLatch;

import de.fau.cs.mad.smime_api.SMimeApi;

public class SmimeSignEncryptCallback implements SMimeApi.ISMimeCallback {
    private final CountDownLatch latch;
    private MimeMessage currentMessage;

    public SmimeSignEncryptCallback(CountDownLatch latch, MimeMessage currentMessage) {
        this.latch = latch;
        this.currentMessage = currentMessage;
    }

    @Override
    public void onReturn(Intent result) {
        K9.logDebug("SMime api returned: " + result);
        final int resultCode = result.getIntExtra(SMimeApi.EXTRA_RESULT_CODE, SMimeApi.RESULT_CODE_ERROR);
        switch (resultCode) {
            case SMimeApi.RESULT_CODE_SUCCESS:
                latch.countDown();
                K9.logDebug("crypto operation success");
                break;
            case SMimeApi.RESULT_CODE_ERROR:
                currentMessage = null;
                latch.countDown();
                K9.logDebug("crypto operation fail");
                break;
        }
    }
}
