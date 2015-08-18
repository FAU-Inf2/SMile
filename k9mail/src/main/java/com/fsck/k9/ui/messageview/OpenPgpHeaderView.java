
package com.fsck.k9.ui.messageview;


import android.app.PendingIntent;
import android.content.Context;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.support.annotation.ColorRes;
import android.support.annotation.DrawableRes;
import android.support.annotation.StringRes;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.fsck.k9.mailstore.OpenPgpResultAnnotation;

import org.openintents.openpgp.OpenPgpError;
import org.openintents.openpgp.OpenPgpSignatureResult;
import org.openintents.openpgp.util.OpenPgpUtils;

import de.fau.cs.mad.smile.android.R;


public final class OpenPgpHeaderView extends LinearLayout {
    private Context context;

    private OpenPgpHeaderViewCallback callback;
    private OpenPgpResultAnnotation cryptoAnnotation;

    private ImageView resultEncryptionIcon;
    private TextView resultEncryptionText;
    private ImageView resultSignatureIcon;
    private TextView resultSignatureText;
    private LinearLayout resultSignatureLayout;
    private TextView resultSignatureName;
    private TextView resultSignatureEmail;
    private Button resultSignatureButton;


    public OpenPgpHeaderView(final Context context, final AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
    }

    @Override
    public void onFinishInflate() {
        resultEncryptionIcon = (ImageView) findViewById(R.id.result_encryption_icon);
        resultEncryptionText = (TextView) findViewById(R.id.result_encryption_text);
        resultSignatureIcon = (ImageView) findViewById(R.id.result_signature_icon);
        resultSignatureText = (TextView) findViewById(R.id.result_signature_text);
        resultSignatureLayout = (LinearLayout) findViewById(R.id.result_signature_layout);
        resultSignatureName = (TextView) findViewById(R.id.result_signature_name);
        resultSignatureEmail = (TextView) findViewById(R.id.result_signature_email);
        resultSignatureButton = (Button) findViewById(R.id.result_signature_button);
        super.onFinishInflate();
    }

    public void setCallback(final OpenPgpHeaderViewCallback callback) {
        this.callback = callback;
    }

    public void setOpenPgpData(final OpenPgpResultAnnotation cryptoAnnotation) {
        this.cryptoAnnotation = cryptoAnnotation;

        initializeEncryptionHeader();
        initializeSignatureHeader();
    }

    private final void initializeEncryptionHeader() {
        if (noCryptoAnnotationFound()) {
            displayNotEncrypted();
            return;
        }

        switch (cryptoAnnotation.getErrorType()) {
            case NONE: {
                if (cryptoAnnotation.wasEncrypted()) {
                    displayEncrypted();
                } else {
                    displayNotEncrypted();
                }
                break;
            }
            case CRYPTO_API_RETURNED_ERROR: {
                displayEncryptionError();
                break;
            }
            case ENCRYPTED_BUT_INCOMPLETE: {
                displayIncompleteEncryptedPart();
                break;
            }
            case SIGNED_BUT_INCOMPLETE: {
                displayNotEncrypted();
                break;
            }
        }
    }

    private final boolean noCryptoAnnotationFound() {
        return cryptoAnnotation == null;
    }

    private final void displayEncrypted() {
        setEncryptionImageAndTextColor(CryptoState.ENCRYPTED);
        resultEncryptionText.setText(R.string.openpgp_result_encrypted);
    }

    private final void displayNotEncrypted() {
        setEncryptionImageAndTextColor(CryptoState.NOT_ENCRYPTED);
        resultEncryptionText.setText(R.string.openpgp_result_not_encrypted);
    }

    private final void displayEncryptionError() {
        setEncryptionImageAndTextColor(CryptoState.INVALID);

        OpenPgpError error = cryptoAnnotation.getError();
        String text;
        if (error == null) {
            text = context.getString(R.string.openpgp_unknown_error);
        } else {
            text = context.getString(R.string.openpgp_error, error.getMessage());
        }
        resultEncryptionText.setText(text);
    }

