package com.app.model;

import lombok.Data;
import java.util.List;

@Data
public class ChannelMetadataRequest {
    private String countryCode;
    private List<Channel> metadata;
    private String product;

    @Data
    public static class Channel {
        private String name;
        private String language;
        private String type;
    }
}