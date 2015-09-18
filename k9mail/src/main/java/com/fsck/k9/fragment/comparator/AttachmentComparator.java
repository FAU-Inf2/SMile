package com.fsck.k9.fragment.comparator;

import android.database.Cursor;

import com.fsck.k9.fragment.MessageListFragment;

import java.util.Comparator;

public class AttachmentComparator implements Comparator<Cursor> {

    @Override
    public int compare(Cursor cursor1, Cursor cursor2) {
        int o1HasAttachment = (cursor1.getInt(MessageListFragment.ATTACHMENT_COUNT_COLUMN) > 0) ? 0 : 1;
        int o2HasAttachment = (cursor2.getInt(MessageListFragment.ATTACHMENT_COUNT_COLUMN) > 0) ? 0 : 1;
        return o1HasAttachment - o2HasAttachment;
    }
}
