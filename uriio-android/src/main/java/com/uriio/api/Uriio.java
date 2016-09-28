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
     * Creates an EphemeralURL beacon based on the provided URL registration result.
     * @param urlResource         URL registration info
     * @param beaconTimeToLive    Initial value for the beacon's TTL for issuing ephemeral short URLs.
     * @return A new beaoon, without saving or starting it. You can adjust any other beacon properties and save it.
     */
    @NonNull
    public static EphemeralURL createBeacon(UrlResource urlResource, int beaconTimeToLive) {
        return new EphemeralURL(urlResource.getId(), urlResource.getToken(),
                beaconTimeToLive, urlResource.getUrl(),
                AdvertiseSettings.ADVERTISE_MODE_BALANCED,
                AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM);
    }

    /**
     * Registers an URL resource.
     * @param url         The URL to register
     * @param callback    Callback for receiving the registration result.
     */
    public static void registerUrl(String url, Callback<UrlResource> callback) {
        getAPiClient().registerUrl(url, null, callback);
    }

    /**
     * Registers an URL resource, creates a beacon for it, and optionally starts and saves it.
     * @param url                 The URL to register
     * @param beaconTimeToLive    Initial TTL for the issued beacon URLs.
     * @param startBeacon         Starts the beacon.
     * @param saveBeacon          Saves the beacon.
     * @param callback            Callback for receiving the beacon created based on the registration result.
     */
    public static void registerUrl(String url, final int beaconTimeToLive,
                                   final boolean startBeacon, final boolean saveBeacon,
                                   final Callback<EphemeralURL> callback) {
        registerUrl(url, new Callback<UrlResource>() {
            @Override
            public void onResult(UrlResource result, Throwable error) {
                EphemeralURL beacon = null;
                if (null != result) {
                    beacon = Uriio.createBeacon(result, beaconTimeToLive);

                    if (saveBeacon) {
                        beacon.save(startBeacon);
                    }
                    else if (startBeacon) {
                        beacon.start();
                    }
                }

                if (null != callback) {
                    callback.onResult(beacon, error);
                }
            }
        });
    }

    /**
     * Registers an URL resource and creates a beacon for it, started and saved.
     * @param url                 The URL to register
     * @param beaconTimeToLive    Initial TTL for the issued beacon URLs.
     * @param callback            Callback for receiving the beacon created based on the registration result.
     */
    public static void registerUrl(String url, int beaconTimeToLive, Callback<EphemeralURL> callback) {
        registerUrl(url, beaconTimeToLive, true, true, callback);
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

    /**
     * Fetches information for a registered URL.
     * @param urlId       Registered URL id.
     * @param urlToken    Registered URL token.
     * @param callback    Result callback.
     */
    public static void getUrl(long urlId, String urlToken, Callback<UrlResource> callback) {
        getAPiClient().getUrl(urlId, urlToken, callback);
    }

    public static void getUrl(EphemeralURL beacon, Callback<UrlResource> callback) {
        getUrl(beacon.getUrlId(), beacon.getUrlToken(), callback);
    }

    /**
     * Deletes a URL resource from the server.
     * @param urlId       Registered URL id.
     * @param urlToken    Registered URL token.
     * @param callback    Result callback. On success, the resource is non-null and contains the deleted date.
     */
    public static void deleteUrl(long urlId, String urlToken, Callback<UrlResource> callback) {
        getAPiClient().deleteUrl(urlId, urlToken, callback);
    }

    /**
     * Deletes a URL resource for the specified beacon. On success, it also stops and deletes the beacon.
     * @param beacon      The beacon to unregister and eventually delete.
     * @param callback    Result callback. On success, the resource is non-null and contains the deleted date.
     */
    public static void deleteUrl(final EphemeralURL beacon, final Callback<UrlResource> callback) {
        deleteUrl(beacon.getUrlId(), beacon.getUrlToken(), new Callback<UrlResource>() {
            @Override
            public void onResult(UrlResource result, Throwable error) {
                if (null != result) {
                    beacon.delete();
                }

                if (null != callback) {
                    callback.onResult(result, error);
                }
            }
        });
    }

    private static void issueShortUrl(final EphemeralURL beacon, final Callback<Boolean> callback) {
        getAPiClient().issueBeaconUrls(beacon.getUrlId(), beacon.getUrlToken(), beacon.getTimeToLive(), 1,
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
