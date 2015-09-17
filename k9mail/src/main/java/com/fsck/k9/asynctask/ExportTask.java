package com.fsck.k9.asynctask;

import android.app.ProgressDialog;
import android.util.Log;

import com.fsck.k9.K9;
import com.fsck.k9.activity.Accounts;
import com.fsck.k9.preferences.SettingsExporter;
import com.fsck.k9.preferences.SettingsImportExportException;

import java.util.Set;

import de.fau.cs.mad.smile.android.R;

/**
 * Handles exporting of global settings and/or accounts in a background thread.
 */
public class ExportTask extends ExtendedAsyncTask<Void, Void, Boolean> {
    private boolean mIncludeGlobals;
    private Set<String> mAccountUuids;
    private String mFileName;

    public ExportTask(Accounts activity, boolean includeGlobals,
                      Set<String> accountUuids) {
        super(activity);
        mIncludeGlobals = includeGlobals;
        mAccountUuids = accountUuids;
    }

    @Override
    protected void showProgressDialog() {
        String title = mContext.getString(R.string.settings_export_dialog_title);
        String message = mContext.getString(R.string.settings_exporting);
        mProgressDialog = ProgressDialog.show(mActivity, title, message, true);
    }

    @Override
    protected Boolean doInBackground(Void... params) {
        try {
            mFileName = SettingsExporter.exportToFile(mContext, mIncludeGlobals,
                    mAccountUuids);
        } catch (SettingsImportExportException e) {
            Log.w(K9.LOG_TAG, "Exception during export", e);
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
            activity.showSimpleDialog(R.string.settings_export_success_header,
                    R.string.settings_export_success, mFileName);
        } else {
            //TODO: better error messages
            activity.showSimpleDialog(R.string.settings_export_failed_header,
                    R.string.settings_export_failure);
        }
    }
}
