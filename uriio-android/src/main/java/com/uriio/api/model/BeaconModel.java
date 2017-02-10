package com.uriio.api.model;

import com.google.gson.annotations.SerializedName;

public class BeaconModel {
    public static final String FIELD_NODE = "node";

    @SerializedName("id")
    public String id;

    /**
     * Beacon absolute epoch as a UNIX timestamp in microseconds. Ephemeral tokens must be
     * correlated to this exact point in time.
     */
    @SerializedName("epoch")
    public long epoch;

    @SerializedName(FIELD_NODE)
    public String nodeId;

    @SerializedName("active")
    public boolean active;

    @SerializedName("tag")
    public String tag;
}