    private final void displayIncompleteEncryptedPart() {
        setEncryptionImageAndTextColor(CryptoState.UNAVAILABLE);
        resultEncryptionText.setText(R.string.crypto_incomplete_message);
    }

    private final void initializeSignatureHeader() {
        initializeSignatureButton();

        if (noCryptoAnnotationFound()) {
            displayNotSigned();
            return;
        }

        switch (cryptoAnnotation.getErrorType()) {
            case CRYPTO_API_RETURNED_ERROR:
            case NONE: {
                displayVerificationResult();
                break;
            }
            case ENCRYPTED_BUT_INCOMPLETE:
            case SIGNED_BUT_INCOMPLETE: {
                displayIncompleteSignedPart();
                break;
            }
        }
    }

    private final void displayIncompleteSignedPart() {
        setSignatureImageAndTextColor(CryptoState.UNAVAILABLE);
        resultSignatureText.setText(R.string.crypto_incomplete_message);
        hideSignatureLayout();
    }

    private final void displayVerificationResult() {
        OpenPgpSignatureResult signatureResult = cryptoAnnotation.getSignatureResult();
        if (signatureResult == null) {
            displayNotSigned();
            return;
        }

        switch (signatureResult.getStatus()) {
            case OpenPgpSignatureResult.SIGNATURE_ERROR: {
                displaySignatureError();
                break;
            }
            case OpenPgpSignatureResult.SIGNATURE_SUCCESS_CERTIFIED: {
                displaySignatureSuccessCertified();
                break;
            }
            case OpenPgpSignatureResult.SIGNATURE_KEY_MISSING: {
                displaySignatureKeyMissing();
                break;
            }
            case OpenPgpSignatureResult.SIGNATURE_SUCCESS_UNCERTIFIED: {
                displaySignatureSuccessUncertified();
                break;
            }
            case OpenPgpSignatureResult.SIGNATURE_KEY_EXPIRED: {
                displaySignatureKeyExpired();
                break;
            }
            case OpenPgpSignatureResult.SIGNATURE_KEY_REVOKED: {
                displaySignatureKeyRevoked();
                break;
            }
        }
    }

    private final void initializeSignatureButton() {
        if (noCryptoAnnotationFound()) {
            hideSignatureButton();
        } else if (isSignatureButtonUsed()) {
            setSignatureButtonClickListener();
        } else {
            hideSignatureButton();
        }
    }

    private final boolean isSignatureButtonUsed() {
        return cryptoAnnotation.getPendingIntent() != null;
    }

