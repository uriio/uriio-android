package com.uriio.api.model;

import com.google.gson.annotations.SerializedName;

public class AccessTokenModel {
    @SerializedName("access_token")
    public String token;

    /**
     * UNIX epoch expiration time, in seconds
     */
    @SerializedName("expires")
    public long expires;
}