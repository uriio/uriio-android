package com.uriio.api.model;

import com.google.gson.annotations.SerializedName;

public class TokenInfoModel {
    @SerializedName("beacon")
    public String beacon;

    @SerializedName("validSince")
    public String validSince;

    @SerializedName("validUntil")
    public String validUntil;
}