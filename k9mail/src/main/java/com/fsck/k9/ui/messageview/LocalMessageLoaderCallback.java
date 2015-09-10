package com.fsck.k9.ui.messageview;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;

import com.fsck.k9.Account;
import com.fsck.k9.activity.MessageReference;
import com.fsck.k9.mailstore.LocalMessage;
import com.fsck.k9.ui.message.LocalMessageLoader;

class LocalMessageLoaderCallback implements LoaderManager.LoaderCallbacks<LocalMessage> {
    private final MessageViewHandler handler;
    private final Context context;
    private final Account account;
    private final MessageReference messageReference;

    public LocalMessageLoaderCallback(MessageViewHandler handler, Context context, Account account, MessageReference messageReference) {
        this.handler = handler;
        this.context = context;
        this.account = account;
        this.messageReference = messageReference;
    }

    @Override
    public Loader<LocalMessage> onCreateLoader(int id, Bundle args) {
        handler.setProgress(true);
        return new LocalMessageLoader(context, account, messageReference);
    }

    @Override
    public void onLoadFinished(Loader<LocalMessage> loader, LocalMessage message) {
        handler.setProgress(false);

        if (message == null) {
            handler.onLoadMessageFromDatabaseFailed();
        } else {
            handler.onLoadMessageFromDatabaseFinished(message);
        }
    }

    @Override
    public void onLoaderReset(Loader<LocalMessage> loader) {
        // Do nothing
    }
}
