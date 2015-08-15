package com.fsck.k9.ui.messageview;


import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import com.fsck.k9.Account;
import com.fsck.k9.K9;
import com.fsck.k9.Preferences;
import de.fau.cs.mad.smile.android.R;
import com.fsck.k9.cache.TemporaryAttachmentStore;
import com.fsck.k9.controller.MessagingController;
import com.fsck.k9.controller.MessagingListener;
import com.fsck.k9.helper.FileHelper;
import com.fsck.k9.helper.MediaScannerNotifier;
import com.fsck.k9.mail.Message;
import com.fsck.k9.mail.Part;
import com.fsck.k9.mail.internet.MimeUtility;
import com.fsck.k9.mailstore.AttachmentViewInfo;
import com.fsck.k9.mailstore.LocalMessage;
import com.fsck.k9.mailstore.LocalPart;
import org.apache.commons.io.IOUtils;


public final class AttachmentController {
    private final Context context;
    private final MessagingController controller;
    private final MessageViewFragment messageViewFragment;
    private final AttachmentViewInfo attachment;

    public AttachmentController(final MessagingController controller,
                         final MessageViewFragment messageViewFragment, // FIXME: pass Handler instead of Fragment
                         final AttachmentViewInfo attachment) {
        this.context = messageViewFragment.getContext();
        this.controller = controller;
        this.messageViewFragment = messageViewFragment;
        this.attachment = attachment;
    }

