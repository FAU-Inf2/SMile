package com.fsck.k9.ui.messageview;

import android.app.LoaderManager;
import android.content.Context;
import android.content.Loader;
import android.os.Bundle;

import com.fsck.k9.mailstore.LocalMessage;
import com.fsck.k9.mailstore.MessageViewInfo;
import com.fsck.k9.mailstore.CryptoResultAnnotation;
import com.fsck.k9.ui.crypto.MessageCryptoAnnotations;
import com.fsck.k9.ui.message.DecodeMessageLoader;

class DecodeMessageLoaderCallback implements LoaderManager.LoaderCallbacks<MessageViewInfo> {
    private final Context context;
    private final MessageViewHandler handler;

    public DecodeMessageLoaderCallback(final Context context, final MessageViewHandler handler) {
        this.context = context;
        this.handler = handler;
    }

    @Override
    public Loader<MessageViewInfo> onCreateLoader(int id, Bundle args) {
        final LocalMessage message = (LocalMessage) args.getSerializable(MessageViewFragment.ARG_MESSAGE);
        final MessageCryptoAnnotations<CryptoResultAnnotation> annotations = (MessageCryptoAnnotations<CryptoResultAnnotation>) args.getSerializable(MessageViewFragment.ARG_ANNOTATIONS);
        handler.setProgress(true);
        return new DecodeMessageLoader(context, message, annotations); //FIXME: messageAnnotations are not parcelable :-(
    }

    @Override
    public void onLoadFinished(Loader<MessageViewInfo> loader, MessageViewInfo messageContainer) {
        handler.setProgress(false);
        handler.onDecodeMessageFinished(messageContainer);
    }

    @Override
    public void onLoaderReset(Loader<MessageViewInfo> loader) {
        // Do nothing
    }
}
