package com.fsck.k9.ui.messageview;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import com.fsck.k9.K9;
import com.fsck.k9.cache.TemporaryAttachmentStore;
import com.fsck.k9.mail.internet.MimeUtility;
import com.fsck.k9.mailstore.AttachmentViewInfo;

import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import de.fau.cs.mad.smile.android.R;

final class ViewAttachmentAsyncTask extends AsyncTask<Void, Void, Intent> {
    private final Context context;
    private final MessageViewFragment.MessageViewHandler messageViewHandler;
    private final AttachmentViewInfo attachment;

    public ViewAttachmentAsyncTask(final Context context, final MessageViewFragment.MessageViewHandler handler, final AttachmentViewInfo attachment) {
        this.context = context;
        this.messageViewHandler = handler;
        this.attachment = attachment;
    }

    @Override
    protected void onPreExecute() {
        messageViewHandler.disableAttachmentButtons(attachment);
    }

    @Override
    protected Intent doInBackground(Void... params) {
        return getBestViewIntentAndSaveFileIfNecessary();
    }

    @Override
    protected void onPostExecute(Intent intent) {
        viewAttachment(intent);
        messageViewHandler.enableAttachmentButtons(attachment);
    }

    private final void viewAttachment(final Intent intent) {
        try {
            context.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Log.e(K9.LOG_TAG, "Could not display attachment of type " + attachment.mimeType, e);

            String message = context.getString(R.string.message_view_no_viewer, attachment.mimeType);
            displayMessageToUser(message);
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

    // TODO: make sure it runs on ui thread
    private final void displayMessageToUser(final String message) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show();
    }
}
