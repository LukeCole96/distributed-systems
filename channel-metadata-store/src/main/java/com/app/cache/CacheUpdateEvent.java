package com.app.cache;

import com.app.model.ChannelMetadataRequest;

public class CacheUpdateEvent {

    private final String countryCode;
    private final ChannelMetadataRequest request;

    public CacheUpdateEvent(String countryCode, ChannelMetadataRequest request) {
        this.countryCode = countryCode;
        this.request = request;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public ChannelMetadataRequest getRequest() {
        return request;
    }
}