    private final void setSignatureButtonClickListener() {
        final PendingIntent pendingIntent = cryptoAnnotation.getPendingIntent();
        resultSignatureButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                callback.onPgpSignatureButtonClick(pendingIntent);
            }
        });
    }

    private final void hideSignatureButton() {
        resultSignatureButton.setVisibility(View.GONE);
        resultSignatureButton.setOnClickListener(null);
    }

    private final void showSignatureButtonWithTextIfNecessary(@StringRes int stringId) {
        if (isSignatureButtonUsed()) {
            resultSignatureButton.setVisibility(View.VISIBLE);
            resultSignatureButton.setText(stringId);
        }
    }

    private final void displayNotSigned() {
        setSignatureImageAndTextColor(CryptoState.NOT_SIGNED);
        resultSignatureText.setText(R.string.openpgp_result_no_signature);
        hideSignatureLayout();
    }

    private final void displaySignatureError() {
        setSignatureImageAndTextColor(CryptoState.INVALID);
        resultSignatureText.setText(R.string.openpgp_result_invalid_signature);
        hideSignatureLayout();
    }

    private final void displaySignatureSuccessCertified() {
        setSignatureImageAndTextColor(CryptoState.VERIFIED);
        resultSignatureText.setText(R.string.openpgp_result_signature_certified);

        displayUserIdAndSignatureButton();
    }

    private final void displaySignatureKeyMissing() {
        setSignatureImageAndTextColor(CryptoState.UNKNOWN_KEY);
        resultSignatureText.setText(R.string.openpgp_result_signature_missing_key);

        setUserId(cryptoAnnotation.getSignatureResult());
        showSignatureButtonWithTextIfNecessary(R.string.openpgp_result_action_lookup);
        showSignatureLayout();
    }

    private final void displaySignatureSuccessUncertified() {
        setSignatureImageAndTextColor(CryptoState.UNVERIFIED);
        resultSignatureText.setText(R.string.openpgp_result_signature_uncertified);

        displayUserIdAndSignatureButton();
    }

    private final void displaySignatureKeyExpired() {
        setSignatureImageAndTextColor(CryptoState.EXPIRED);
        resultSignatureText.setText(R.string.openpgp_result_signature_expired_key);

        displayUserIdAndSignatureButton();
    }

    private final void displaySignatureKeyRevoked() {
        setSignatureImageAndTextColor(CryptoState.REVOKED);
        resultSignatureText.setText(R.string.openpgp_result_signature_revoked_key);

        displayUserIdAndSignatureButton();
    }

    private final void displayUserIdAndSignatureButton() {
        setUserId(cryptoAnnotation.getSignatureResult());
        showSignatureButtonWithTextIfNecessary(R.string.openpgp_result_action_show);
        showSignatureLayout();
    }

    private final void setUserId(final OpenPgpSignatureResult signatureResult) {
        final OpenPgpUtils.UserId userInfo = OpenPgpUtils.splitUserId(signatureResult.getPrimaryUserId());
        if (userInfo.name != null) {
            resultSignatureName.setText(userInfo.name);
        } else {
            resultSignatureName.setText(R.string.openpgp_result_no_name);
        }

        if (userInfo.email != null) {
            resultSignatureEmail.setText(userInfo.email);
        } else {
            resultSignatureEmail.setText(R.string.openpgp_result_no_email);
        }
    }

    private final void hideSignatureLayout() {
        resultSignatureLayout.setVisibility(View.GONE);
    }

    private final void showSignatureLayout() {
        resultSignatureLayout.setVisibility(View.VISIBLE);
    }

    private final void setEncryptionImageAndTextColor(final CryptoState state) {
        setStatusImageAndTextColor(resultEncryptionIcon, resultEncryptionText, state);
    }

    private final void setSignatureImageAndTextColor(final CryptoState state) {
        setStatusImageAndTextColor(resultSignatureIcon, resultSignatureText, state);
    }

    private final void setStatusImageAndTextColor(final ImageView statusIcon, final TextView statusText, final CryptoState state) {
        final Drawable statusImageDrawable = context.getResources().getDrawable(state.getDrawableId());
        statusIcon.setImageDrawable(statusImageDrawable);

        final int color = context.getResources().getColor(state.getColorId());
        statusIcon.setColorFilter(color, PorterDuff.Mode.SRC_IN);
        if (statusText != null) {
            statusText.setTextColor(color);
        }
    }


    private enum CryptoState {
        VERIFIED(R.drawable.status_signature_verified_cutout, R.color.openpgp_green),
        ENCRYPTED(R.drawable.status_lock_closed, R.color.openpgp_green),

        UNAVAILABLE(R.drawable.status_signature_unverified_cutout, R.color.openpgp_orange),
        UNVERIFIED(R.drawable.status_signature_unverified_cutout, R.color.openpgp_orange),
        UNKNOWN_KEY(R.drawable.status_signature_unknown_cutout, R.color.openpgp_orange),

        REVOKED(R.drawable.status_signature_revoked_cutout, R.color.openpgp_red),
        EXPIRED(R.drawable.status_signature_expired_cutout, R.color.openpgp_red),
        NOT_ENCRYPTED(R.drawable.status_lock_open, R.color.openpgp_red),
        NOT_SIGNED(R.drawable.status_signature_unknown_cutout, R.color.openpgp_red),
        INVALID(R.drawable.status_signature_invalid_cutout, R.color.openpgp_red);


        private final int drawableId;
        private final int colorId;

        CryptoState(@DrawableRes int drawableId, @ColorRes int colorId) {
            this.drawableId = drawableId;
            this.colorId = colorId;
        }

        @DrawableRes
        public int getDrawableId() {
            return drawableId;
        }

        @ColorRes
        public int getColorId() {
            return colorId;
        }
    }
}
