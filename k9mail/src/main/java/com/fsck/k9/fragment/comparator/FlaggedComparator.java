package com.fsck.k9.fragment.comparator;

import android.database.Cursor;

import com.fsck.k9.fragment.MessageListFragment;

import java.util.Comparator;

public class FlaggedComparator implements Comparator<Cursor> {

    @Override
    public int compare(Cursor cursor1, Cursor cursor2) {
        int o1IsFlagged = (cursor1.getInt(MessageListFragment.FLAGGED_COLUMN) == 1) ? 0 : 1;
        int o2IsFlagged = (cursor2.getInt(MessageListFragment.FLAGGED_COLUMN) == 1) ? 0 : 1;
        return o1IsFlagged - o2IsFlagged;
    }
}
