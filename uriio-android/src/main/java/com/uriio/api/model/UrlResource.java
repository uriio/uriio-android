package com.uriio.api.model;

import android.util.Base64;

import com.google.gson.annotations.SerializedName;

public class UrlResource {
    /**
     * Outgoing API Key for POST/PUT requests
     */
    @SerializedName("apiKey")
    private String apiKey;

    @SerializedName("id")
    private long id;

    @SerializedName("url")
    private String url;

    @SerializedName("token")
    private String token;

    @SerializedName("publicKey")
    private String publicKey;

    /** Total issued URLs **/
    @SerializedName("numIssued")
    private long numIssued;

    @SerializedName("created")
    private String created;

    @SerializedName("deleted")
    private String deleted;

    @SerializedName("hits")
    private long hits;

    public UrlResource(String apiKey, String url, byte[] publicKey) {
        this.apiKey = apiKey;
        this.url = url;
        this.publicKey = Base64.encodeToString(publicKey, Base64.NO_PADDING | Base64.NO_WRAP | Base64.URL_SAFE);
    }

    public UrlResource(String apiKey, String urlToken, String url) {
        this.apiKey = apiKey;
        this.url = url;
        this.token = urlToken;
    }

    public String getToken() {
        return token;
    }

    public long getId() {
        return id;
    }

    public String getUrl() {
        return url;
    }
}