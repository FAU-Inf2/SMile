package com.fsck.k9.ui.messageview;

import android.content.Context;

import com.fsck.k9.controller.MessagingController;
import com.fsck.k9.helper.FileBrowserHelper;
import com.fsck.k9.mailstore.AttachmentViewInfo;

import java.lang.ref.WeakReference;

class AttachmentCallback implements AttachmentViewCallback {
    private final Context context;
    private final MessagingController controller;
    private final MessageViewHandler handler;
    private final WeakReference<MessageViewFragment> messageViewFragmentWeakReference;

    public AttachmentCallback(final Context context, final MessagingController controller,
                              final MessageViewHandler handler,
                              final MessageViewFragment messageViewFragment) {
        this.context = context;
        this.controller = controller;
        this.handler = handler;
        this.messageViewFragmentWeakReference = new WeakReference<MessageViewFragment>(messageViewFragment);
    }

    private AttachmentController getAttachmentController(AttachmentViewInfo attachment) {
        return new AttachmentController(context, controller, attachment, handler);
    }

    @Override
    public void onViewAttachment(AttachmentViewInfo attachment) {
        //TODO: check if we have to download the attachment first

        getAttachmentController(attachment).viewAttachment();
    }

    @Override
    public void onSaveAttachment(AttachmentViewInfo attachment) {
        //TODO: check if we have to download the attachment first

        getAttachmentController(attachment).saveAttachment();
    }

    @Override
    public void onSaveAttachmentToUserProvidedDirectory(final AttachmentViewInfo attachment) {
        //TODO: check if we have to download the attachment first
        MessageViewFragment fragment = messageViewFragmentWeakReference.get();
        if (fragment != null) {
            FileBrowserHelper.getInstance().showFileBrowserActivity(fragment, null,
                    MessageViewFragment.ACTIVITY_CHOOSE_DIRECTORY, new FileBrowserHelper.FileBrowserFailOverCallback() {
                        @Override
                        public void onPathEntered(String path) {
                            getAttachmentController(attachment).saveAttachmentTo(path);
                        }

                        @Override
                        public void onCancel() {
                            // Do nothing
                        }
                    });
        }
    }

}
