package com.app.service;

import com.app.entity.ChannelMetadataEntity;
import com.app.model.ChannelMetadataRequest;
import com.app.repository.ChannelMetadataRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;

@Service
public class ChannelMetadataService {

    private final ChannelMetadataRepository channelMetadataRepository;

    @Autowired
    public ChannelMetadataService(ChannelMetadataRepository channelMetadataRepository) {
        this.channelMetadataRepository = channelMetadataRepository;
    }

    @CachePut(value = "distributed-cache", key = "#request.countryCode")
    @Transactional
    public ChannelMetadataRequest saveOrUpdateChannelMetadata(String countryCode, ChannelMetadataRequest request) {
        ChannelMetadataEntity existingEntity = channelMetadataRepository.findByCountryCode(countryCode);

        // If the entity exists, update it; otherwise, create a new one
        ChannelMetadataEntity entity;
        if (existingEntity != null) {
            entity = existingEntity;
            entity.setMetadata(convertToJson(request.getMetadata()));
            entity.setProduct(request.getProduct());
        } else {
            entity = new ChannelMetadataEntity(request);
        }

        ChannelMetadataEntity savedEntity = channelMetadataRepository.save(entity);
        return mapEntityToModel(savedEntity);
    }
    @Cacheable(value = "distributed-cache", key = "#id")
    public ChannelMetadataRequest getChannelMetadataById(Long id) {
        ChannelMetadataEntity entity = channelMetadataRepository.findById(id).orElse(null);
        return entity != null ? mapEntityToModel(entity) : null;
    }

    private ChannelMetadataRequest mapEntityToModel(ChannelMetadataEntity entity) {
        ChannelMetadataRequest model = new ChannelMetadataRequest();
        model.setCountryCode(entity.getCountryCode());
        model.setProduct(entity.getProduct());
        model.setMetadata(parseJsonToChannels(entity.getMetadata()));
        return model;
    }

    private List<ChannelMetadataRequest.Channel> parseJsonToChannels(String json) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.readValue(json, new TypeReference<List<ChannelMetadataRequest.Channel>>(){});
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to parse JSON", e);
        }
    }

    private String convertToJson(List<ChannelMetadataRequest.Channel> channels) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.writeValueAsString(channels);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to convert metadata to JSON", e);
        }
    }
}
