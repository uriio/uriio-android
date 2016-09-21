package com.uriio.api.model;

import com.google.gson.annotations.SerializedName;

public class ErrorHolder {
    @SerializedName("error")
    private Error error;

    public Error getError() {
        return error;
    }
}