package com.fsck.k9.ui.message;


import android.content.Context;
import android.support.v4.content.AsyncTaskLoader;
import android.util.Log;

import com.fsck.k9.K9;
import com.fsck.k9.mail.Message;
import com.fsck.k9.mailstore.LocalMessage;
import com.fsck.k9.mailstore.LocalMessageExtractor;
import com.fsck.k9.mailstore.MessageViewInfo;
import com.fsck.k9.mailstore.CryptoResultAnnotation;
import com.fsck.k9.ui.crypto.MessageCryptoAnnotations;


public class DecodeMessageLoader extends AsyncTaskLoader<MessageViewInfo> {
    private final LocalMessage message;
    private final MessageCryptoAnnotations annotations;
    private MessageViewInfo messageViewInfo;

    public DecodeMessageLoader(Context context, LocalMessage message, MessageCryptoAnnotations annotations) {
        super(context);
        this.message = message;
        this.annotations = annotations;
    }

    @Override
    protected void onStartLoading() {
        if (messageViewInfo != null) {
            super.deliverResult(messageViewInfo);
        }

        if (takeContentChanged() || messageViewInfo == null) {
            forceLoad();
        }
    }

    @Override
    public void deliverResult(MessageViewInfo messageViewInfo) {
        this.messageViewInfo = messageViewInfo;
        super.deliverResult(messageViewInfo);
    }

    @Override
    public MessageViewInfo loadInBackground() {
        try {
            return LocalMessageExtractor.decodeMessageForView(getContext(), message, annotations);
        } catch (Exception e) {
            Log.e(K9.LOG_TAG, "Error while decoding message", e);
            return null;
        }
    }

    @Override
    protected void onReset() {
        super.onReset();
        this.messageViewInfo = null;
    }
}
