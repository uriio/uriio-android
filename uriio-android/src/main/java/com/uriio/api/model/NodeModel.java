package com.uriio.api.model;

import com.google.gson.annotations.SerializedName;

public class NodeModel {
    @SerializedName("id")
    public String id;

    @SerializedName("url")
    public String url;

    @SerializedName("title")
    public String title;

    @SerializedName("description")
    public String description;

    @SerializedName("favicon")
    public String favicon;

    @SerializedName("forced")
    public boolean forceRedirect;
}