package com.fsck.k9.view;

import android.content.Context;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.ColorRes;
import android.support.annotation.DrawableRes;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.text.style.StyleSpan;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.QuickContactBadge;
import android.widget.TextView;
import android.widget.Toast;

import com.fsck.k9.Account;
import com.fsck.k9.FontSizes;
import com.fsck.k9.K9;
import com.fsck.k9.activity.misc.ContactPictureLoader;
import com.fsck.k9.helper.ContactPicture;
import com.fsck.k9.helper.Contacts;
import com.fsck.k9.helper.MessageHelper;
import com.fsck.k9.mail.Address;
import com.fsck.k9.mail.Flag;
import com.fsck.k9.mail.Message;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.internet.MimeUtility;
import com.fsck.k9.mailstore.CryptoResultAnnotation;
import com.fsck.k9.mailstore.SignatureResult;

import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import de.fau.cs.mad.smile.android.R;

public class MessageHeader extends LinearLayout implements OnClickListener {
    private TextView mFromView;
    private TextView mDateView;
    private TextView mToView;
    private TextView mToLabel;
    private TextView mCcView;
    private TextView mCcLabel;
    private TextView mSubjectView;
    private ImageView mEncryptionIcon;
    private ImageView mSignatureIcon;

    private View mChip;
    private CheckBox mFlagged;
    private int defaultSubjectColor;
    private TextView mAdditionalHeadersView;
    private View mAnsweredIcon;
    private View mForwardedIcon;
    private Message mMessage;
    private Account mAccount;
    private final FontSizes mFontSizes;
    private final Contacts mContacts;
    private SavedState mSavedState;

    private MessageHelper mMessageHelper;
    private ContactPictureLoader mContactsPictureLoader;
    private QuickContactBadge mContactBadge;
    private CryptoResultAnnotation cryptoAnnotation;

    private OnLayoutChangedListener mOnLayoutChangedListener; // TODO: is never set, remove?

    /**
     * Pair class is only available since API Level 5, so we need
     * this helper class unfortunately
     */
    private static class HeaderEntry {
        public String label;
        public String value;

        public HeaderEntry(String label, String value) {
            this.label = label;
            this.value = value;
        }
    }

    public MessageHeader(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContacts = Contacts.getInstance(getContext());
        mFontSizes = K9.getFontSizes();
    }

