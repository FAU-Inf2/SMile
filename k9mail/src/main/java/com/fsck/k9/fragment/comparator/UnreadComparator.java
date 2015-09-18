package com.fsck.k9.fragment.comparator;

import android.database.Cursor;

import com.fsck.k9.fragment.MessageListFragment;

import java.util.Comparator;

public class UnreadComparator implements Comparator<Cursor> {

    @Override
    public int compare(Cursor cursor1, Cursor cursor2) {
        int o1IsUnread = cursor1.getInt(MessageListFragment.READ_COLUMN);
        int o2IsUnread = cursor2.getInt(MessageListFragment.READ_COLUMN);
        return o1IsUnread - o2IsUnread;
    }
}
