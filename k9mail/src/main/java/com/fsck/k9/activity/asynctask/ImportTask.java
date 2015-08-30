package com.fsck.k9.activity.asynctask;

import android.app.ProgressDialog;
import android.net.Uri;
import android.util.Log;

import com.fsck.k9.K9;
import com.fsck.k9.activity.Accounts;
import com.fsck.k9.preferences.SettingsImportExportException;
import com.fsck.k9.preferences.SettingsImporter;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import de.fau.cs.mad.smile.android.R;

/**
 * Handles importing of global settings and/or accounts in a background thread.
 */
public class ImportTask extends ExtendedAsyncTask<Void, Void, Boolean> {
    private boolean mIncludeGlobals;
    private List<String> mAccountUuids;
    private boolean mOverwrite;
    private Uri mUri;
    private SettingsImporter.ImportResults mImportResults;

    public ImportTask(Accounts activity, boolean includeGlobals,
                      List<String> accountUuids, boolean overwrite, Uri uri) {
        super(activity);
        mIncludeGlobals = includeGlobals;
        mAccountUuids = accountUuids;
        mOverwrite = overwrite;
        mUri = uri;
    }

    @Override
    protected void showProgressDialog() {
        String title = mContext.getString(R.string.settings_import_dialog_title);
        String message = mContext.getString(R.string.settings_importing);
        mProgressDialog = ProgressDialog.show(mActivity, title, message, true);
    }

    @Override
    protected Boolean doInBackground(Void... params) {
        try {
            InputStream is = mContext.getContentResolver().openInputStream(mUri);
            try {
                mImportResults = SettingsImporter.importSettings(mContext, is,
                        mIncludeGlobals, mAccountUuids, mOverwrite);
            } finally {
                try {
                    is.close();
                } catch (IOException e) {
                    /* Ignore */
                }
            }
        } catch (SettingsImportExportException e) {
            Log.w(K9.LOG_TAG, "Exception during import", e);
            return false;
        } catch (FileNotFoundException e) {
            Log.w(K9.LOG_TAG, "Couldn't open import file", e);
            return false;
        } catch (Exception e) {
            Log.w(K9.LOG_TAG, "Unknown error", e);
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

        String filename = mUri.getLastPathSegment();
        boolean globalSettings = mImportResults.globalSettings;
        int imported = mImportResults.importedAccounts.size();
        if (success && (globalSettings || imported > 0)) {
            if (imported == 0) {
                activity.showSimpleDialog(R.string.settings_import_success_header,
                        R.string.settings_import_global_settings_success, filename);
            } else {
                activity.showAccountsImportedDialog(mImportResults, filename);
            }

            activity.refresh();
        } else {
            //TODO: better error messages
            activity.showSimpleDialog(R.string.settings_import_failed_header,
                    R.string.settings_import_failure, filename);
        }
    }
}
