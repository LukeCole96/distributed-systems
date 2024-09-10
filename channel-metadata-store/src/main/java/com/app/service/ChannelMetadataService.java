package com.app.service;

import com.app.cache.CacheUpdateEvent;
import com.app.entity.ChannelMetadataEntity;
import com.app.metrics.CustomCacheWrapper;
import com.app.model.ChannelMetadataRequest;
import com.app.repository.ChannelMetadataRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;
@Slf4j
@Service
public class ChannelMetadataService {

    private final ChannelMetadataRepository channelMetadataRepository;
    private final CustomCacheWrapper customCacheWrapper;
    private final ApplicationEventPublisher applicationEventPublisher;

    @Autowired
    public ChannelMetadataService(ChannelMetadataRepository channelMetadataRepository, CustomCacheWrapper customCacheWrapper, ApplicationEventPublisher applicationEventPublisher) {
        this.channelMetadataRepository = channelMetadataRepository;
        this.customCacheWrapper = customCacheWrapper;
        this.applicationEventPublisher = applicationEventPublisher;  // Inject ApplicationEventPublisher
    }

    @Transactional
    public ChannelMetadataRequest saveOrUpdateChannelMetadata(String countryCode, ChannelMetadataRequest request) {
        ChannelMetadataEntity entity = convertRequestToEntity(countryCode, request);

        // Save or update the entity in the database
        ChannelMetadataEntity savedEntity = channelMetadataRepository.save(entity);

        // Publish cache update event after saving to the database
        applicationEventPublisher.publishEvent(new CacheUpdateEvent(countryCode, mapEntityToModel(savedEntity)));

        // Return the updated model
        return mapEntityToModel(savedEntity);
    }


    // Fetch Channel Metadata by countryCode, manually checking cache first
    public ChannelMetadataRequest getChannelMetadataByCountryCode(String countryCode) {
        log.info("Fetching channel metadata for countryCode: {}", countryCode);

        // Check the cache first
        ChannelMetadataRequest cachedValue = (ChannelMetadataRequest) customCacheWrapper.get(countryCode);
        if (cachedValue != null) {
            log.info("Found value in cache for countryCode: {}", countryCode);
            return cachedValue;
        }

        // If not found in cache, query from the database
        ChannelMetadataEntity entity = channelMetadataRepository.findByCountryCode(countryCode);
        if (entity != null) {
            // Update cache after fetching from DB (Cache-Aside behavior)
            ChannelMetadataRequest model = mapEntityToModel(entity);
            customCacheWrapper.put(countryCode, model);
            log.info("Updated cache with database value for countryCode: {}", countryCode);
            return model;
        }

        log.warn("No channel metadata found for countryCode: {}", countryCode);
        return null;
    }

    private ChannelMetadataEntity convertRequestToEntity(String countryCode, ChannelMetadataRequest request) {
        ChannelMetadataEntity existingEntity = channelMetadataRepository.findByCountryCode(countryCode);
        ChannelMetadataEntity entity;
        if (existingEntity != null) {
            entity = existingEntity;
            entity.setMetadata(convertToJson(request.getMetadata()));
            entity.setProduct(request.getProduct());
        } else {
            entity = new ChannelMetadataEntity();
            entity.setCountryCode(countryCode);
            entity.setMetadata(convertToJson(request.getMetadata()));
            entity.setProduct(request.getProduct());
        }
        return entity;
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
            return mapper.readValue(json, new TypeReference<List<ChannelMetadataRequest.Channel>>() {});
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
