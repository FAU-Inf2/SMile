package com.fsck.k9.view;

import android.content.Context;
import android.view.ContextMenu;
import android.view.MenuInflater;
import android.view.View;

import com.fsck.k9.Account;
import com.fsck.k9.controller.MessagingController;
import com.fsck.k9.mail.Flag;
import com.fsck.k9.mail.Message;
import com.fsck.k9.mailstore.LocalMessage;

import de.fau.cs.mad.smile.android.R;

public class MessageListItemContextMenu implements View.OnCreateContextMenuListener {
    @Override
    public void onCreateContextMenu(ContextMenu menu, View view, ContextMenu.ContextMenuInfo menuInfo) {
        final MessageListItemView itemView = (MessageListItemView)view;
        final Context context = view.getContext();
        final MessagingController controller = MessagingController.getInstance(context);
        final LocalMessage message = itemView.getMessage();

        MenuInflater menuInflater = new MenuInflater(context);
        menuInflater.inflate(R.menu.message_list_item_context, menu);

        //final long contextMenuUniqueId = message.getId();
        final Account account = message.getAccount();

        String subject = message.getSubject();
        boolean read = message.isSet(Flag.SEEN);
        boolean flagged = message.isSet(Flag.FLAGGED);

        menu.setHeaderTitle(subject);

        /*
        if (mSelected.contains(contextMenuUniqueId)) {
            menu.findItem(R.id.select).setVisible(false);
        } else {
            menu.findItem(R.id.deselect).setVisible(false);
        }*/

        if (read) {
            menu.findItem(R.id.mark_as_read).setVisible(false);
        } else {
            menu.findItem(R.id.mark_as_unread).setVisible(false);
        }

        if (flagged) {
            menu.findItem(R.id.flag).setVisible(false);
        } else {
            menu.findItem(R.id.unflag).setVisible(false);
        }

        if (!controller.isCopyCapable(account)) {
            menu.findItem(R.id.copy).setVisible(false);
        }

        if (!controller.isMoveCapable(account)) {
            menu.findItem(R.id.move).setVisible(false);
            menu.findItem(R.id.archive).setVisible(false);
            menu.findItem(R.id.spam).setVisible(false);
        }

        if (!account.hasArchiveFolder()) {
            menu.findItem(R.id.archive).setVisible(false);
        }

        if (!account.hasSpamFolder()) {
            menu.findItem(R.id.spam).setVisible(false);
        }
    }
}
