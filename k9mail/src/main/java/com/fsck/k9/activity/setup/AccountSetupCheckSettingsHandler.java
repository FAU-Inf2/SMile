package com.fsck.k9.activity.setup;

import android.app.Activity;
import android.os.Handler;
import android.support.annotation.StringRes;

import com.fsck.k9.mail.CertificateValidationException;

import java.lang.ref.WeakReference;

public class AccountSetupCheckSettingsHandler extends Handler {
    private WeakReference<AccountSetupCheckSettings> reference;

    public AccountSetupCheckSettingsHandler(AccountSetupCheckSettings checkSettings) {
        reference = new WeakReference<>(checkSettings);
    }

    public void showErrorDialog(@StringRes int message, String exceptionMessage) {
        AccountSetupCheckSettings ref = reference.get();
        if(ref != null) {
            ref.showErrorDialog(message, exceptionMessage);
        }
    }

    public void handleCertificateValidationException(CertificateValidationException cve) {
        AccountSetupCheckSettings ref = reference.get();
        if(ref != null) {
            ref.handleCertificateValidationException(cve);
        }
    }

    public void checkFinished() {
        AccountSetupCheckSettings ref = reference.get();
        if(ref != null) {
            ref.setResult(Activity.RESULT_OK);
            ref.finish();
        }
    }

    public void setMessage(@StringRes int message) {
        AccountSetupCheckSettings ref = reference.get();
        if(ref != null) {
            ref.setMessage(message);
        }
    }
}
