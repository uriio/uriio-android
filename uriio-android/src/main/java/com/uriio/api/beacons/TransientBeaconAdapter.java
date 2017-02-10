package com.uriio.api.beacons;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.util.Base64;

import com.uriio.beacons.Storage;
import com.uriio.beacons.model.Beacon;
import com.uriio.beacons.model.TransientBeacon;

/**
 * Storage adapter for Uriio v2 beacon model.
 */
public class TransientBeaconAdapter implements Storage.Persistable {
    public static final int KIND  = 0x10001;
    public static final String URL_PREFIX = "http://u-c.info/";

    private static final TransientBeacon.TokenConverter CONVERTER = new TransientBeacon.TokenConverter() {
        @Override
        public String convert(byte[] eid) {
            // need 78 bits to encode = 10 bytes
            return Base64.encodeToString(eid, 0, 10, Base64.NO_PADDING | Base64.NO_WRAP | Base64.URL_SAFE).substring(0, 13);
        }
    };

    private static final int COLUMN_IDENTITY_KEY = 0;
    private static final int COLUMN_EXPONENT     = 1;
    private static final int COLUMN_EPOCH        = 2;
    private static final int COLUMN_ID           = 3;

    // lazy update statements
    private SQLiteStatement mUpdateStmt = null;
    private SQLiteStatement mUpdateShortUrlStmt = null;

    @Override
    public int getKind() {
        return KIND;
    }

    @Override
    public void prepareInsert(Beacon beacon, SQLiteStatement statement) {
        TransientBeacon item = (TransientBeacon) beacon;

        statement.bindBlob(1, item.getIdentityKey());
        statement.bindLong(2, item.getRotationExponent());
        statement.bindLong(3, item.getEpoch());
        Storage.bindStringOrNull(statement, 4, item.getId());
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
        TransientBeacon item = (TransientBeacon) beacon;

        switch (flags) {
            case TransientBeacon.FLAG_UPDATE_EPOCH:
                if (null == mUpdateShortUrlStmt) {
                    mUpdateShortUrlStmt = Storage.createUpdater(db, COLUMN_EPOCH, COLUMN_ID);
                }

                mUpdateShortUrlStmt.bindLong(2, item.getEpoch());
                Storage.bindStringOrNull(mUpdateShortUrlStmt, 3, item.getId());

                return mUpdateShortUrlStmt;
            default:
                return null;
        }
    }

    @Override
    public Beacon fromCursor(Cursor cursor) {
        byte[] identityKey = cursor.getBlob(COLUMN_IDENTITY_KEY);
        int exponent = cursor.getInt(COLUMN_EXPONENT);
        long epoch = cursor.getLong(COLUMN_EPOCH);
        String id = cursor.getString(COLUMN_ID);

        return createBeacon(identityKey, (byte) exponent, epoch, id);
    }

    public static TransientBeacon createBeacon(byte[] identityKey, byte exponent, long epoch, String id) {
        return new TransientBeacon(KIND, URL_PREFIX, CONVERTER, identityKey, exponent, epoch, id);
    }

//    private static final byte[] BASE79_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_.~!$&'()*+,;=:@".getBytes();
//    private String computeTokenBase79(byte[] eid) {
//        // need 82 bits to cover entire 79**13 range
//        BigInteger bigInt = new BigInteger(1, Arrays.copyOfRange(eid, 0, 11));
//        byte[] digits = new byte[13];
//        int pos = 0;
//
//        BigInteger base = new BigInteger(1, new byte[]{79});
//
//        // make sure we don't do a buffer overrun (only keep most significant digits)
//        while (pos < digits.length && bigInt.compareTo(BigInteger.ZERO) > 0) {
//            BigInteger[] results = bigInt.divideAndRemainder(base);
//            bigInt = results[0];
//            digits[pos++] = BASE79_CHARS[results[1].byteValue()];
//        }
//
//        // pad with 'zeros' in case the value was less than 79**12
//        while (pos < digits.length) {
//            digits[pos++] = BASE79_CHARS[0];
//        }
//
//        return new String(digits);
//    }
}