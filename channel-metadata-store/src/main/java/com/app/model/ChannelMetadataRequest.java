package com.app.model;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class ChannelMetadataRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    private String countryCode;
    private List<Channel> metadata;
    private String product;

    @Data
    public static class Channel implements Serializable {
        private static final long serialVersionUID = 1L;
        private String name;
        private String language;
        private String type;
    }
}