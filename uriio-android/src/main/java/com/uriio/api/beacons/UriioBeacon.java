package com.uriio.api.beacons;

import com.uriio.beacons.BleService;
import com.uriio.beacons.BuildConfig;
import com.uriio.beacons.Callback;
import com.uriio.beacons.Storage;
import com.uriio.beacons.Util;
import com.uriio.beacons.model.EddystoneURL;

import static com.uriio.beacons.BleService.EVENT_START_FAILED;

/**
 * Wrapper for an UriIO beacon.
 */
public class UriioBeacon extends EddystoneURL {
    public static final int KIND  = 0x10000;
    private static final String TAG = "UriioBeacon";

    /** Long URL **/
    private String mLongUrl;

    /** Url ID **/
    private long mUrlId;

    /** Url Token **/
    private String mUrlToken;

    private int mTimeToLive;

    private long mExpireTime = 0;

    /**
     * Ephemeral URL spec.
     * @param urlId         The URL registration ID.
     * @param urlToken      The URL registration token.
     * @param ttl           Optional Time to Live for the ephemeral beacon URLs, in seconds.
     * @param longUrl       The destination URL. May be null if registration was done already.
     */
    public UriioBeacon(long itemId, long urlId, String urlToken, int ttl, String longUrl,
                       long expireTimestamp, String shortUrl,
                       @AdvertiseMode int advertiseMode,
                       @AdvertiseTxPower int txPowerLevel,
                       String name) {
        super(itemId, shortUrl, null, advertiseMode, txPowerLevel, name);
        init(urlId, urlToken, ttl, longUrl, expireTimestamp);
    }

    public UriioBeacon(long urlId, String urlToken, int ttl, String longUrl,
                       long expireTimestamp, String shortUrl,
                       @AdvertiseMode int advertiseMode,
                       @AdvertiseTxPower int txPowerLevel,
                       String name) {
        this(0, urlId, urlToken, ttl, longUrl, expireTimestamp, shortUrl, advertiseMode, txPowerLevel, name);
    }

    public UriioBeacon(long urlId, String urlToken, int ttl, String longUrl,
                       long expireTimestamp, String shortUrl,
                       @AdvertiseMode int advertiseMode,
                       @AdvertiseTxPower int txPowerLevel) {
        this(0, urlId, urlToken, ttl, longUrl, expireTimestamp, shortUrl, advertiseMode, txPowerLevel, null);
    }

    public UriioBeacon(long urlId, String urlToken, int ttl, String longUrl,
                       @AdvertiseMode int advertiseMode,
                       @AdvertiseTxPower int txPowerLevel, String name) {
        this(0, urlId, urlToken, ttl, longUrl, 0, null, advertiseMode, txPowerLevel, name);
    }

    public UriioBeacon(long urlId, String urlToken, int ttl, String longUrl,
                       @AdvertiseMode int advertiseMode,
                       @AdvertiseTxPower int txPowerLevel) {
        this(0, urlId, urlToken, ttl, longUrl, 0, null, advertiseMode, txPowerLevel, null);
    }

    public UriioBeacon(long urlId, String urlToken, int ttl, String longUrl, long expireTime, String shortUrl) {
        super(shortUrl);
        init(urlId, urlToken, ttl, longUrl, expireTime);
    }

    public UriioBeacon(long urlId, String urlToken, int ttl) {
        this(urlId, urlToken, ttl, null, 0, null);
    }

    private void init(long urlId, String urlToken, int ttl, String longUrl, long expireTimestamp) {
        mUrlId = urlId;
        mUrlToken = urlToken;
        mTimeToLive = ttl;
        mLongUrl = longUrl;
        mExpireTime = expireTimestamp;
    }

    public String getUrlToken() {
        return mUrlToken;
    }

    public int getTimeToLive() {
        return mTimeToLive;
    }

    public String getLongUrl() {
        return mLongUrl;
    }

    public long getMillisecondsUntilExpires() {
        return 0 == mExpireTime ? Long.MAX_VALUE : mExpireTime - System.currentTimeMillis();
    }

    @Override
    public long getScheduledRefreshTime() {
        // schedule refresh 7 seconds before actual server timeout
        return mExpireTime - 7 * 1000;
    }

    public long getActualExpireTime() {
        return mExpireTime;
    }

    public long getUrlId() {
        return mUrlId;
    }

    @Override
    public int getKind() {
        return KIND;
    }

    public interface ShortURLIssuer {
        void issueBeaconUrl(UriioBeacon beacon, Callback<Boolean> callback);
    }

    private static ShortURLIssuer _issuerImpl = null;

    public static void setIssuer(ShortURLIssuer issuer) {
        _issuerImpl = issuer;
    }

    @Override
    public void onAdvertiseEnabled(final BleService service) {
        if (null == getURL() || getMillisecondsUntilExpires() < 7 * 1000) {
            if (null == _issuerImpl) {
                service.broadcastError(this, EVENT_START_FAILED, "No URL provider!");
            }
            else {
                if (BuildConfig.DEBUG) Util.log(TAG, "Updating beacon URL for beacon " + getUUID());
                _issuerImpl.issueBeaconUrl(this, new Callback<Boolean>() {
                    @Override
                    public void onResult(Boolean result, Throwable error) {
                        if (result) {   // true or false, never null
                            service.startBeaconAdvertiser(UriioBeacon.this);
                        }
                        else if (null != error) {
                            service.broadcastError(UriioBeacon.this, EVENT_START_FAILED, error.getMessage());
                        }
                    }
                });
            }
        }
        else {
            service.startBeaconAdvertiser(this);
        }
    }

    @Override
    public UriioEditor edit() {
        return new UriioEditor();
    }

    public class UriioEditor extends EddystoneURLEditor {
        private boolean mShortUrlChanged = false;

        public BaseEditor setShortUrl(String shortUrl, long expireTime) {
            setUrl(shortUrl);

            if (mExpireTime != expireTime) {
                mExpireTime = expireTime;
                mRestartBeacon = true;
            }

            mShortUrlChanged = true;

            return this;
        }

        public UriioEditor setTTL(int timeToLive) {
            if (timeToLive != mTimeToLive) {
                mTimeToLive = timeToLive;
                mRestartBeacon = true;

                // force a short URL issue since TTL changed
                setShortUrl(null, 0);
            }
            return this;
        }

        public UriioEditor setLongUrl(String url) {
            if (null == url || !url.equals(mLongUrl)) {
                mLongUrl = url;
            }
            return this;
        }

        @Override
        public void apply() {
            if (mShortUrlChanged) {
                Storage.getInstance().update(UriioBeacon.this, Store.FLAG_UPDATE_SHORT_URL);
            }

            super.apply();
        }
    }
}