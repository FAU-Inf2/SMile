package com.fsck.k9.fragment.comparator;

import android.database.Cursor;

import com.fsck.k9.fragment.MessageListFragment;
import com.fsck.k9.mail.Address;
import com.fsck.k9.mailstore.LocalMessage;
import com.fsck.k9.provider.EmailProvider;
import com.fsck.k9.provider.EmailProvider.MessageColumns;

import java.util.Comparator;

public class SenderComparator implements Comparator<Cursor> {

    @Override
    public int compare(Cursor cursor1, Cursor cursor2) {
        String sender1 = getSenderAddressFromCursor(cursor1);
        String sender2 = getSenderAddressFromCursor(cursor2);

        if (sender1 == null && sender2 == null) {
            return 0;
        } else if (sender1 == null) {
            return 1;
        } else if (sender2 == null) {
            return -1;
        } else {
            return sender1.compareToIgnoreCase(sender2);
        }
    }

    private String getSenderAddressFromCursor(Cursor cursor) {
        String fromList = cursor.getString(cursor.getColumnIndex(MessageColumns.SENDER_LIST));
        Address[] fromAddrs = Address.unpack(fromList);
        return (fromAddrs.length > 0) ? fromAddrs[0].getAddress() : null;
    }
}
