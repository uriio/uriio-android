package com.uriio.api.beacons;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;

import com.uriio.beacons.Storage;
import com.uriio.beacons.model.Beacon;

/**
 * Storage interface layer for dynamic beacon model.
 */
public class Store implements Storage.Persistable {
    static final int FLAG_UPDATE_SHORT_URL = 1;

    private static final int COLUMN_URL_TOKEN = 0;
    private static final int COLUMN_LONG_URL  = 1;
    private static final int COLUMN_URL_ID    = 2;
    private static final int COLUMN_TTL       = 3;
    private static final int COLUMN_EXPIRES   = 4;
    private static final int COLUMN_SHORT_URL = 5;

    // lazy update statements
    private SQLiteStatement mUpdateStmt = null;
    private SQLiteStatement mUpdateShortUrlStmt = null;

    @Override
    public int getKind() {
        return UriioBeacon.KIND;
    }

    @Override
    public void prepareInsert(Beacon beacon, SQLiteStatement statement) {
        UriioBeacon item = (UriioBeacon) beacon;

        statement.bindString(1, item.getUrlToken());
        Storage.bindStringOrNull(statement, 2, item.getLongUrl());
        statement.bindLong(3, item.getUrlId());
        statement.bindLong(4, item.getTimeToLive());
    }

    @Override
    public void close() {
        if (null != mUpdateStmt) {
            mUpdateStmt.close();
            mUpdateStmt = null;
        }

        if (null != mUpdateShortUrlStmt) {
            mUpdateShortUrlStmt.close();
            mUpdateShortUrlStmt = null;
        }
    }

    @Override
    public void onDeleted(Beacon beacon) {

    }

    @Override
    public SQLiteStatement prepareUpdate(Beacon beacon, SQLiteDatabase db, int flags) {
        UriioBeacon item = (UriioBeacon) beacon;

        switch (flags) {
            case 0:
                if (null == mUpdateStmt) {
                    mUpdateStmt = Storage.createUpdater(db, COLUMN_LONG_URL, COLUMN_TTL);
                }

                Storage.bindStringOrNull(mUpdateStmt, 2, item.getLongUrl());
                mUpdateStmt.bindLong(3, item.getTimeToLive());

                return mUpdateStmt;
            case FLAG_UPDATE_SHORT_URL:
                if (null == mUpdateShortUrlStmt) {
                    mUpdateShortUrlStmt = Storage.createUpdater(db, COLUMN_SHORT_URL, COLUMN_EXPIRES);
                }

                Storage.bindStringOrNull(mUpdateShortUrlStmt, 2, item.getURL());
                mUpdateShortUrlStmt.bindLong(3, item.getActualExpireTime());

                return mUpdateShortUrlStmt;
            default:
                return null;
        }
    }

    @Override
    public Beacon fromCursor(Cursor cursor) {
        String urlToken = cursor.getString(COLUMN_URL_TOKEN);
        String longUrl = cursor.getString(COLUMN_LONG_URL);
        long urlId = cursor.getLong(COLUMN_URL_ID);
        int ttl = cursor.getInt(COLUMN_TTL);
        long expires = cursor.getLong(COLUMN_EXPIRES);
        String shortUrl = cursor.getString(COLUMN_SHORT_URL);

        return new UriioBeacon(urlId, urlToken, ttl, longUrl, expires, shortUrl);
    }
}