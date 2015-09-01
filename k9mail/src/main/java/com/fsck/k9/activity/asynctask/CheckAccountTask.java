package com.fsck.k9.activity.asynctask;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.fsck.k9.Account;
import com.fsck.k9.K9;
import com.fsck.k9.activity.setup.AccountSetupCheckSettings;
import com.fsck.k9.activity.setup.AccountSetupCheckSettingsHandler;
import com.fsck.k9.controller.MessagingController;
import com.fsck.k9.helper.NotificationHelper;
import com.fsck.k9.mail.AuthenticationFailedException;
import com.fsck.k9.mail.CertificateValidationException;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.Store;
import com.fsck.k9.mail.Transport;
import com.fsck.k9.mail.store.webdav.WebDavStore;

import de.fau.cs.mad.smile.android.R;

/**
 * FIXME: Don't use an AsyncTask to perform network operations.
 * See also discussion in https://github.com/k9mail/k-9/pull/560
 */
public class CheckAccountTask extends AsyncTask<Void, Integer, Void> {
    private final Context context;
    private final MessagingController controller;
    private final NotificationHelper helper;
    private final Account account;
    private final AccountSetupCheckSettings.CheckDirection checkDirection;
    private final AccountSetupCheckSettingsHandler handler;

    public CheckAccountTask(Context context, AccountSetupCheckSettingsHandler handler, Account account, AccountSetupCheckSettings.CheckDirection checkDirection) {
        this.context = context;
        this.account = account;
        this.controller = MessagingController.getInstance(context);
        this.helper = NotificationHelper.getInstance(context);
        this.checkDirection = checkDirection;
        this.handler = handler;
    }

    @Override
    protected Void doInBackground(Void... params) {
        try {
            /*
             * This task could be interrupted at any point, but network operations can block,
             * so relying on InterruptedException is not enough. Instead, check after
             * each potentially long-running operation.
             */
            if (isCancelled()) {
                return null;
            }

            clearCertificateErrorNotifications(checkDirection);
            checkServerSettings(checkDirection);

            if (isCancelled()) {
                return null;
            }

            handler.checkFinished();
        } catch (AuthenticationFailedException afe) {
            Log.e(K9.LOG_TAG, "Error while testing settings", afe);
            handler.showErrorDialog(
                    R.string.account_setup_failed_dlg_auth_message_fmt,
                    afe.getMessage() == null ? "" : afe.getMessage());
        } catch (CertificateValidationException cve) {
            handler.handleCertificateValidationException(cve);
        } catch (Throwable t) {
            Log.e(K9.LOG_TAG, "Error while testing settings", t);
            handler.showErrorDialog(
                    R.string.account_setup_failed_dlg_server_message_fmt,
                    (t.getMessage() == null ? "" : t.getMessage()));
        }

        return null;
    }

    private void clearCertificateErrorNotifications(AccountSetupCheckSettings.CheckDirection direction) {
        helper.clearCertificateErrorNotifications(context, account, direction);
    }

    private void checkServerSettings(AccountSetupCheckSettings.CheckDirection direction) throws MessagingException {
        switch (direction) {
            case INCOMING: {
                checkIncoming();
                break;
            }
            case OUTGOING: {
                checkOutgoing();
                break;
            }
        }
    }

    private void checkIncoming() throws MessagingException {
        final Store store = account.getRemoteStore();
        if (store instanceof WebDavStore) {
            publishProgress(R.string.account_setup_check_settings_authenticate);
        } else {
            publishProgress(R.string.account_setup_check_settings_check_incoming_msg);
        }

        store.checkSettings();

        if (store instanceof WebDavStore) {
            publishProgress(R.string.account_setup_check_settings_fetch);
        }

        controller.listFoldersSynchronous(account, true, null);
        controller.synchronizeMailbox(account, account.getInboxFolderName(), null, null);
    }

    private void checkOutgoing() throws MessagingException {
        if (!(account.getRemoteStore() instanceof WebDavStore)) {
            publishProgress(R.string.account_setup_check_settings_check_outgoing_msg);
        }

        Transport transport = Transport.getInstance(K9.app, account);
        transport.close();
        transport.open();
        transport.close();
    }

    @Override
    protected void onProgressUpdate(Integer... values) {
        handler.setMessage(values[0]);
    }
}
