package com.uriio.api;

import android.os.Build;

import com.uriio.api.model.AccessTokenModel;
import com.uriio.api.model.AuthModel;
import com.uriio.api.model.BeaconModel;
import com.uriio.api.model.ClockModel;
import com.uriio.api.model.NodeModel;
import com.uriio.api.model.PageModel;
import com.uriio.api.model.RegBeaconModel;
import com.uriio.api.model.RegParamsModel;
import com.uriio.api.model.TokenInfoModel;
import com.uriio.beacons.Callback;

import org.json.JSONObject;

import java.io.IOException;
import java.util.Map;

import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;

/**
 * REST API wrapper.
 */
class ApiClient {
    private static final String ROOT_SERVICE_URL = "https://api.uriio.com/v2/";

    private static Retrofit _retrofit;
    private static ApiClient _apiClient = null;

    private interface ApiService {
        @POST("auth")
        Call<AccessTokenModel> authenticate(@Body AuthModel auth);

        @GET("clock")
        Call<ClockModel> getServerClock();

        @GET("params")
        Call<RegParamsModel> getParams(@Header("Authorization") String auth);

        @POST("beacons")
        Call<BeaconModel> registerBeacon(@Header("Authorization") String auth,
                                         @Body RegBeaconModel beaconRegistration);

        @GET("beacons/{id}")
        Call<BeaconModel> getBeacon(@Header("Authorization") String auth, @Path("id") String id);

        @PUT("beacons/{id}")
        Call<BeaconModel> updateBeacon(@Header("Authorization") String auth, @Path("id") String id,
                                       @Body RequestBody params);

        @GET("eid/{id}")
        Call<TokenInfoModel> checkToken(@Header("Authorization") String auth, @Path("id") String id);

        @GET("nodes")
        Call<PageModel<NodeModel>> listNodes(@Header("Authorization") String auth,
                                             @Query("page") String pageToken,
                                             @Query("limit") int limit);

        @POST("nodes")
        Call<NodeModel> insertNode(@Header("Authorization") String auth, @Body NodeModel node);

        @GET("nodes/{id}")
        Call<NodeModel> getNode(@Header("Authorization") String auth, @Path("id") String id);
    }

    private final ApiService mApiService;
    private String mAuthHeader = null;

    private static ApiClient getInstance() {
        if (null == _apiClient) {
            _apiClient = new ApiClient();
        }

        return _apiClient;
    }

    private ApiClient() {
        mApiService = getRetrofit().create(ApiService.class);
    }

    private static ApiService getService() {
        return getInstance().mApiService;
    }

    static Retrofit getRetrofit() {
        if (null == _retrofit) {
            OkHttpClient.Builder httpClient = new OkHttpClient.Builder();
            httpClient.addInterceptor(new Interceptor() {
                private String userAgent = String.format("BeaconToy/37 (Android %s; %s)",
                        Build.VERSION.RELEASE, Build.MODEL);

                @Override
                public Response intercept(Interceptor.Chain chain) throws IOException {
                    Request original = chain.request();

                    Request request = original.newBuilder()
                            .header("User-Agent", userAgent)
                            .method(original.method(), original.body())
                            .build();

                    return chain.proceed(request);
                }
            });

            _retrofit = new Retrofit.Builder()
                    .baseUrl(ROOT_SERVICE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .client(httpClient.build())
                    .build();
        }
        return _retrofit;
    }

    static void setAccessToken(String accessToken) {
        getInstance().mAuthHeader = null == accessToken ? null : String.format("Bearer %s", accessToken);
    }

    private static <T> Call<T> enqueue(Call<T> call, Callback<T> callback) {
        call.enqueue(new SimpleResultHandler<>(callback));
        return call;
    }

    static Call<AccessTokenModel> authenticate(String firebaseIdToken,
                                               Callback<AccessTokenModel> callback) {
        AuthModel auth = new AuthModel();
        auth.firebaseIdToken = firebaseIdToken;

        return enqueue(getService().authenticate(auth), callback);
    }

    static Call<ClockModel> getServerTime(Callback<ClockModel> callback) {
        return enqueue(getService().getServerClock(), callback);
    }

    static Call<RegParamsModel> getRegistrationParams(Callback<RegParamsModel> callback) {
        return enqueue(getService().getParams(getInstance().mAuthHeader), callback);
    }

    static Call<BeaconModel> registerBeacon(RegBeaconModel body, Callback<BeaconModel> callback) {
        return enqueue(getService().registerBeacon(getInstance().mAuthHeader, body), callback);
    }

    static Call<TokenInfoModel> checkToken(String id, Callback<TokenInfoModel> callback) {
        return enqueue(getService().checkToken(getInstance().mAuthHeader, id), callback);
    }

    static Call<BeaconModel> getBeacon(String id, Callback<BeaconModel> callback) {
        return enqueue(getService().getBeacon(getInstance().mAuthHeader, id), callback);
    }

    static Call<BeaconModel> updateBeacon(String id, Map<String, Object> params,
                                          Callback<BeaconModel> callback) {
        return enqueue(getService().updateBeacon(getInstance().mAuthHeader, id,
                RequestBody.create(MediaType.parse("application/json; charset=utf-8"),
                        new JSONObject(params).toString())), callback);
    }

    static Call<NodeModel> getNode(String id, Callback<NodeModel> callback) {
        return enqueue(getService().getNode(getInstance().mAuthHeader, id), callback);
    }

    static Call<PageModel<NodeModel>> listNodes(int limit, String pageToken,
                                                Callback<PageModel<NodeModel>> callback) {
        return enqueue(getService().listNodes(getInstance().mAuthHeader, pageToken, limit), callback);
    }

    static Call<NodeModel> insertNode(NodeModel node, Callback<NodeModel> callback) {
        return enqueue(getService().insertNode(getInstance().mAuthHeader, node), callback);
    }
}