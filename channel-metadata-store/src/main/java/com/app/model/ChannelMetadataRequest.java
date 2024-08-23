package com.app.model;

import lombok.Data;

@Data
public class ChannelMetadataRequest {
    private String metadata;
    private String product; //comes from client in a header
}