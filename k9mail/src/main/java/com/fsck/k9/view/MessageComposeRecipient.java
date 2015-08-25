package com.fsck.k9.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.text.InputType;
import android.text.TextWatcher;
import android.text.util.Rfc822Tokenizer;
import android.util.AttributeSet;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.MultiAutoCompleteTextView;

import com.fsck.k9.EmailAddressAdapter;
import com.fsck.k9.EmailAddressValidator;
import com.fsck.k9.mail.Address;

import java.util.List;

import de.fau.cs.mad.smile.android.R;

public class MessageComposeRecipient extends LinearLayout {
    private MultiAutoCompleteTextView mRecipient;
    private ImageButton mAddButton;

    public MessageComposeRecipient(final Context context, final AttributeSet attributeSet) {
        super(context, attributeSet);
        initializeUIElements(context, attributeSet);
        configureUIElements(context);
    }

    private void initializeUIElements(Context context, AttributeSet attributeSet) {
        TypedArray typedArray = context.obtainStyledAttributes(attributeSet, R.styleable.MessageComposeRecipient);
        mRecipient = new MultiAutoCompleteTextView(context);
        mRecipient.setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        mRecipient.setImeOptions(EditorInfo.IME_ACTION_NEXT);

        CharSequence hint = typedArray.getString(R.styleable.MessageComposeRecipient_recipientHint);
        if(hint != null) {
            mRecipient.setHint(hint);
        }

        mRecipient.setTextAppearance(context, android.R.style.TextAppearance_Medium);
        LayoutParams recipientLayoutParams = new LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT);
        recipientLayoutParams.rightMargin = getPixelsFromDp(6);
        recipientLayoutParams.weight = 1;
        addView(mRecipient, recipientLayoutParams);

        mAddButton = new ImageButton(context);

        TypedArray k9Styles = context.obtainStyledAttributes(attributeSet, R.styleable.K9Styles);
        int addImage = k9Styles.getResourceId(R.styleable.K9Styles_messageComposeAddContactImage, -1);
        if(addImage > 0) {
            mAddButton.setImageResource(addImage);
        }

        CharSequence addDescription = typedArray.getString(R.styleable.MessageComposeRecipient_addDescription);
        if(addDescription != null) {
            mAddButton.setContentDescription(addDescription);
        }

        LayoutParams addButtonLayoutParams = new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        addButtonLayoutParams.topMargin = getPixelsFromDp(1);
        final int padding = getPixelsFromDp(8);
        mAddButton.setPadding(padding, padding, padding, padding);
        addView(mAddButton, addButtonLayoutParams);
    }

    private void configureUIElements(Context context) {
        mRecipient.setAdapter(new EmailAddressAdapter(context));
        mRecipient.setTokenizer(new Rfc822Tokenizer());
        mRecipient.setValidator(new EmailAddressValidator());
        mAddButton.setVisibility(GONE);
    }

    private int getPixelsFromDp(int dp) {
        float scale = getResources().getDisplayMetrics().density;
        int dpAsPixels = (int) (dp*scale + 0.5f);
        return dpAsPixels;
    }

    public void addTextChangedListener(TextWatcher watcher) {
        mRecipient.addTextChangedListener(watcher);
    }

    public Address[] getRecipients() {
        return Address.parseUnencoded(mRecipient.getText().toString().trim());
    }

    public boolean addRecipients(List<String> recipients) {
        if (recipients == null || recipients.isEmpty()) {
            return false;
        }

        StringBuilder addressList = new StringBuilder();

        // Read current contents of the TextView
        String text = mRecipient.getText().toString();
        addressList.append(text);

        // Add comma if necessary
        if (text.length() != 0 && !(text.endsWith(", ") || text.endsWith(","))) {
            addressList.append(", ");
        }

        // Add recipients
        for (String recipient : recipients) {
            addressList.append(recipient);
            addressList.append(", ");
        }

        mRecipient.setText(addressList);

        return true;
    }

    public void addRecipients(Address[] recipients) {
        if (recipients == null) {
            return;
        }
        for (Address address : recipients) {
            addRecipient(address);
        }
    }

    public void addRecipient(Address recipient) {
        mRecipient.append(recipient + ", ");
    }

    public void clearRecipients() {
        mRecipient.setText("");
    }

    public void setError(CharSequence error) {
        mRecipient.setError(error);
    }

    public void setAddContactListener(OnClickListener onClickListener) {
        mAddButton.setOnClickListener(onClickListener);
        mAddButton.setVisibility(VISIBLE);
    }
}
