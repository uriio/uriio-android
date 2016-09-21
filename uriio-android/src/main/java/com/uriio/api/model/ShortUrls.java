package com.uriio.api.model;

import com.google.gson.annotations.SerializedName;

public class ShortUrls {
    @SerializedName("items")
    private ShortUrl[] items;

    public ShortUrl[] getItems() {
        return items;
    }
}
