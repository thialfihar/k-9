package com.fsck.k9.mail.store;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.SystemClock;
import android.util.Log;

public class LoggingSQLiteDatabase {
    private static final String LOG_TAG = "k9-sql";


    private SQLiteDatabase mDatabase;


    public LoggingSQLiteDatabase(SQLiteDatabase database) {
        mDatabase = database;
    }

    public int getVersion() {
        return mDatabase.getVersion();
    }

    public void close() {
        mDatabase.close();
    }

    public void beginTransaction() {
        mDatabase.beginTransaction();
    }

    public void setTransactionSuccessful() {
        mDatabase.setTransactionSuccessful();
    }

    public void endTransaction() {
        mDatabase.endTransaction();
    }

    public void setVersion(int version) {
        mDatabase.setVersion(version);
    }

    public void execSQL(String sql) {
        long start = SystemClock.elapsedRealtime();
        mDatabase.execSQL(sql);
        long end = SystemClock.elapsedRealtime();

        Log.d(LOG_TAG, "execSQL [" + (end - start) + "ms]: " + sql);
    }

    public void execSQL(String sql, Object[] args) {
        long start = SystemClock.elapsedRealtime();
        mDatabase.execSQL(sql, args);
        long end = SystemClock.elapsedRealtime();

        Log.d(LOG_TAG, "execSQL [" + (end - start) + "ms]: " + sql);
    }

    public Cursor rawQuery(String sql, String[] selectionArgs) {
        long start = SystemClock.elapsedRealtime();
        Cursor cursor = mDatabase.rawQuery(sql, selectionArgs);
        long end = SystemClock.elapsedRealtime();

        // DB might be queried lazily; make sure query is performed now
        cursor.getCount();
        long end2 = SystemClock.elapsedRealtime();

        Log.d(LOG_TAG, "rawQuery [" + (end - start) + "ms + " + (end2 - end) + "ms]: " + sql);
        return cursor;
    }

    public int update(String table, ContentValues values, String whereClause, String[] whereArgs) {
        long start = SystemClock.elapsedRealtime();
        int rows = mDatabase.update(table, values, whereClause, whereArgs);
        long end = SystemClock.elapsedRealtime();

        Log.d(LOG_TAG, "update [" + (end - start) + "ms]: " + table);
        return rows;
    }

    public Cursor query(String table, String[] columns, String selection, String[] selectionArgs,
            String groupBy, String having, String orderBy) {
        long start = SystemClock.elapsedRealtime();
        Cursor cursor = mDatabase.query(table, columns, selection, selectionArgs, groupBy, having,
                orderBy);
        long end = SystemClock.elapsedRealtime();

        // DB might be queried lazily; make sure query is performed now
        cursor.getCount();
        long end2 = SystemClock.elapsedRealtime();

        Log.d(LOG_TAG, "query [" + (end - start) + "ms + " + (end2 - end) + "ms]: " + table + "; where= " + selection);
        return cursor;
    }

    public long insert(String table, String nullColumnHack, ContentValues values) {
        long start = SystemClock.elapsedRealtime();
        long id = mDatabase.insert(table, nullColumnHack, values);
        long end = SystemClock.elapsedRealtime();

        Log.d(LOG_TAG, "insert [" + (end - start) + "ms]: " + table);
        return id;
    }

    public int delete(String table, String whereClause, String[] whereArgs) {
        long start = SystemClock.elapsedRealtime();
        int rows = mDatabase.delete(table, whereClause, whereArgs);
        long end = SystemClock.elapsedRealtime();

        Log.d(LOG_TAG, "delete [" + (end - start) + "ms]: " + table);
        return rows;
    }

    public long replace(String table, String nullColumnHack, ContentValues initialValues) {
        long start = SystemClock.elapsedRealtime();
        long id = mDatabase.replace(table, nullColumnHack, initialValues);
        long end = SystemClock.elapsedRealtime();

        Log.d(LOG_TAG, "replace [" + (end - start) + "ms]: " + table);
        return id;
    }
}
