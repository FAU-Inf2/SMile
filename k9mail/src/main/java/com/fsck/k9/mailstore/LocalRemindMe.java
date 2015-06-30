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
    private static final String TABLE_REMINDME = "RemindMe";
    private static final String COLUMN_REMINDME_ID = "Id";
    private static final String COLUMN_REMINDME_MESSAGEID = "MessageId";
    private static final String COLUMN_REMINDME_FOLDERID = "FolderId";
    private static final String COLUMN_REMINDME_REMINDTIME = "RemindTime";
    private static final String COLUMN_REMINDME_LASTMODIFIED = "LastModified";
    private static final String COLUMN_REMINDME_SEEN = "Seen";

    private static final String[] allColumns = {
            COLUMN_REMINDME_ID,
            COLUMN_REMINDME_MESSAGEID,
            COLUMN_REMINDME_FOLDERID,
            COLUMN_REMINDME_REMINDTIME,
            COLUMN_REMINDME_LASTMODIFIED,
            COLUMN_REMINDME_SEEN
    };

    private final LocalStore localStore;
    private Account mAccount;

    public LocalRemindMe(LocalStore localStore) {
        this.localStore = localStore;
        this.mAccount = localStore.getAccount();
    }

    private RemindMe populateFromCursor(Cursor cursor) throws MessagingException {
        RemindMe remindMe = new RemindMe();
        remindMe.setId(cursor.getInt(cursor.getColumnIndex(COLUMN_REMINDME_ID)));
        remindMe.setRemindTime(new Date(cursor.getLong(cursor.getColumnIndex(COLUMN_REMINDME_REMINDTIME))));
        remindMe.setLastModified(new Date(cursor.getLong(cursor.getColumnIndex(COLUMN_REMINDME_LASTMODIFIED))));

        if(cursor.isNull(cursor.getColumnIndex(COLUMN_REMINDME_SEEN))) {
            remindMe.setSeen(null);
        } else {
            remindMe.setSeen(new Date(cursor.getLong(cursor.getColumnIndex(COLUMN_REMINDME_SEEN))));
        }

        int folderId = cursor.getInt(cursor.getColumnIndex(COLUMN_REMINDME_FOLDERID));
        long messageId = cursor.getLong(cursor.getColumnIndex(COLUMN_REMINDME_MESSAGEID));

        String messageUid;
        Message message = null;

        Log.d(K9.LOG_TAG, "RemindMe.populateFromCursor, folderId: " + folderId + " messageId: " + messageId);
        //LocalFolder folder = this.localStore.getFolderById(folderId);
        //check inbox and RemindMe-folder -- folderId can be different
        LocalFolder folder = this.localStore.getFolder(mAccount.getInboxFolderName());
        messageUid = folder.getMessageUidById(messageId);

        if(messageUid == null) {
            folder = this.localStore.getFolder(mAccount.getRemindMeFolderName());
            messageUid = folder.getMessageUidById(messageId);
        }

        if(messageUid != null) {
            message = folder.getMessage(messageUid);
            remindMe.setReference(message);

            if (message != null) {
                remindMe.setTitle(message.getSubject());
            } else {
                Log.e(K9.LOG_TAG, "RemindMe.populateFromCursor, message was null.");
            }
        }

        remindMe.setFolderId(folderId);
        return remindMe;
    }

    public List<RemindMe> getAllRemindMes() throws MessagingException {
        return this.localStore.database.execute(false, new LockableDatabase.DbCallback<List<RemindMe>>() {
            @Override
            public List<RemindMe> doDbWork(SQLiteDatabase db) throws LockableDatabase.WrappedException, MessagingException {
                ArrayList<RemindMe> remindMes = new ArrayList<RemindMe>();
                Cursor cursor = db.query(TABLE_REMINDME, allColumns, null, null, null, null, null, null);

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
                cv.put(COLUMN_REMINDME_MESSAGEID, remindMe.getReference().getId());
                cv.put(COLUMN_REMINDME_FOLDERID, remindMe.getFolderId());
                cv.put(COLUMN_REMINDME_REMINDTIME, remindMe.getRemindTime().getTime());
                cv.put(COLUMN_REMINDME_LASTMODIFIED, System.currentTimeMillis());
                db.insert(LocalRemindMe.TABLE_REMINDME, null, cv);
                return null;
            }
        });
    }

    public void delete(final RemindMe remindMe) throws MessagingException {
        this.localStore.getDatabase().execute(true, new LockableDatabase.DbCallback<Void>() {
            @Override
            public Void doDbWork(SQLiteDatabase db) throws LockableDatabase.WrappedException, MessagingException {
                db.delete(LocalRemindMe.TABLE_REMINDME, COLUMN_REMINDME_ID + "=?", new String[]{Long.toString(remindMe.getId())});
                return null;
            }
        });
    }

    public void update(final RemindMe remindMe) throws MessagingException {

        this.localStore.getDatabase().execute(true, new LockableDatabase.DbCallback<Void>() {
            @Override
            public Void doDbWork(SQLiteDatabase db) throws LockableDatabase.WrappedException, MessagingException {
                ContentValues cv = new ContentValues();
                cv.put(COLUMN_REMINDME_MESSAGEID, remindMe.getReference().getId());
                cv.put(COLUMN_REMINDME_FOLDERID, remindMe.getFolderId());
                cv.put(COLUMN_REMINDME_REMINDTIME, remindMe.getRemindTime().getTime());
                cv.put(COLUMN_REMINDME_LASTMODIFIED, System.currentTimeMillis());
                cv.put(COLUMN_REMINDME_SEEN, remindMe.getSeen().getTime());
                db.update(LocalRemindMe.TABLE_REMINDME, cv, COLUMN_REMINDME_ID + "=?", new String[]{Long.toString(remindMe.getId())});
                return null;
            }
        });
    }

    public RemindMe getById(final int remindMeId) throws MessagingException {
        return this.localStore.getDatabase().execute(false, new LockableDatabase.DbCallback<RemindMe>() {
            @Override
            public RemindMe doDbWork(SQLiteDatabase db) throws LockableDatabase.WrappedException, MessagingException {
                RemindMe remindMe = null;
                Cursor cursor = db.query(TABLE_REMINDME, allColumns, COLUMN_REMINDME_ID + "=?", new String[]{Long.toString(remindMeId)}, null, null, null, null);

                if (cursor.moveToNext()) {
                    remindMe = populateFromCursor(cursor);
                }

                cursor.close();

                return remindMe;
            }
        });
    }
}
