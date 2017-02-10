package com.uriio.api;

import com.google.gson.JsonParseException;
import com.uriio.api.model.Error;
import com.uriio.api.model.ErrorHolder;
import com.uriio.beacons.Callback;

import java.io.IOException;
import java.lang.annotation.Annotation;

import retrofit2.Call;
import retrofit2.Response;

/**
 * Handles a valid API or server error response and calls a higher-level result callback.
 * Created on 5/5/2016.
 */
public class SimpleResultHandler<T> implements retrofit2.Callback<T> {
    private final Callback<T> callback;

    public SimpleResultHandler(Callback<T> callback) {
        this.callback = callback;
    }

    @Override
    public void onResponse(Call<T> call, Response<T> response) {
        if (response.isSuccessful()) {
            callback.onResult(response.body(), null);
        } else {
            callback.onResult(null, extractError(response));
        }
    }

    @Override
    public void onFailure(Call<T> call, Throwable t) {
        callback.onResult(null, t);
    }

    private static Throwable extractError(Response response) {
        Throwable throwable;
        int statusCode = response.code();

        if (response.errorBody() != null) {
            try {
                ErrorHolder errorModel = (ErrorHolder) ApiClient.getRetrofit()
                        .responseBodyConverter(ErrorHolder.class, new Annotation[0])
                        .convert(response.errorBody());
                Error errorDetails = errorModel.getError();
                String error = null != errorDetails ? errorDetails.message : null;
                throwable = new ApiException(statusCode, error);
            } catch (IOException | JsonParseException e) {
                // http error 5xx or non-json content
                throwable = e;
            }
        }
        else throwable = new ApiException(statusCode, "Invalid response");

        return throwable;
    }
}