    @Override
    protected void onFinishInflate() {
        mAnsweredIcon = findViewById(R.id.answered);
        mForwardedIcon = findViewById(R.id.forwarded);
        mEncryptionIcon = (ImageView) findViewById(R.id.encryption_icon);
        mSignatureIcon = (ImageView) findViewById(R.id.signature_icon);
        mFromView = (TextView) findViewById(R.id.from);
        mToView = (TextView) findViewById(R.id.to);
        mToLabel = (TextView) findViewById(R.id.to_label);
        mCcView = (TextView) findViewById(R.id.cc);
        mCcLabel = (TextView) findViewById(R.id.cc_label);

        mContactBadge = (QuickContactBadge) findViewById(R.id.contact_badge);

        mSubjectView = (TextView) findViewById(R.id.subject);
        mAdditionalHeadersView = (TextView) findViewById(R.id.additional_headers_view);
        mChip = findViewById(R.id.chip);
        mDateView = (TextView) findViewById(R.id.date);
        mFlagged = (CheckBox) findViewById(R.id.flagged);

        defaultSubjectColor = mSubjectView.getCurrentTextColor();
        mFontSizes.setViewTextSize(mSubjectView, mFontSizes.getMessageViewSubject());
        mFontSizes.setViewTextSize(mDateView, mFontSizes.getMessageViewDate());
        mFontSizes.setViewTextSize(mAdditionalHeadersView, mFontSizes.getMessageViewAdditionalHeaders());

        mFontSizes.setViewTextSize(mFromView, mFontSizes.getMessageViewSender());
        mFontSizes.setViewTextSize(mToView, mFontSizes.getMessageViewTo());
        mFontSizes.setViewTextSize(mToLabel, mFontSizes.getMessageViewTo());
        mFontSizes.setViewTextSize(mCcView, mFontSizes.getMessageViewCC());
        mFontSizes.setViewTextSize(mCcLabel, mFontSizes.getMessageViewCC());

        mFromView.setOnClickListener(this);
        mToView.setOnClickListener(this);
        mCcView.setOnClickListener(this);

        mMessageHelper = MessageHelper.getInstance(getContext());

        mSubjectView.setVisibility(VISIBLE);
        hideAdditionalHeaders();
        super.onFinishInflate();
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.from: {
                onAddSenderToContacts();
                break;
            }
            case R.id.to:
            case R.id.cc: {
                expand((TextView) view, ((TextView) view).getEllipsize() != null);
                layoutChanged();
            }
        }
    }

    private void onAddSenderToContacts() {
        if (mMessage != null) {
            try {
                final Address senderEmail = mMessage.getFrom()[0];
                mContacts.createContact(senderEmail);
            } catch (Exception e) {
                Log.e(K9.LOG_TAG, "Couldn't create contact", e);
            }
        }
    }

    public void setOnFlagListener(OnClickListener listener) {
        mFlagged.setOnClickListener(listener);
    }

    public boolean additionalHeadersVisible() {
        return (mAdditionalHeadersView != null &&
                mAdditionalHeadersView.getVisibility() == View.VISIBLE);
    }

    /**
     * Clear the text field for the additional headers display if they are
     * not shown, to save UI resources.
     */
    private void hideAdditionalHeaders() {
        mAdditionalHeadersView.setVisibility(View.GONE);
        mAdditionalHeadersView.setText("");
    }

    /**
     * Set up and then show the additional headers view. Called by
     * {@link #onShowAdditionalHeaders()}
     * (when switching between messages).
     */
    private void showAdditionalHeaders() {
        Integer messageToShow = null;
        try {
            // Retrieve additional headers
            boolean allHeadersDownloaded = mMessage.isSet(Flag.X_GOT_ALL_HEADERS);
            List<HeaderEntry> additionalHeaders = getAdditionalHeaders(mMessage);
            if (!additionalHeaders.isEmpty()) {
                // Show the additional headers that we have got.
                populateAdditionalHeadersView(additionalHeaders);
                mAdditionalHeadersView.setVisibility(View.VISIBLE);
            }

            if (!allHeadersDownloaded) {
                /*
                * Tell the user about the "save all headers" setting
                *
                * NOTE: This is only a temporary solution... in fact,
                * the system should download headers on-demand when they
                * have not been saved in their entirety initially.
                */
                messageToShow = R.string.message_additional_headers_not_downloaded;
            } else if (additionalHeaders.isEmpty()) {
                // All headers have been downloaded, but there are no additional headers.
                messageToShow = R.string.message_no_additional_headers_available;
            }
        } catch (Exception e) {
            messageToShow = R.string.message_additional_headers_retrieval_failed;
        }
        // Show a message to the user, if any
        if (messageToShow != null) {
            Toast toast = Toast.makeText(getContext(), messageToShow, Toast.LENGTH_LONG);
            toast.setGravity(Gravity.CENTER_VERTICAL | Gravity.CENTER_HORIZONTAL, 0, 0);
            toast.show();
        }

    }

    public void populate(final Message message, final Account account) throws MessagingException {
        final Contacts contacts = K9.showContactName() ? mContacts : null;
        final CharSequence from = MessageHelper.toFriendly(message.getFrom(), contacts);
        final CharSequence to = MessageHelper.toFriendly(message.getRecipients(Message.RecipientType.TO), contacts);
        final CharSequence cc = MessageHelper.toFriendly(message.getRecipients(Message.RecipientType.CC), contacts);

        Address[] fromAddrs = message.getFrom();
        Address[] toAddrs = message.getRecipients(Message.RecipientType.TO);
        Address[] ccAddrs = message.getRecipients(Message.RecipientType.CC);
        boolean fromMe = mMessageHelper.toMe(account, fromAddrs);

        Address counterpartyAddress = null;
        if (fromMe) {
            if (toAddrs.length > 0) {
                counterpartyAddress = toAddrs[0];
            } else if (ccAddrs.length > 0) {
                counterpartyAddress = ccAddrs[0];
            }
        } else if (fromAddrs.length > 0) {
            counterpartyAddress = fromAddrs[0];
        }

        /*
         * Only reset visibility of the subject if populate() was called because a new
         * message is shown. If it is the same, do not force the subject visible, because
         * this breaks the MessageTitleView in the action bar, which may hide our subject
         * if it fits in the action bar but is only called when a new message is shown
         * or the device is rotated.
         */
        if (mMessage == null || mMessage.getId() != message.getId()) {
            mSubjectView.setVisibility(VISIBLE);
        }

        mMessage = message;
        mAccount = account;

        if (K9.showContactPicture()) {
            mContactBadge.setVisibility(View.VISIBLE);
            mContactsPictureLoader = ContactPicture.getContactPictureLoader(getContext());
        } else {
            mContactBadge.setVisibility(View.GONE);
        }

        final String subject = message.getSubject();
        if (TextUtils.isEmpty(subject)) {
            mSubjectView.setText(getContext().getText(R.string.general_no_subject));
        } else {
            mSubjectView.setText(subject);
        }

        mSubjectView.setTextColor(0xff000000 | defaultSubjectColor);

        String dateTime = DateUtils.formatDateTime(getContext(),
                message.getSentDate().getTime(),
                DateUtils.FORMAT_SHOW_DATE
                        | DateUtils.FORMAT_ABBREV_ALL
                        | DateUtils.FORMAT_SHOW_TIME
                        | DateUtils.FORMAT_SHOW_YEAR);
        mDateView.setText(dateTime);

        if (K9.showContactPicture()) {
            if (counterpartyAddress != null) {
                mContactBadge.assignContactFromEmail(counterpartyAddress.getAddress(), true);
                mContactsPictureLoader.loadContactPicture(counterpartyAddress, mContactBadge);
            } else {
                mContactBadge.setImageResource(R.drawable.ic_contact_picture);
            }
        }

        mFromView.setText(from);

        updateAddressField(mToView, to, mToLabel);
        updateAddressField(mCcView, cc, mCcLabel);
        mAnsweredIcon.setVisibility(message.isSet(Flag.ANSWERED) ? View.VISIBLE : View.GONE);
        mForwardedIcon.setVisibility(message.isSet(Flag.FORWARDED) ? View.VISIBLE : View.GONE);
        mFlagged.setChecked(message.isSet(Flag.FLAGGED));

        mChip.setBackgroundColor(mAccount.getChipColor());

        setVisibility(View.VISIBLE);

        if (mSavedState != null) {
            if (mSavedState.additionalHeadersVisible) {
                showAdditionalHeaders();
            }
            mSavedState = null;
        } else {
            hideAdditionalHeaders();
        }
    }

    public void setCryptoAnnotation(final CryptoResultAnnotation cryptoAnnotation) throws MessagingException {
        this.cryptoAnnotation = cryptoAnnotation;
        invalidate();
        initializeEncryptionHeader();
        initializeSignatureHeader();
    }

    private void initializeEncryptionHeader() {
        if (noCryptoAnnotationFound()) {
            setEncryptionImageAndTextColor(CryptoState.NOT_ENCRYPTED);
            return;
        }

        switch (cryptoAnnotation.getErrorType()) {
            case NONE: {
                if (cryptoAnnotation.isEncrypted()) {
                    setEncryptionImageAndTextColor(CryptoState.ENCRYPTED);
                } else {
                    setEncryptionImageAndTextColor(CryptoState.NOT_ENCRYPTED);
                }
                break;
            }
            case CRYPTO_API_RETURNED_ERROR: {
                setEncryptionImageAndTextColor(CryptoState.INVALID);
                break;
            }
            case ENCRYPTED_BUT_INCOMPLETE: {
                setEncryptionImageAndTextColor(CryptoState.UNAVAILABLE);
                break;
            }
            case SIGNED_BUT_INCOMPLETE: {
                setEncryptionImageAndTextColor(CryptoState.NOT_ENCRYPTED);
                break;
            }
        }
    }

    private void initializeSignatureHeader() {
        if (noCryptoAnnotationFound()) {
            setSignatureImageAndTextColor(CryptoState.NOT_SIGNED);
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
                setSignatureImageAndTextColor(CryptoState.UNAVAILABLE);
                break;
            }
        }
    }

    private void displayVerificationResult() {
        SignatureResult signatureResult = cryptoAnnotation.getSignatureResult();

        switch (signatureResult.getStatus()) {
            case UNSIGNED: {
                setSignatureImageAndTextColor(CryptoState.NOT_SIGNED);
                break;
            }
            case INVALID_SIGNATURE: {
                setSignatureImageAndTextColor(CryptoState.INVALID);
                break;
            }
            case SUCCESS: {
                setSignatureImageAndTextColor(CryptoState.VERIFIED);
                break;
            }
            case KEY_MISSING: {
                setSignatureImageAndTextColor(CryptoState.UNKNOWN_KEY);
                break;
            }
            case SUCCESS_UNCERTIFIED: {
                setSignatureImageAndTextColor(CryptoState.UNVERIFIED);
                break;
            }
            case KEY_EXPIRED: {
                setSignatureImageAndTextColor(CryptoState.EXPIRED);
                break;
            }
            case KEY_REVOKED: {
                setSignatureImageAndTextColor(CryptoState.REVOKED);
                break;
            }
            case ERROR: {
                setSignatureImageAndTextColor(CryptoState.INVALID);
                break;
            }
            default:
                throw new RuntimeException("OpenPgpSignatureResult result not handled!");
        }
    }

    private void setEncryptionImageAndTextColor(final CryptoState state) {
        setStatusImageColor(mEncryptionIcon, state);
    }

    private void setSignatureImageAndTextColor(final CryptoState state) {
        setStatusImageColor(mSignatureIcon, state);
    }

    private void setStatusImageColor(final ImageView statusIcon, final CryptoState state) {
        final Drawable statusImageDrawable = getContext().getResources().getDrawable(state.getDrawableId());
        statusIcon.setImageDrawable(statusImageDrawable);
        final int color = getContext().getResources().getColor(state.getColorId());
        statusIcon.setColorFilter(color, PorterDuff.Mode.SRC_IN);
    }

    private boolean noCryptoAnnotationFound() {
        return cryptoAnnotation == null;
    }

    public void onShowAdditionalHeaders() {
        int currentVisibility = mAdditionalHeadersView.getVisibility();
        if (currentVisibility == View.VISIBLE) {
            hideAdditionalHeaders();
            expand(mToView, false);
            expand(mCcView, false);
        } else {
            showAdditionalHeaders();
            expand(mToView, true);
            expand(mCcView, true);
        }

        layoutChanged();
    }

    private void updateAddressField(TextView v, CharSequence text, View label) {
        boolean hasText = !TextUtils.isEmpty(text);

        v.setText(text);
        v.setVisibility(hasText ? View.VISIBLE : View.GONE);
        label.setVisibility(hasText ? View.VISIBLE : View.GONE);
    }

    /**
     * Expand or collapse a TextView by removing or adding the 2 lines limitation
     */
    private void expand(TextView v, boolean expand) {
        if (expand) {
            v.setMaxLines(Integer.MAX_VALUE);
            v.setEllipsize(null);
        } else {
            v.setMaxLines(2);
            v.setEllipsize(android.text.TextUtils.TruncateAt.END);
        }
    }

    private List<HeaderEntry> getAdditionalHeaders(final Message message)
            throws MessagingException {
        List<HeaderEntry> additionalHeaders = new LinkedList<HeaderEntry>();

        Set<String> headerNames = new LinkedHashSet<String>(message.getHeaderNames());
        for (String headerName : headerNames) {
            String[] headerValues = message.getHeader(headerName);
            for (String headerValue : headerValues) {
                additionalHeaders.add(new HeaderEntry(headerName, headerValue));
            }
        }

        return additionalHeaders;
    }

    /**
     * Set up the additional headers text view with the supplied header data.
     *
     * @param additionalHeaders List of header entries. Each entry consists of a header
     *                          name and a header value. Header names may appear multiple
     *                          times.
     *                          <p>
     *                          This method is always called from within the UI thread by
     *                          {@link #showAdditionalHeaders()}.
     */
    private void populateAdditionalHeadersView(final List<HeaderEntry> additionalHeaders) {
        SpannableStringBuilder sb = new SpannableStringBuilder();
        boolean first = true;
        for (HeaderEntry additionalHeader : additionalHeaders) {
            if (!first) {
                sb.append("\n");
            } else {
                first = false;
            }

            StyleSpan boldSpan = new StyleSpan(Typeface.BOLD);
            SpannableString label = new SpannableString(additionalHeader.label + ": ");
            label.setSpan(boldSpan, 0, label.length(), 0);
            sb.append(label);
            sb.append(MimeUtility.unfoldAndDecode(additionalHeader.value));
        }

        mAdditionalHeadersView.setText(sb);
    }

    @Override
    public Parcelable onSaveInstanceState() {
        Parcelable superState = super.onSaveInstanceState();
        SavedState savedState = new SavedState(superState);
        savedState.additionalHeadersVisible = additionalHeadersVisible();
        return savedState;
    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {
        if (!(state instanceof SavedState)) {
            super.onRestoreInstanceState(state);
            return;
        }

        SavedState savedState = (SavedState) state;
        super.onRestoreInstanceState(savedState.getSuperState());

        mSavedState = savedState;
    }

    static class SavedState extends BaseSavedState {
        boolean additionalHeadersVisible;

        public static final Parcelable.Creator<SavedState> CREATOR =
                new Parcelable.Creator<SavedState>() {
                    @Override
                    public SavedState createFromParcel(Parcel in) {
                        return new SavedState(in);
                    }

                    @Override
                    public SavedState[] newArray(int size) {
                        return new SavedState[size];
                    }
                };


        SavedState(Parcelable superState) {
            super(superState);
        }

        private SavedState(Parcel in) {
            super(in);
            this.additionalHeadersVisible = (in.readInt() != 0);
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeInt((this.additionalHeadersVisible) ? 1 : 0);
        }
    }

    public interface OnLayoutChangedListener {
        void onLayoutChanged();
    }

    public void setOnLayoutChangedListener(OnLayoutChangedListener listener) {
        mOnLayoutChangedListener = listener;
    }

    private void layoutChanged() {
        if (mOnLayoutChangedListener != null) {
            mOnLayoutChangedListener.onLayoutChanged();
        }
    }

    public void hideSubjectLine() {
        mSubjectView.setVisibility(GONE);
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
