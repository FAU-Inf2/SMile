package com.fsck.k9.activity.misc;

import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.view.View;

import com.fsck.k9.activity.MessageCompose;
import com.fsck.k9.asynctask.AttachmentContentLoader;

import de.fau.cs.mad.smile.android.R;

public class AttachmentContentLoaderCallback implements LoaderManager.LoaderCallbacks<Attachment> {
    private MessageCompose messageCompose;

    public AttachmentContentLoaderCallback(MessageCompose messageCompose) {
        this.messageCompose = messageCompose;
    }

    @Override
    public Loader<Attachment> onCreateLoader(int id, Bundle args) {
        Attachment attachment = args.getParcelable(MessageCompose.LOADER_ARG_ATTACHMENT);
        return new AttachmentContentLoader(messageCompose, attachment);
    }

    @Override
    public void onLoadFinished(Loader<Attachment> loader, Attachment attachment) {
        int loaderId = loader.getId();

        View view = AttachmentMessageComposeHelper.getAttachmentView(messageCompose.getAttachments(), loaderId);
        if (view != null) {
            if (attachment.state == Attachment.LoadingState.COMPLETE) {
                view.setTag(attachment);

                View progressBar = view.findViewById(R.id.progressBar);
                progressBar.setVisibility(View.GONE);
            } else {
                messageCompose.getAttachments().removeView(view);
            }
        }

        messageCompose.onFetchAttachmentFinished();

        messageCompose.getLoaderManager().destroyLoader(loaderId);
    }

    @Override
    public void onLoaderReset(Loader<Attachment> loader) {
        messageCompose.onFetchAttachmentFinished();
    }

}
