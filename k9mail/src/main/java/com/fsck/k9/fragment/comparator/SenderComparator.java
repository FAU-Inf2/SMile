package com.fsck.k9.fragment.comparator;

import android.database.Cursor;

import com.fsck.k9.fragment.MessageListFragment;

import java.util.Comparator;

public class SenderComparator implements Comparator<Cursor> {

    @Override
    public int compare(Cursor cursor1, Cursor cursor2) {
        String sender1 = MessageListFragment.getSenderAddressFromCursor(cursor1);
        String sender2 = MessageListFragment.getSenderAddressFromCursor(cursor2);

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
}
