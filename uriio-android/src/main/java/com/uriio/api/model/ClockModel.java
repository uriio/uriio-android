package com.uriio.api.model;

import com.google.gson.annotations.SerializedName;

public class ClockModel {
    @SerializedName("now")
    public long currentTimeMs;
}