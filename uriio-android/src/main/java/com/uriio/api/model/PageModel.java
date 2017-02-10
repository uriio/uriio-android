package com.uriio.api.model;

import com.google.gson.annotations.SerializedName;

public class PageModel<T> {
    @SerializedName("items")
    public T[] items;

    @SerializedName("next_page")
    public String nextPageToken;
}