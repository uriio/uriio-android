package com.uriio.api.model;

import com.google.gson.annotations.SerializedName;

public class RegBeaconModel {
    @SerializedName("serviceEcdhPublicKey")
    public String serviceEcdhPublicKey;

    @SerializedName("beaconEcdhPublicKey")
    public String beaconEcdhPublicKey;

    @SerializedName("initialEid")
    public String initialEid;

    @SerializedName("initialClockValue")
    public int initialClock;

    @SerializedName("rotationPeriodExponent")
    public byte rotationPeriodExponent;

    @SerializedName("tag")
    public String tag;
}