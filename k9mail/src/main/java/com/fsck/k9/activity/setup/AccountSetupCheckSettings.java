package com.fsck.k9.activity.setup;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DialogFragment;
import android.app.FragmentTransaction;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.StringRes;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.fsck.k9.Account;
import com.fsck.k9.K9;
import com.fsck.k9.Preferences;
import com.fsck.k9.activity.K9Activity;
import com.fsck.k9.activity.asynctask.CheckAccountTask;
import com.fsck.k9.fragment.ConfirmationDialogFragment;
import com.fsck.k9.fragment.ConfirmationDialogFragment.ConfirmationDialogFragmentListener;
import com.fsck.k9.mail.CertificateValidationException;
import com.fsck.k9.mail.filter.Hex;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

import de.fau.cs.mad.smile.android.R;

/**
 * Checks the given settings to make sure that they can be used to send and
 * receive mail.
 * <p>
 * XXX NOTE: The manifest for this app has it ignore config changes, because
 * it doesn't correctly deal with restarting while its thread is running.
 */
public class AccountSetupCheckSettings extends K9Activity implements OnClickListener,
        ConfirmationDialogFragmentListener {

    public static final int ACTIVITY_REQUEST_CODE = 1;
    private static final String EXTRA_ACCOUNT = "account";
    private static final String EXTRA_CHECK_DIRECTION = "checkDirection";
    private AccountSetupCheckSettingsHandler mHandler = new AccountSetupCheckSettingsHandler(this);
    private ProgressBar mProgressBar;
    private TextView mMessageView;
    private Account mAccount;
    private CheckDirection mDirection;
    private boolean mDestroyed;
    private CheckAccountTask checkAccountTask;

    public static void actionCheckSettings(Activity context, Account account,
                                           CheckDirection direction) {
        Intent i = new Intent(context, AccountSetupCheckSettings.class);
        i.putExtra(EXTRA_ACCOUNT, account.getUuid());
        i.putExtra(EXTRA_CHECK_DIRECTION, direction);
        context.startActivityForResult(i, ACTIVITY_REQUEST_CODE);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.account_setup_check_settings);
        mMessageView = (TextView) findViewById(R.id.message);
        mProgressBar = (ProgressBar) findViewById(R.id.progress);
        findViewById(R.id.cancel).setOnClickListener(this);

        setMessage(R.string.account_setup_check_settings_retr_info_msg);
        mProgressBar.setIndeterminate(true);

        String accountUuid = getIntent().getStringExtra(EXTRA_ACCOUNT);
        mAccount = Preferences.getPreferences(this).getAccount(accountUuid);
        mDirection = (CheckDirection) getIntent().getSerializableExtra(EXTRA_CHECK_DIRECTION);

        checkAccountTask = new CheckAccountTask(getApplicationContext(), mHandler, mAccount, mDirection);
        checkAccountTask.execute();
    }

    public void handleCertificateValidationException(CertificateValidationException cve) {
        Log.e(K9.LOG_TAG, "Error while testing settings", cve);

        X509Certificate[] chain = cve.getCertChain();
        // Avoid NullPointerException in acceptKeyDialog()
        if (chain != null) {
            acceptKeyDialog(
                    R.string.account_setup_failed_dlg_certificate_message_fmt,
                    cve);
        } else {
            showErrorDialog(
                    R.string.account_setup_failed_dlg_server_message_fmt,
                    errorMessageForCertificateException(cve));
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        checkAccountTask.cancel(true);
        mDestroyed = true;
    }

    public void setMessage(@StringRes final int resId) {
        mMessageView.setText(getString(resId));
    }

    private void acceptKeyDialog(@StringRes final int msgResId, final CertificateValidationException ex) {
        mHandler.post(new AcceptKeyRunnable(ex, msgResId));
    }

    /**
     * Permanently accepts a certificate for the INCOMING or OUTGOING direction
     * by adding it to the local key store.
     *
     * @param certificate
     */
    private void acceptCertificate(X509Certificate certificate) {
        try {
            mAccount.addCertificate(mDirection, certificate);
        } catch (CertificateException e) {
            showErrorDialog(
                    R.string.account_setup_failed_dlg_certificate_message_fmt,
                    e.getMessage() == null ? "" : e.getMessage());
        }
        AccountSetupCheckSettings.actionCheckSettings(AccountSetupCheckSettings.this, mAccount,
                mDirection);
    }

    @Override
    public void onActivityResult(int reqCode, int resCode, Intent data) {
        setResult(resCode);
        finish();
    }

    private void onCancel() {
        checkAccountTask.cancel(true);
        setMessage(R.string.account_setup_check_settings_canceling_msg);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.cancel:
                onCancel();
                break;
        }
    }

    public void showErrorDialog(final int msgResId, final Object... args) {
        showDialogFragment(R.id.dialog_account_setup_error, getString(msgResId, args));
    }

    private void showDialogFragment(int dialogId, String customMessage) {
        if (mDestroyed) {
            return;
        }

        mProgressBar.setIndeterminate(false);

        DialogFragment fragment;
        switch (dialogId) {
            case R.id.dialog_account_setup_error: {
                fragment = ConfirmationDialogFragment.newInstance(dialogId,
                        getString(R.string.account_setup_failed_dlg_title),
                        customMessage,
                        getString(R.string.account_setup_failed_dlg_edit_details_action),
                        getString(R.string.account_setup_failed_dlg_continue_action)
                );
                break;
            }
            default: {
                throw new RuntimeException("Called showDialog(int) with unknown dialog id.");
            }
        }

        FragmentTransaction ta = getFragmentManager().beginTransaction();
        ta.add(fragment, getDialogTag(dialogId));
        ta.commitAllowingStateLoss();

        // TODO: commitAllowingStateLoss() is used to prevent https://code.google.com/p/android/issues/detail?id=23761
        // but is a bad...
        //fragment.show(ta, getDialogTag(dialogId));
    }

    private String getDialogTag(int dialogId) {
        return String.format(Locale.US, "dialog-%d", dialogId);
    }

    @Override
    public void doPositiveClick(int dialogId) {
        switch (dialogId) {
            case R.id.dialog_account_setup_error: {
                finish();
                break;
            }
        }
    }

    @Override
    public void doNegativeClick(int dialogId) {
        switch (dialogId) {
            case R.id.dialog_account_setup_error: {
                setResult(RESULT_OK);
                finish();
                break;
            }
        }
    }

    @Override
    public void dialogCancelled(int dialogId) {
        // nothing to do here...
    }

    private String errorMessageForCertificateException(CertificateValidationException e) {
        switch (e.getReason()) {
            case Expired:
                return getString(R.string.client_certificate_expired, e.getAlias(), e.getMessage());
            case MissingCapability:
                return getString(R.string.auth_external_error);
            case RetrievalFailure:
                return getString(R.string.client_certificate_retrieval_failure, e.getAlias());
            case UseMessage:
                return e.getMessage();
            case Unknown:
            default:
                return "";
        }
    }

    public enum CheckDirection {
        INCOMING,
        OUTGOING
    }

    private class AcceptKeyRunnable implements Runnable {
        private final CertificateValidationException ex;
        private final int msgResId;

        public AcceptKeyRunnable(CertificateValidationException ex, int msgResId) {
            this.ex = ex;
            this.msgResId = msgResId;
        }

        @Override
        public void run() {
            if (mDestroyed) {
                return;
            }

            String exMessage = "Unknown Error";

            if (ex != null) {
                if (ex.getCause() != null) {
                    if (ex.getCause().getCause() != null) {
                        exMessage = ex.getCause().getCause().getMessage();

                    } else {
                        exMessage = ex.getCause().getMessage();
                    }
                } else {
                    exMessage = ex.getMessage();
                }
            }

            mProgressBar.setIndeterminate(false);
            StringBuilder chainInfo = new StringBuilder(100);
            MessageDigest sha1 = null;
            try {
                sha1 = MessageDigest.getInstance("SHA-1");
            } catch (NoSuchAlgorithmException e) {
                Log.e(K9.LOG_TAG, "Error while initializing MessageDigest", e);
            }

            final X509Certificate[] chain = ex.getCertChain();
            // We already know chain != null (tested before calling this method)
            for (int i = 0; i < chain.length; i++) {
                // display certificate chain information
                //TODO: localize this strings
                chainInfo.append("Certificate chain[").append(i).append("]:\n");
                chainInfo.append("Subject: ").append(chain[i].getSubjectDN().toString()).append("\n");

                // display SubjectAltNames too
                // (the user may be mislead into mistrusting a certificate
                //  by a subjectDN not matching the server even though a
                //  SubjectAltName matches)
                try {
                    final Collection<List<?>> subjectAlternativeNames = chain[i].getSubjectAlternativeNames();
                    if (subjectAlternativeNames != null) {
                        // The list of SubjectAltNames may be very long
                        //TODO: localize this string
                        StringBuilder altNamesText = new StringBuilder();
                        altNamesText.append("Subject has ").append(subjectAlternativeNames.size()).append(" alternative names\n");

                        // we need these for matching
                        String storeURIHost = (Uri.parse(mAccount.getStoreUri())).getHost();
                        String transportURIHost = (Uri.parse(mAccount.getTransportUri())).getHost();

                        for (List<?> subjectAlternativeName : subjectAlternativeNames) {
                            Integer type = (Integer) subjectAlternativeName.get(0);
                            Object value = subjectAlternativeName.get(1);
                            String name;
                            switch (type) {
                                case 0:
                                    Log.w(K9.LOG_TAG, "SubjectAltName of type OtherName not supported.");
                                    continue;
                                case 1: // RFC822Name
                                    name = (String) value;
                                    break;
                                case 2:  // DNSName
                                    name = (String) value;
                                    break;
                                case 3:
                                    Log.w(K9.LOG_TAG, "unsupported SubjectAltName of type x400Address");
                                    continue;
                                case 4:
                                    Log.w(K9.LOG_TAG, "unsupported SubjectAltName of type directoryName");
                                    continue;
                                case 5:
                                    Log.w(K9.LOG_TAG, "unsupported SubjectAltName of type ediPartyName");
                                    continue;
                                case 6:  // Uri
                                    name = (String) value;
                                    break;
                                case 7: // ip-address
                                    name = (String) value;
                                    break;
                                default:
                                    Log.w(K9.LOG_TAG, "unsupported SubjectAltName of unknown type");
                                    continue;
                            }

                            // if some of the SubjectAltNames match the store or transport -host,
                            // display them
                            if (name.equalsIgnoreCase(storeURIHost) || name.equalsIgnoreCase(transportURIHost)) {
                                //TODO: localize this string
                                altNamesText.append("Subject(alt): ").append(name).append(",...\n");
                            } else if (name.startsWith("*.") && (
                                    storeURIHost.endsWith(name.substring(2)) ||
                                            transportURIHost.endsWith(name.substring(2)))) {
                                //TODO: localize this string
                                altNamesText.append("Subject(alt): ").append(name).append(",...\n");
                            }
                        }
                        chainInfo.append(altNamesText);
                    }
                } catch (Exception e1) {
                    // don't fail just because of subjectAltNames
                    Log.w(K9.LOG_TAG, "cannot display SubjectAltNames in dialog", e1);
                }

                chainInfo.append("Issuer: ").append(chain[i].getIssuerDN().toString()).append("\n");
                if (sha1 != null) {
                    sha1.reset();
                    try {
                        char[] sha1sum = Hex.encodeHex(sha1.digest(chain[i].getEncoded()));
                        chainInfo.append("Fingerprint (SHA-1): ").append(new String(sha1sum)).append("\n");
                    } catch (CertificateEncodingException e) {
                        Log.e(K9.LOG_TAG, "Error while encoding certificate", e);
                    }
                }
            }

            // TODO: refactor with DialogFragment.
            // This is difficult because we need to pass through chain[0] for onClick()
            new AlertDialog.Builder(AccountSetupCheckSettings.this)
                    .setTitle(getString(R.string.account_setup_failed_dlg_invalid_certificate_title))
                            //.setMessage(getString(R.string.account_setup_failed_dlg_invalid_certificate)
                    .setMessage(getString(msgResId, exMessage)
                                    + " " + chainInfo.toString()
                    )
                    .setCancelable(true)
                    .setPositiveButton(
                            getString(R.string.account_setup_failed_dlg_invalid_certificate_accept),
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    acceptCertificate(chain[0]);
                                }
                            })
                    .setNegativeButton(
                            getString(R.string.account_setup_failed_dlg_invalid_certificate_reject),
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    finish();
                                }
                            })
                    .show();
        }
    }
}
