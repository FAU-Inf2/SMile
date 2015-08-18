package com.fsck.k9.ui.messageview;


import android.content.Context;
import android.os.AsyncTask;
import android.os.Environment;
import android.widget.Toast;

import com.fsck.k9.Account;
import com.fsck.k9.K9;
import com.fsck.k9.Preferences;
import com.fsck.k9.controller.MessagingController;
import com.fsck.k9.controller.MessagingListener;
import com.fsck.k9.mail.Message;
import com.fsck.k9.mail.Part;
import com.fsck.k9.mailstore.AttachmentViewInfo;
import com.fsck.k9.mailstore.LocalMessage;
import com.fsck.k9.mailstore.LocalPart;

import java.io.File;

import de.fau.cs.mad.smile.android.R;


public final class AttachmentController {
    private final Context context;
    private final MessagingController controller;
    private final AttachmentViewInfo attachment;
    private final MessageViewHandler handler;

    public AttachmentController(final Context context,
                                final MessagingController controller,
                                final AttachmentViewInfo attachment,
                                final MessageViewHandler handler) {
        this.context = context;
        this.controller = controller;
        this.attachment = attachment;
        this.handler = handler;
    }

    public final void viewAttachment() {
        if (needsDownloading()) {
            downloadAndViewAttachment((LocalPart) attachment.part);
        } else {
            viewLocalAttachment();
        }
    }

    public final void saveAttachment() {
        saveAttachmentTo(K9.getAttachmentDefaultPath());
    }

    public final void saveAttachmentTo(final String directory) {
        saveAttachmentTo(new File(directory));
    }

    private final boolean needsDownloading() {
        return isPartMissing() && isLocalPart();
    }

    private final boolean isPartMissing() {
        return attachment.part.getBody() == null;
    }

    private final boolean isLocalPart() {
        // FIXME: fugly?
        return attachment.part instanceof LocalPart;
    }

    private final void downloadAndViewAttachment(final LocalPart localPart) {
        downloadAttachment(localPart, new Runnable() {
            @Override
            public void run() {
                viewLocalAttachment();
            }
        });
    }

    private final void downloadAndSaveAttachmentTo(final LocalPart localPart, final File directory) {
        downloadAttachment(localPart, new Runnable() {
            @Override
            public void run() {
                handler.refreshAttachmentThumbnail(attachment);
                saveAttachmentTo(directory);
            }
        });
    }

    private void downloadAttachment(final LocalPart localPart, final Runnable attachmentDownloadedCallback) {
        final String accountUuid = localPart.getAccountUuid();
        final Account account = Preferences.getPreferences(context).getAccount(accountUuid);
        final LocalMessage message = localPart.getMessage();

        handler.showAttachmentLoadingDialog();
        controller.loadAttachment(account, message, attachment.part, new MessagingListener() {
            @Override
            public void loadAttachmentFinished(final Account account, final Message message, final Part part) {
                handler.hideAttachmentLoadingDialogOnMainThread();
                handler.post(attachmentDownloadedCallback);
            }

            @Override
            public void loadAttachmentFailed(final Account account, final Message message, final Part part, final String reason) {
                handler.hideAttachmentLoadingDialogOnMainThread();
            }
        });
    }

    private final void viewLocalAttachment() {
        new ViewAttachmentAsyncTask(context, handler, attachment).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private final void saveAttachmentTo(final File directory) {
        final boolean isExternalStorageMounted = Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED);
        if (!isExternalStorageMounted) {
            String message = context.getString(R.string.message_view_status_attachment_not_saved);
            displayMessageToUser(message);
            return;
        }

        if (needsDownloading()) {
            downloadAndSaveAttachmentTo((LocalPart) attachment.part, directory);
        } else {
            saveLocalAttachmentTo(directory);
        }
    }

    private final void saveLocalAttachmentTo(final File directory) {
        new SaveAttachmentAsyncTask(context, attachment, handler).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, directory);
    }

    private final void displayMessageToUser(final String message) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show();
    }
}
