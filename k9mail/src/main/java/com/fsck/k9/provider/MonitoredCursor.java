package com.fsck.k9.provider;

import android.content.ContentResolver;
import android.database.CharArrayBuffer;
import android.database.ContentObserver;
import android.database.CrossProcessCursor;
import android.database.CursorWindow;
import android.database.DataSetObserver;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import com.fsck.k9.K9;

import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Cursor wrapper that release a semaphore on close. Close is also triggered
 * on {@link #finalize()}.
 */
public class MonitoredCursor implements CrossProcessCursor {
    /**
     * The underlying cursor implementation that handles regular
     * requests
     */
    private CrossProcessCursor mCursor;

    /**
     * Whether {@link #close()} was invoked
     */
    private AtomicBoolean mClosed = new AtomicBoolean(false);

    private Semaphore mSemaphore;

    /**
     * @param cursor
     *            Never <code>null</code>.
     * @param semaphore
     *            The semaphore to release on close. Never
     *            <code>null</code>.
     */
    protected MonitoredCursor(final CrossProcessCursor cursor, final Semaphore semaphore) {
        this.mCursor = cursor;
        this.mSemaphore = semaphore;
    }

    /* (non-Javadoc)
     *
     * Close the underlying cursor and dereference it.
     *
     * @see android.database.Cursor#close()
     */
    @Override
    public void close() {
        if (mClosed.compareAndSet(false, true)) {
            mCursor.close();
            Log.d(K9.LOG_TAG, "Cursor closed, null'ing & releasing semaphore");
            mCursor = null;
            mSemaphore.release();
        }
    }

    @Override
    public boolean isClosed() {
        return mClosed.get() || mCursor.isClosed();
    }

    /* (non-Javadoc)
     *
     * Making sure cursor gets closed on garbage collection
     *
     * @see java.lang.Object#finalize()
     */
    @Override
    protected void finalize() throws Throwable {
        close();
        super.finalize();
    }

    protected void checkClosed() throws IllegalStateException {
        if (mClosed.get()) {
            throw new IllegalStateException("Cursor was closed");
        }
    }

    @Override
    public void fillWindow(int pos, CursorWindow winow) {
        checkClosed();
        mCursor.fillWindow(pos, winow);
    }

    @Override
    public CursorWindow getWindow() {
        checkClosed();
        return mCursor.getWindow();
    }

    @Override
    public boolean onMove(int oldPosition, int newPosition) {
        checkClosed();
        return mCursor.onMove(oldPosition, newPosition);
    }

    @Override
    public void copyStringToBuffer(int columnIndex, CharArrayBuffer buffer) {
        checkClosed();
        mCursor.copyStringToBuffer(columnIndex, buffer);
    }

    @Override
    public void deactivate() {
        checkClosed();
        mCursor.deactivate();
    }

    @Override
    public byte[] getBlob(int columnIndex) {
        checkClosed();
        return mCursor.getBlob(columnIndex);
    }

    @Override
    public int getColumnCount() {
        checkClosed();
        return mCursor.getColumnCount();
    }

    @Override
    public int getColumnIndex(String columnName) {
        checkClosed();
        return mCursor.getColumnIndex(columnName);
    }

    @Override
    public int getColumnIndexOrThrow(String columnName) throws IllegalArgumentException {
        checkClosed();
        return mCursor.getColumnIndexOrThrow(columnName);
    }

    @Override
    public String getColumnName(int columnIndex) {
        checkClosed();
        return mCursor.getColumnName(columnIndex);
    }

    @Override
    public String[] getColumnNames() {
        checkClosed();
        return mCursor.getColumnNames();
    }

    @Override
    public int getCount() {
        checkClosed();
        return mCursor.getCount();
    }

    @Override
    public double getDouble(int columnIndex) {
        checkClosed();
        return mCursor.getDouble(columnIndex);
    }

    @Override
    public Bundle getExtras() {
        checkClosed();
        return mCursor.getExtras();
    }

    @Override
    public float getFloat(int columnIndex) {
        checkClosed();
        return mCursor.getFloat(columnIndex);
    }

    @Override
    public int getInt(int columnIndex) {
        checkClosed();
        return mCursor.getInt(columnIndex);
    }

    @Override
    public long getLong(int columnIndex) {
        checkClosed();
        return mCursor.getLong(columnIndex);
    }

    @Override
    public int getPosition() {
        checkClosed();
        return mCursor.getPosition();
    }

    @Override
    public short getShort(int columnIndex) {
        checkClosed();
        return mCursor.getShort(columnIndex);
    }

    @Override
    public String getString(int columnIndex) {
        checkClosed();
        return mCursor.getString(columnIndex);
    }

    @Override
    public boolean getWantsAllOnMoveCalls() {
        checkClosed();
        return mCursor.getWantsAllOnMoveCalls();
    }

    @Override
    public void setExtras(Bundle extras) {
        checkClosed();
        mCursor.setExtras(extras);
    }

    @Override
    public boolean isAfterLast() {
        checkClosed();
        return mCursor.isAfterLast();
    }

    @Override
    public boolean isBeforeFirst() {
        checkClosed();
        return mCursor.isBeforeFirst();
    }

    @Override
    public boolean isFirst() {
        checkClosed();
        return mCursor.isFirst();
    }

    @Override
    public boolean isLast() {
        checkClosed();
        return mCursor.isLast();
    }

    @Override
    public boolean isNull(int columnIndex) {
        checkClosed();
        return mCursor.isNull(columnIndex);
    }

    @Override
    public boolean move(int offset) {
        checkClosed();
        return mCursor.move(offset);
    }

    @Override
    public boolean moveToFirst() {
        checkClosed();
        return mCursor.moveToFirst();
    }

    @Override
    public boolean moveToLast() {
        checkClosed();
        return mCursor.moveToLast();
    }

    @Override
    public boolean moveToNext() {
        checkClosed();
        return mCursor.moveToNext();
    }

    @Override
    public boolean moveToPosition(int position) {
        checkClosed();
        return mCursor.moveToPosition(position);
    }

    @Override
    public boolean moveToPrevious() {
        checkClosed();
        return mCursor.moveToPrevious();
    }

    @Override
    public void registerContentObserver(ContentObserver observer) {
        checkClosed();
        mCursor.registerContentObserver(observer);
    }

    @Override
    public void registerDataSetObserver(DataSetObserver observer) {
        checkClosed();
        mCursor.registerDataSetObserver(observer);
    }

    @SuppressWarnings("deprecation")
    @Override
    public boolean requery() {
        checkClosed();
        return mCursor.requery();
    }

    @Override
    public Bundle respond(Bundle extras) {
        checkClosed();
        return mCursor.respond(extras);
    }

    @Override
    public void setNotificationUri(ContentResolver cr, Uri uri) {
        checkClosed();
        mCursor.setNotificationUri(cr, uri);
    }

    @Override
    public void unregisterContentObserver(ContentObserver observer) {
        checkClosed();
        mCursor.unregisterContentObserver(observer);
    }

    @Override
    public void unregisterDataSetObserver(DataSetObserver observer) {
        checkClosed();
        mCursor.unregisterDataSetObserver(observer);
    }

    @Override
    public int getType(int columnIndex) {
        checkClosed();
        return mCursor.getType(columnIndex);
    }

    @Override
    public Uri getNotificationUri() {
        return null;
    }
}