    public void viewAttachment() {
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
                messageViewFragment.refreshAttachmentThumbnail(attachment);
                saveAttachmentTo(directory);
            }
        });
    }

    private void downloadAttachment(final LocalPart localPart, final Runnable attachmentDownloadedCallback) {
        final String accountUuid = localPart.getAccountUuid();
        final Account account = Preferences.getPreferences(context).getAccount(accountUuid);
        final LocalMessage message = localPart.getMessage();

        messageViewFragment.showAttachmentLoadingDialog();
        controller.loadAttachment(account, message, attachment.part, new MessagingListener() {
            @Override
            public void loadAttachmentFinished(final Account account, final Message message, final Part part) {
                messageViewFragment.hideAttachmentLoadingDialogOnMainThread();
                messageViewFragment.runOnMainThread(attachmentDownloadedCallback);
            }

            @Override
            public void loadAttachmentFailed(final Account account, final Message message, final Part part, final String reason) {
                messageViewFragment.hideAttachmentLoadingDialogOnMainThread();
            }
        });
    }

    private final void viewLocalAttachment() {
        new ViewAttachmentAsyncTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
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
        new SaveAttachmentAsyncTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, directory);
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

    private final Intent getBestViewIntentAndSaveFileIfNecessary() {
        final String displayName = attachment.displayName;
        final String mimeType = attachment.mimeType;
        final String inferredMimeType = MimeUtility.getMimeTypeByExtension(displayName);

        IntentAndResolvedActivitiesCount resolvedIntentInfo;
        if (MimeUtility.isDefaultMimeType(mimeType)) {
            resolvedIntentInfo = getBestViewIntentForMimeType(inferredMimeType);
        } else {
            resolvedIntentInfo = getBestViewIntentForMimeType(mimeType);
            if (!resolvedIntentInfo.hasResolvedActivities() && !inferredMimeType.equals(mimeType)) {
                resolvedIntentInfo = getBestViewIntentForMimeType(inferredMimeType);
            }
        }

        if (!resolvedIntentInfo.hasResolvedActivities()) {
            resolvedIntentInfo = getBestViewIntentForMimeType(MimeUtility.DEFAULT_ATTACHMENT_MIME_TYPE);
        }

        Intent viewIntent;
        if (resolvedIntentInfo.hasResolvedActivities() && resolvedIntentInfo.containsFileUri()) {
            try {
                File tempFile = TemporaryAttachmentStore.getFileForWriting(context, displayName);
                writeAttachmentToStorage(tempFile);
                viewIntent = createViewIntentForFileUri(resolvedIntentInfo.getMimeType(), Uri.fromFile(tempFile));
            } catch (IOException e) {
                if (K9.DEBUG) {
                    Log.e(K9.LOG_TAG, "Error while saving attachment to use file:// URI with ACTION_VIEW Intent", e);
                }
                viewIntent = createViewIntentForAttachmentProviderUri(MimeUtility.DEFAULT_ATTACHMENT_MIME_TYPE);
            }
        } else {
            viewIntent = resolvedIntentInfo.getIntent();
        }

        return viewIntent;
    }

    private final IntentAndResolvedActivitiesCount getBestViewIntentForMimeType(final String mimeType) {
        final Intent contentUriIntent = createViewIntentForAttachmentProviderUri(mimeType);
        final int contentUriActivitiesCount = getResolvedIntentActivitiesCount(contentUriIntent);

        if (contentUriActivitiesCount > 0) {
            return new IntentAndResolvedActivitiesCount(contentUriIntent, contentUriActivitiesCount);
        }

        final File tempFile = TemporaryAttachmentStore.getFile(context, attachment.displayName);
        final Uri tempFileUri = Uri.fromFile(tempFile);
        final Intent fileUriIntent = createViewIntentForFileUri(mimeType, tempFileUri);
        final int fileUriActivitiesCount = getResolvedIntentActivitiesCount(fileUriIntent);

        if (fileUriActivitiesCount > 0) {
            return new IntentAndResolvedActivitiesCount(fileUriIntent, fileUriActivitiesCount);
        }

        return new IntentAndResolvedActivitiesCount(contentUriIntent, contentUriActivitiesCount);
    }

    private final Intent createViewIntentForAttachmentProviderUri(final String mimeType) {
        final Uri uri = getAttachmentUriForMimeType(attachment, mimeType);

        final Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(uri, mimeType);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        addUiIntentFlags(intent);

        return intent;
    }

    private final Uri getAttachmentUriForMimeType(final AttachmentViewInfo attachment, final String mimeType) {
        if (attachment.mimeType.equals(mimeType)) {
            return attachment.uri;
        }

        return attachment.uri.buildUpon()
                .appendPath(mimeType)
                .build();
    }

    private final Intent createViewIntentForFileUri(final String mimeType, final Uri uri) {
        final Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(uri, mimeType);
        addUiIntentFlags(intent);

        return intent;
    }

    private final void addUiIntentFlags(final Intent intent) {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
    }

    private final int getResolvedIntentActivitiesCount(final Intent intent) {
        final PackageManager packageManager = context.getPackageManager();

        final List<ResolveInfo> resolveInfos =
                packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);

        return resolveInfos.size();
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
        Toast.makeText(context, message, Toast.LENGTH_LONG).show();
    }

    private static class IntentAndResolvedActivitiesCount {
        private final Intent intent;
        private final int activitiesCount;

        IntentAndResolvedActivitiesCount(final Intent intent, final int activitiesCount) {
            this.intent = intent;
            this.activitiesCount = activitiesCount;
        }

        public Intent getIntent() {
            return intent;
        }

        public boolean hasResolvedActivities() {
            return activitiesCount > 0;
        }

        public String getMimeType() {
            return intent.getType();
        }

        public boolean containsFileUri() {
            return "file".equals(intent.getData().getScheme());
        }
    }

    // FIXME: use callbacks or handler
    private final class ViewAttachmentAsyncTask extends AsyncTask<Void, Void, Intent> {

        @Override
        protected void onPreExecute() {
            messageViewFragment.disableAttachmentButtons(attachment);
        }

        @Override
        protected Intent doInBackground(Void... params) {
            return getBestViewIntentAndSaveFileIfNecessary();
        }

        @Override
        protected void onPostExecute(Intent intent) {
            viewAttachment(intent);
            messageViewFragment.enableAttachmentButtons(attachment);
        }

        private void viewAttachment(Intent intent) {
            try {
                context.startActivity(intent);
            } catch (ActivityNotFoundException e) {
                Log.e(K9.LOG_TAG, "Could not display attachment of type " + attachment.mimeType, e);

                String message = context.getString(R.string.message_view_no_viewer, attachment.mimeType);
                displayMessageToUser(message);
            }
        }
    }

    // FIXME: use callbacks or handler
    private final class SaveAttachmentAsyncTask extends AsyncTask<File, Void, File> {

        @Override
        protected void onPreExecute() {
            messageViewFragment.disableAttachmentButtons(attachment);
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
            messageViewFragment.enableAttachmentButtons(attachment);
            if (file != null) {
                displayAttachmentSavedMessage(file.toString());
                MediaScannerNotifier.notify(context, file);
            } else {
                displayAttachmentNotSavedMessage();
            }
        }
    }
}
