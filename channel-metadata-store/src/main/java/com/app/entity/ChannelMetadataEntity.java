package com.app.entity;

import com.app.model.ChannelMetadataRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.*;

import java.util.List;

@Entity
@Table(name = "channel_metadata")
public class ChannelMetadataEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    protected Long id;

    protected String countryCode;
    protected String metadata;
    protected String product;  //comes from client in a header

    public ChannelMetadataEntity() {
        // Default constructor required by JPA
    }

    public ChannelMetadataEntity(ChannelMetadataRequest request) {
        this.countryCode = request.getCountryCode();
        this.metadata = convertToJson(request.getMetadata());
        this.product = request.getProduct();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public void setCountryCode(String countryCode) {
        this.countryCode = countryCode;
    }

    public String getMetadata() {
        return metadata;
    }

    public void setMetadata(String metadata) {
        this.metadata = metadata;
    }

    public String getProduct() {
        return product;
    }

    public void setProduct(String product) {
        this.product = product;
    }

    private String convertToJson(Object obj) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            if (obj instanceof List<?>) {
                return mapper.writeValueAsString(obj);
            } else if (obj instanceof String) {
                return (String) obj; // Assumes the input string is already in JSON format
            } else {
                throw new IllegalArgumentException("Unsupported type for JSON conversion");
            }
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to convert object to JSON", e);
        }
    }

}