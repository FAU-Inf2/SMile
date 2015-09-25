package com.fsck.k9.activity.misc;

import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.fsck.k9.activity.MessageCompose;
import com.fsck.k9.asynctask.AttachmentInfoLoader;

import de.fau.cs.mad.smile.android.R;

public class AttachmentInfoLoaderCallback implements LoaderManager.LoaderCallbacks<Attachment> {
    private MessageCompose messageCompose;
    LoaderManager.LoaderCallbacks<Attachment> mAttachmentContentLoaderCallback;

    public AttachmentInfoLoaderCallback(MessageCompose messageCompose, LoaderManager.LoaderCallbacks<Attachment> mAttachmentContentLoaderCallback) {
        this.messageCompose = messageCompose;
        this.mAttachmentContentLoaderCallback = mAttachmentContentLoaderCallback;
    }

    @Override
    public Loader<Attachment> onCreateLoader(int id, Bundle args) {
        messageCompose.onFetchAttachmentStarted();
        Attachment attachment = args.getParcelable(MessageCompose.LOADER_ARG_ATTACHMENT);
        return new AttachmentInfoLoader(messageCompose, attachment);
    }

    @Override
    public void onLoadFinished(Loader<Attachment> loader, Attachment attachment) {
        int loaderId = loader.getId();

        View view = AttachmentMessageComposeHelper.getAttachmentView(messageCompose.getmAttachments(), loaderId);
        if (view != null) {
            view.setTag(attachment);

            TextView nameView = (TextView) view.findViewById(R.id.attachment_name);
            nameView.setText(attachment.name);

            attachment.loaderId = messageCompose.increaseAndReturnMaxLoadId();
            AttachmentMessageComposeHelper.initAttachmentContentLoader(messageCompose, mAttachmentContentLoaderCallback, attachment);
        } else {
            messageCompose.onFetchAttachmentFinished();
        }

        messageCompose.getLoaderManager().destroyLoader(loaderId);
    }

    @Override
    public void onLoaderReset(Loader<Attachment> loader) {
        messageCompose.onFetchAttachmentFinished();
    }
}
