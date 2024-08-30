package com.app.entity;

import com.app.model.ChannelMetadataRequest;
import jakarta.persistence.*;

@Entity
@Table(name = "channel_metadata")
public class ChannelMetadataEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    protected Long id;

    protected String countryCode;
    protected String metadata;
    protected String product;  //comes from client in a header

    public ChannelMetadataEntity(String countryCode, ChannelMetadataRequest request) {
        this.countryCode = countryCode;
        this.metadata = request.getMetadata();
        this.product = request.getProduct();
    }
}