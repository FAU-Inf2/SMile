package com.fsck.k9.activity;

import android.os.Handler;
import android.widget.Toast;

import java.lang.ref.WeakReference;

import de.fau.cs.mad.smile.android.R;

class MessageComposeHandler extends Handler {
    private final WeakReference<MessageCompose> reference;

    public MessageComposeHandler(final MessageCompose messageViewFragment) {
        reference = new WeakReference<MessageCompose>(messageViewFragment);
    }

    @Override
    public void handleMessage(android.os.Message msg) {
        MessageCompose compose = reference.get();
        if (compose == null) {
            return;
        }

        switch (msg.what) {
            case MessageCompose.MSG_PROGRESS_ON:
                compose.setProgressBarIndeterminateVisibility(true);
                break;
            case MessageCompose.MSG_PROGRESS_OFF:
                compose.setProgressBarIndeterminateVisibility(false);
                break;
            case MessageCompose.MSG_SKIPPED_ATTACHMENTS:
                Toast.makeText(
                        compose,
                        compose.getString(R.string.message_compose_attachments_skipped_toast),
                        Toast.LENGTH_LONG).show();
                break;
            case MessageCompose.MSG_SAVED_DRAFT:
                Toast.makeText(
                        compose,
                        compose.getString(R.string.message_saved_toast),
                        Toast.LENGTH_LONG).show();
                break;
            case MessageCompose.MSG_DISCARDED_DRAFT:
                Toast.makeText(
                        compose,
                        compose.getString(R.string.message_discarded_toast),
                        Toast.LENGTH_LONG).show();
                break;
            case MessageCompose.MSG_PERFORM_STALLED_ACTION:
                compose.performStalledAction();
                break;
            default:
                super.handleMessage(msg);
                break;
        }
    }
}
