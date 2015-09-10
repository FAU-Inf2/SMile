package com.fsck.k9.ui.crypto;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.fsck.k9.K9;
import com.fsck.k9.mail.internet.MimeBodyPart;
import com.fsck.k9.mailstore.DecryptStreamParser;

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.concurrent.CountDownLatch;

abstract class DecryptedDataAsyncTask extends AsyncTask<Void, Void, MimeBodyPart> {
    private Context context;
    private final PipedInputStream decryptedInputStream;
    private final CountDownLatch latch;

    public DecryptedDataAsyncTask(Context context, final PipedOutputStream decryptedOutputStream, final CountDownLatch latch) throws IOException {
        this.context = context;
        this.decryptedInputStream = new PipedInputStream(decryptedOutputStream);
        this.latch = latch;
    }

    @Override
    protected MimeBodyPart doInBackground(Void... params) {
        MimeBodyPart decryptedPart = null;
        try {
            // DecryptTempFileBody is created through this call
            decryptedPart = DecryptStreamParser.parse(context, decryptedInputStream);

            latch.await();
        } catch (InterruptedException e) {
            Log.w(K9.LOG_TAG, "we were interrupted while waiting for onReturn!", e);
        } catch (Exception e) {
            Log.e(K9.LOG_TAG, "Something went wrong while parsing the decrypted MIME part", e);
            //TODO: pass error to main thread and display error message to user
        }

        return decryptedPart;
    }
}
