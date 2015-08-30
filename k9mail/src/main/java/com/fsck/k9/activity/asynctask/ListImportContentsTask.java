package com.fsck.k9.activity.asynctask;

import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.net.Uri;
import android.util.Log;

import com.fsck.k9.K9;
import com.fsck.k9.activity.Accounts;
import com.fsck.k9.preferences.SettingsImportExportException;
import com.fsck.k9.preferences.SettingsImporter;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import de.fau.cs.mad.smile.android.R;

public class ListImportContentsTask extends ExtendedAsyncTask<Void, Void, Boolean> {
    private Uri mUri;
    private SettingsImporter.ImportContents mImportContents;

    public ListImportContentsTask(Accounts activity, Uri uri) {
        super(activity);

        mUri = uri;
    }

    @Override
    protected void showProgressDialog() {
        String title = mContext.getString(R.string.settings_import_dialog_title);
        String message = mContext.getString(R.string.settings_import_scanning_file);
        mProgressDialog = ProgressDialog.show(mActivity, title, message, true);
    }

    @Override
    protected Boolean doInBackground(Void... params) {
        try {
            ContentResolver resolver = mContext.getContentResolver();
            InputStream is = resolver.openInputStream(mUri);
            try {
                mImportContents = SettingsImporter.getImportStreamContents(is);
            } finally {
                try {
                    is.close();
                } catch (IOException e) {
                    /* Ignore */
                }
            }
        } catch (SettingsImportExportException e) {
            Log.w(K9.LOG_TAG, "Exception during export", e);
            return false;
        } catch (FileNotFoundException e) {
            Log.w(K9.LOG_TAG, "Couldn't read content from URI " + mUri);
            return false;
        }
        return true;
    }

    @Override
    protected void onPostExecute(Boolean success) {
        Accounts activity = (Accounts) mActivity;

        // Let the activity know that the background task is complete
        activity.setNonConfigurationInstance(null);

        removeProgressDialog();

        if (success) {
            activity.showImportSelectionDialog(mImportContents, mUri);
        } else {
            String filename = mUri.getLastPathSegment();
            //TODO: better error messages
            activity.showSimpleDialog(R.string.settings_import_failed_header,
                    R.string.settings_import_failure, filename);
        }
    }
}
