package com.fsck.k9.ui.messageview;

import android.os.Handler;

import com.fsck.k9.mailstore.AttachmentViewInfo;
import com.fsck.k9.mailstore.LocalMessage;
import com.fsck.k9.mailstore.MessageViewInfo;

import java.lang.ref.WeakReference;

import de.fau.cs.mad.smile.android.R;


class MessageViewHandler extends Handler {
    private final WeakReference<MessageViewFragment> messageViewFragmentWeakReference;

    public MessageViewHandler(final MessageViewFragment messageViewFragment) {
        messageViewFragmentWeakReference = new WeakReference<MessageViewFragment>(messageViewFragment);
    }

    public void hideAttachmentLoadingDialogOnMainThread() {
        this.post(new Runnable() {
            @Override
            public void run() {
                MessageViewFragment fragment = messageViewFragmentWeakReference.get();
                if (fragment != null) {
                    fragment.removeDialog(R.id.dialog_attachment_progress);
                }
            }
        });
    }

    public void showAttachmentLoadingDialog() {
        this.post(new Runnable() {
            @Override
            public void run() {
                MessageViewFragment fragment = messageViewFragmentWeakReference.get();
                if (fragment != null) {
                    fragment.showDialog(R.id.dialog_attachment_progress);
                }
            }
        });
    }

    public void loadMessageFinished(final LocalMessage message) {
        this.post(new Runnable() {
            @Override
            public void run() {
                MessageViewFragment fragment = messageViewFragmentWeakReference.get();
                if (fragment != null) {
                    fragment.onMessageDownloadFinished(message);
                }
            }
        });
    }

    public void loadMessageFailed(final Throwable t) {
        this.post(new Runnable() {
            @Override
            public void run() {
                MessageViewFragment fragment = messageViewFragmentWeakReference.get();
                if (fragment != null) {
                    fragment.onDownloadMessageFailed(t);
                }
            }
        });
    }

    // FIXME: remove them?
    public void disableAttachmentButtons(AttachmentViewInfo attachment) {
        // mMessageView.disableAttachmentButtons(attachment);
    }

    public void enableAttachmentButtons(AttachmentViewInfo attachment) {
        // mMessageView.enableAttachmentButtons(attachment);
    }

    public void refreshAttachmentThumbnail(AttachmentViewInfo attachment) {
        // mMessageView.refreshAttachmentThumbnail(attachment);
    }

    public void onDecodeMessageFinished(final MessageViewInfo messageContainer) {
        this.post(new Runnable() {
            @Override
            public void run() {
                MessageViewFragment fragment = messageViewFragmentWeakReference.get();
                if (fragment != null) {
                    fragment.showMessage(messageContainer);
                }
            }
        });
    }

    public void setProgress(final Boolean enable) {
        this.post(new Runnable() {
            @Override
            public void run() {
                MessageViewFragment fragment = messageViewFragmentWeakReference.get();
                if (fragment != null) {
                    fragment.setProgress(enable);
                }
            }
        });

    }
}
