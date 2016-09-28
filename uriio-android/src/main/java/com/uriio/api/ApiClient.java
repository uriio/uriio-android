package com.uriio.api;

import com.uriio.api.model.IssueUrls;
import com.uriio.api.model.ShortUrls;
import com.uriio.api.model.UrlResource;
import com.uriio.beacons.Callback;

import org.whispersystems.curve25519.Curve25519;
import org.whispersystems.curve25519.Curve25519KeyPair;

import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;

/**
 * REST API wrapper.
 */
class ApiClient {
    private interface UriioService {
        @POST("urls")
        Call<UrlResource> registerUrl(@Body UrlResource urlResource);

        @PUT("urls/{id}")
        Call<UrlResource> updateUrl(@Path("id") long id, @Body UrlResource urlResource);

        @POST("urls/{id}")
        Call<ShortUrls> issueBeaconUrls(@Path("id") long id, @Body IssueUrls issueUrls);

        @GET("urls/{id}")
        Call<UrlResource> getUrl(@Path("id") long id, @Query("apiKey") String apiKey,
                                 @Query("token") String token);

        @DELETE("urls/{id}")
        Call<UrlResource> deleteUrl(@Path("id") long id, @Query("apiKey") String apiKey,
                                    @Query("token") String token);
    }

    private static final String ROOT_SERVICE_URL = "https://api.uriio.com/v1/";

    private static Retrofit _instance;
    private final String mApiKey;
    private final UriioService mApiService;

    static Retrofit getRetrofit() {
        if (null == _instance) {
            _instance = new Retrofit.Builder()
                    .baseUrl(ROOT_SERVICE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return _instance;
    }

    ApiClient(String apiKey) {
        mApiKey = apiKey;
        mApiService = getRetrofit().create(ApiClient.UriioService.class);
    }

    /**
     * Registers a new long URL resource.
     * @param url           The long URL.
     * @param urlPublicKey  The public key of the new URL. Each URL should have its own key-pair.
     *                      If null, a public key will be generated using Curve25519.generateKeyPair()
     * @param callback      Result callback.
     */
    void registerUrl(String url, byte[] urlPublicKey, Callback<UrlResource> callback) {
        if (null == urlPublicKey) {
            Curve25519KeyPair keyPair = Curve25519.getInstance(Curve25519.BEST).generateKeyPair();
            urlPublicKey = keyPair.getPublicKey();
        }

        mApiService.registerUrl(new UrlResource(mApiKey, url, urlPublicKey))
                .enqueue(new SimpleResultHandler<>(callback));
    }

    /**
     * Requests a new short URL for the specified resource.
     * @param urlId       The registered URL's ID.
     * @param urlToken    The URL token.
     * @param ttl         Time To Live for the returned short URL (or 0 to never expire).
     * @param numToIssue  How many short URLs to request.
     * @param callback    Result callback.
     */
    void issueBeaconUrls(long urlId, String urlToken, int ttl, int numToIssue,
                         Callback<ShortUrls> callback) {
        mApiService.issueBeaconUrls(urlId, new IssueUrls(mApiKey, urlToken, ttl, numToIssue))
                .enqueue(new SimpleResultHandler<>(callback));
    }

    void updateUrl(long urlId, String urlToken, String longUrl, Callback<UrlResource> callback) {
        mApiService.updateUrl(urlId, new UrlResource(mApiKey, urlToken, longUrl))
                .enqueue(new SimpleResultHandler<>(callback));
    }

    void deleteUrl(long urlId, String urlToken, Callback<UrlResource> callback) {
        mApiService.deleteUrl(urlId, mApiKey, urlToken).enqueue(new SimpleResultHandler<>(callback));
    }

    void getUrl(long urlId, String urlToken, Callback<UrlResource> callback) {
        mApiService.getUrl(urlId, mApiKey, urlToken).enqueue(new SimpleResultHandler<>(callback));
    }
}