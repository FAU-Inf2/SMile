package com.fsck.k9.mailstore;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.fsck.k9.mail.FollowUp;
import com.fsck.k9.mail.Message;
import com.fsck.k9.mail.MessagingException;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class LocalFollowUp {
    private static final String TABLE_FOLLOWUP = "FollowUp";
    private static final String COLUMN_FOLLOWUP_ID = "Id";
    private static final String COLUMN_FOLLOWUP_MESSAGEID = "MessageId";
    private static final String COLUMN_FOLLOWUP_FOLDERID = "FolderId";
    private static final String COLUMN_FOLLOWUP_REMINDTIME = "RemindTime";
    private static final String[] allColumns = {
            COLUMN_FOLLOWUP_ID,
            COLUMN_FOLLOWUP_MESSAGEID,
            COLUMN_FOLLOWUP_FOLDERID,
            COLUMN_FOLLOWUP_REMINDTIME
    };

    private final LocalStore localStore;

    public LocalFollowUp(LocalStore localStore) {
        this.localStore = localStore;
    }

    private FollowUp populateFromCursor(Cursor cursor) throws MessagingException {
        FollowUp followUp = new FollowUp();
        followUp.setId(cursor.getInt(cursor.getColumnIndex(COLUMN_FOLLOWUP_ID)));
        followUp.setRemindTime(new Date(cursor.getLong(cursor.getColumnIndex(COLUMN_FOLLOWUP_REMINDTIME))));
        int folderId= cursor.getInt(cursor.getColumnIndex(COLUMN_FOLLOWUP_FOLDERID));
        String messageUid = this.localStore.getFolderById(folderId).getMessageUidById(cursor.getLong(cursor.getColumnIndex(COLUMN_FOLLOWUP_MESSAGEID)));
        Message m = this.localStore.getFolderById(folderId).getMessage(messageUid);
        followUp.setFolderId(folderId);
        followUp.setReference(m);
        followUp.setTitle(m.getSubject());
        return followUp;
    }

    public List<FollowUp> getAllFollowUps() throws MessagingException {
        return this.localStore.database.execute(false, new LockableDatabase.DbCallback<List<FollowUp>>() {
            @Override
            public List<FollowUp> doDbWork(SQLiteDatabase db) throws LockableDatabase.WrappedException, MessagingException {
                ArrayList<FollowUp> followUps = new ArrayList<FollowUp>();
                Cursor cursor = db.query(TABLE_FOLLOWUP, allColumns, null, null, null, null, null);

                while (cursor.moveToNext()) {
                    followUps.add(populateFromCursor(cursor));
                }
                cursor.close();

                return followUps;
            }
        });
    }

    public void add(final FollowUp followUp) throws MessagingException {
        this.localStore.getDatabase().execute(true, new LockableDatabase.DbCallback<Void>() {
            @Override
            public Void doDbWork(SQLiteDatabase db) throws LockableDatabase.WrappedException, MessagingException {
                ContentValues cv = new ContentValues();
                cv.put(COLUMN_FOLLOWUP_MESSAGEID, followUp.getReference().getId());
                cv.put(COLUMN_FOLLOWUP_FOLDERID, followUp.getFolderId());
                cv.put(COLUMN_FOLLOWUP_REMINDTIME, followUp.getRemindTime().getTime());
                db.insert(LocalFollowUp.TABLE_FOLLOWUP, null, cv);
                return null;
            }
        });
    }

    public void delete(final FollowUp followUp) throws MessagingException {
        this.localStore.getDatabase().execute(true, new LockableDatabase.DbCallback<Void>() {
            @Override
            public Void doDbWork(SQLiteDatabase db) throws LockableDatabase.WrappedException, MessagingException {
                db.delete(LocalFollowUp.TABLE_FOLLOWUP, COLUMN_FOLLOWUP_ID + "=?", new String[]{Long.toString(followUp.getId())});
                return null;
            }
        });
    }

    public void update(final FollowUp followUp) throws MessagingException {

        this.localStore.getDatabase().execute(true, new LockableDatabase.DbCallback<Void>() {
            @Override
            public Void doDbWork(SQLiteDatabase db) throws LockableDatabase.WrappedException, MessagingException {
                ContentValues cv = new ContentValues();
                cv.put(COLUMN_FOLLOWUP_MESSAGEID, followUp.getReference().getId());
                cv.put(COLUMN_FOLLOWUP_FOLDERID, followUp.getFolderId());
                cv.put(COLUMN_FOLLOWUP_REMINDTIME, followUp.getRemindTime().getTime());
                db.update(LocalFollowUp.TABLE_FOLLOWUP, cv, "id=?", new String[] {Long.toString(followUp.getId())});
                return null;
            }
        });
    }

}
