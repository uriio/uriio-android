package com.uriio.api;

import android.bluetooth.le.AdvertiseSettings;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;

import com.uriio.api.model.ShortUrl;
import com.uriio.api.model.ShortUrls;
import com.uriio.api.model.UrlResource;
import com.uriio.beacons.Beacons;
import com.uriio.beacons.Callback;
import com.uriio.beacons.model.EphemeralURL;

import java.util.Date;

/**
 * UriIO API wrapper, used to register, update, and issue ephemeral URLs.
 */
public class Uriio {
    private static ApiClient _apiClient = null;
    private static boolean _initialized = false;

    /**
     * Initializes the library.
     * @param context    Calling context
     */
    public static void initialize(Context context) {
        Beacons.initialize(context);

        if (!_initialized) {
            _initialized = true;

            // inject issuer
            EphemeralURL.setIssuer(new EphemeralURL.ShortURLIssuer() {
                @Override
                public void issueBeaconUrl(EphemeralURL beacon, Callback<Boolean> callback) {
                    issueShortUrl(beacon, callback);
                }
            });
        }
    }

    private static ApiClient getAPiClient() {
        if (null == _apiClient) {
            _apiClient = new ApiClient(extractApiKey(Beacons.getContext()));
        }

        return _apiClient;
    }

    /**
     * Registers an URL resource.
     * @param url         The URL to register
     * @param callback    Callback for receiving the registration result.
     */
    public static void registerUrl(String url, final Callback<UrlResource> callback) {
        getAPiClient().registerUrl(url, null, callback);
    }

    /**
     * Registers an URL resource, creates a beacon for it, and starts it.
     * @param url                 The URL to register
     * @param beaconTimeToLive    Initial TTL for the issued beacon URLs.
     * @param callback            Callback for receiving the beacon created based on the registration result.
     */
    public static void registerUrlAndAdvertise(String url, final int beaconTimeToLive, final Callback<EphemeralURL> callback) {
        getAPiClient().registerUrl(url, null, new Callback<UrlResource>() {
            @Override
            public void onResult(UrlResource result, Throwable error) {
                EphemeralURL beacon = null;
                if (null != result) {
                    beacon = Uriio.createBeacon(result, beaconTimeToLive);
                    beacon.save(true);
                }

                if (null != callback) {
                    callback.onResult(beacon, error);
                }
            }
        });
    }

    /**
     * Creates an EphemeralURL beacon based on the provided URL registration result.
     * @param urlResource         URL registration info
     * @param beaconTimeToLive    Initial value for the beacon's TTL for issuing ephemeral short URLs.
     * @return A new beaoon, without saving it or starting it. You can adjust any other beacon properties and save it to storage.
     */
    @NonNull
    public static EphemeralURL createBeacon(UrlResource urlResource, int beaconTimeToLive) {
        return new EphemeralURL(urlResource.getId(), urlResource.getToken(),
                beaconTimeToLive, urlResource.getUrl(),
                AdvertiseSettings.ADVERTISE_MODE_BALANCED,
                AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM);
    }

    /**
     * Modifies the target URL.
     * @param beacon      The beacon containing URL registration info.
     * @param url         New target URL to be redirected to.
     * @param callback    Callback for being notified when the operation finishes and the new info is saved.
     */
    public static void updateUrl(final EphemeralURL beacon, String url, final Callback<EphemeralURL> callback) {
        getAPiClient().updateUrl(beacon.getUrlId(), beacon.getUrlToken(), url, new Callback<UrlResource>() {
            @Override
            public void onResult(UrlResource result, Throwable error) {
                if (null != result) {
                    beacon.edit().setLongUrl(result.getUrl()).apply();
                }

                if (null != callback) {
                    callback.onResult(beacon, error);
                }
            }
        });
    }

    private static void issueShortUrl(final EphemeralURL beacon, final Callback<Boolean> callback) {
        getAPiClient().issueShortUrls(beacon.getUrlId(), beacon.getUrlToken(), beacon.getTimeToLive(), 1,
                new Callback<ShortUrls>() {
                    @Override
                    public void onResult(ShortUrls result, Throwable error) {
                        if (null != result) {
                            ShortUrl shortUrl = result.getItems()[0];
                            Date expireDate = shortUrl.getExpire();
                            long expireTime = null == expireDate ? 0 : expireDate.getTime();

                            beacon.edit()
                                    .setShortUrl(shortUrl.getUrl(), expireTime)
                                    .apply();
                        } else {
                            beacon.setError("Update failed");
                        }

                        if (null != callback) {
                            callback.onResult(null != result, error);
                        }
                    }
                });
    }

    private static String extractApiKey(Context context) {
        ApplicationInfo appInfo;
        try {
            appInfo = context.getPackageManager().getApplicationInfo(context.getPackageName(), PackageManager.GET_META_DATA);
        } catch (PackageManager.NameNotFoundException e) {
            throw new IllegalArgumentException("App package not found");
        }

        String apiKey = null;
        // metadata is null when no entries exist
        if (null != appInfo && null != appInfo.metaData) {
            apiKey = appInfo.metaData.getString("com.uriio.apiKey");
        }

        if (null == apiKey) {
            // fatal error - no api key defined in the client app
            throw new IllegalArgumentException("Missing com.uriio.apiKey meta-data in manifest");
        }

        return apiKey;
    }
}
