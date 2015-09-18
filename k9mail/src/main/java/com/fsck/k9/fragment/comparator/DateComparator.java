package com.fsck.k9.fragment.comparator;

import android.database.Cursor;

import com.fsck.k9.fragment.MessageListFragment;

import java.util.Comparator;

public class DateComparator implements Comparator<Cursor> {

    @Override
    public int compare(Cursor cursor1, Cursor cursor2) {
        long o1Date = cursor1.getLong(MessageListFragment.DATE_COLUMN);
        long o2Date = cursor2.getLong(MessageListFragment.DATE_COLUMN);
        if (o1Date < o2Date) {
            return -1;
        } else if (o1Date == o2Date) {
            return 0;
        } else {
            return 1;
        }
    }
}
