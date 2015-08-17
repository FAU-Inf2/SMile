package com.fsck.k9.ui.messageview;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import com.fsck.k9.K9;
import com.fsck.k9.helper.FileHelper;
import com.fsck.k9.helper.MediaScannerNotifier;
import com.fsck.k9.mailstore.AttachmentViewInfo;

import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import de.fau.cs.mad.smile.android.R;

final class SaveAttachmentAsyncTask extends AsyncTask<File, Void, File> {
    private final Context context;
    private final AttachmentViewInfo attachment;
    private final MessageViewFragment.MessageViewHandler messageViewHandler;

    public SaveAttachmentAsyncTask(final Context context, final AttachmentViewInfo attachment, final MessageViewFragment.MessageViewHandler handler) {
        this.messageViewHandler = handler;
        this.context = context;
        this.attachment = attachment;
    }

    @Override
    protected void onPreExecute() {
        messageViewHandler.disableAttachmentButtons(attachment);
    }

    @Override
    protected File doInBackground(File... params) {
        try {
            File directory = params[0];
            return saveAttachmentWithUniqueFileName(directory);
        } catch (IOException e) {
            if (K9.DEBUG) {
                Log.e(K9.LOG_TAG, "Error saving attachment", e);
            }
            return null;
        }
    }

    @Override
    protected void onPostExecute(File file) {
        messageViewHandler.enableAttachmentButtons(attachment);
        if (file != null) {
            displayAttachmentSavedMessage(file.toString());
            MediaScannerNotifier.notify(context, file);
        } else {
            displayAttachmentNotSavedMessage();
        }
    }

    private final File saveAttachmentWithUniqueFileName(final File directory) throws IOException {
        final String filename = FileHelper.sanitizeFilename(attachment.displayName);
        final File file = FileHelper.createUniqueFile(directory, filename);

        writeAttachmentToStorage(file);

        return file;
    }

    private final void writeAttachmentToStorage(final File file) throws IOException {
        final InputStream in = context.getContentResolver().openInputStream(attachment.uri);
        try {
            OutputStream out = new FileOutputStream(file);
            try {
                IOUtils.copy(in, out);
                out.flush();
            } finally {
                out.close();
            }
        } finally {
            in.close();
        }
    }

    private final void displayAttachmentSavedMessage(final String filename) {
        final String message = context.getString(R.string.message_view_status_attachment_saved, filename);
        displayMessageToUser(message);
    }

    private final void displayAttachmentNotSavedMessage() {
        final String message = context.getString(R.string.message_view_status_attachment_not_saved);
        displayMessageToUser(message);
    }

    private final void displayMessageToUser(final String message) {
        // TODO: make sure it runs on ui thread
        Toast.makeText(context, message, Toast.LENGTH_LONG).show();
    }
}
