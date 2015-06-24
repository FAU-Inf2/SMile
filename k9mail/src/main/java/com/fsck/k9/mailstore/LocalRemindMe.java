package com.fsck.k9.mailstore;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.fsck.k9.Account;
import com.fsck.k9.K9;
import com.fsck.k9.mail.RemindMe;
import com.fsck.k9.mail.Message;
import com.fsck.k9.mail.MessagingException;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class LocalRemindMe {
    private static final String TABLE_FOLLOWUP = "RemindMe";
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
    private Account mAccount;

    public LocalRemindMe(LocalStore localStore) {
        this.localStore = localStore;
        this.mAccount = localStore.getAccount();
    }

    private RemindMe populateFromCursor(Cursor cursor) throws MessagingException {
        RemindMe remindMe = new RemindMe();
        remindMe.setId(cursor.getInt(cursor.getColumnIndex(COLUMN_FOLLOWUP_ID)));
        remindMe.setRemindTime(new Date(cursor.getLong(cursor.getColumnIndex(COLUMN_FOLLOWUP_REMINDTIME))));
        int folderId = cursor.getInt(cursor.getColumnIndex(COLUMN_FOLLOWUP_FOLDERID));
        long messageId = cursor.getLong(cursor.getColumnIndex(COLUMN_FOLLOWUP_MESSAGEID));

        String messageUid;
        Message message = null;

        Log.d(K9.LOG_TAG, "RemindMe.populateFromCursor, folderId: " + folderId + " messageId: " + messageId);
        LocalFolder folder = this.localStore.getFolderById(folderId);
        messageUid = folder.getMessageUidById(messageId);

        if(messageUid != null) {
            message = folder.getMessage(messageUid);
            remindMe.setReference(message);
            remindMe.setTitle(message.getSubject());
        }

        remindMe.setFolderId(folderId);
        return remindMe;
    }

    public List<RemindMe> getAllFollowUps() throws MessagingException {
        return this.localStore.database.execute(false, new LockableDatabase.DbCallback<List<RemindMe>>() {
            @Override
            public List<RemindMe> doDbWork(SQLiteDatabase db) throws LockableDatabase.WrappedException, MessagingException {
                ArrayList<RemindMe> remindMes = new ArrayList<RemindMe>();
                Cursor cursor = db.query(TABLE_FOLLOWUP, allColumns, null, null, null, null, null);

                while (cursor.moveToNext()) {
                    remindMes.add(populateFromCursor(cursor));
                }
                cursor.close();

                return remindMes;
            }
        });
    }

    public void add(final RemindMe remindMe) throws MessagingException {
        this.localStore.getDatabase().execute(true, new LockableDatabase.DbCallback<Void>() {
            @Override
            public Void doDbWork(SQLiteDatabase db) throws LockableDatabase.WrappedException, MessagingException {
                ContentValues cv = new ContentValues();
                cv.put(COLUMN_FOLLOWUP_MESSAGEID, remindMe.getReference().getId());
                cv.put(COLUMN_FOLLOWUP_FOLDERID, remindMe.getFolderId());
                cv.put(COLUMN_FOLLOWUP_REMINDTIME, remindMe.getRemindTime().getTime());
                db.insert(LocalRemindMe.TABLE_FOLLOWUP, null, cv);
                return null;
            }
        });
    }

    public void delete(final RemindMe remindMe) throws MessagingException {
        this.localStore.getDatabase().execute(true, new LockableDatabase.DbCallback<Void>() {
            @Override
            public Void doDbWork(SQLiteDatabase db) throws LockableDatabase.WrappedException, MessagingException {
                db.delete(LocalRemindMe.TABLE_FOLLOWUP, COLUMN_FOLLOWUP_ID + "=?", new String[]{Long.toString(remindMe.getId())});
                return null;
            }
        });
    }

    public void update(final RemindMe remindMe) throws MessagingException {

        this.localStore.getDatabase().execute(true, new LockableDatabase.DbCallback<Void>() {
            @Override
            public Void doDbWork(SQLiteDatabase db) throws LockableDatabase.WrappedException, MessagingException {
                ContentValues cv = new ContentValues();
                cv.put(COLUMN_FOLLOWUP_MESSAGEID, remindMe.getReference().getId());
                cv.put(COLUMN_FOLLOWUP_FOLDERID, remindMe.getFolderId());
                cv.put(COLUMN_FOLLOWUP_REMINDTIME, remindMe.getRemindTime().getTime());
                db.update(LocalRemindMe.TABLE_FOLLOWUP, cv, "id=?", new String[] {Long.toString(remindMe.getId())});
                return null;
            }
        });
    }

}
