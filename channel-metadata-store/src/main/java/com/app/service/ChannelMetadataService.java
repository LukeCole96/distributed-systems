package com.app.service;

import com.app.cache.CacheUpdateEvent;
import com.app.entity.ChannelMetadataEntity;
import com.app.kafka.KafkaProducerService;
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
    private final KafkaProducerService kafkaProducerService;
    private final ApplicationEventPublisher applicationEventPublisher;

    @Autowired
    public ChannelMetadataService(ChannelMetadataRepository channelMetadataRepository, CustomCacheWrapper customCacheWrapper, KafkaProducerService kafkaProducerService, ApplicationEventPublisher applicationEventPublisher) {
        this.channelMetadataRepository = channelMetadataRepository;
        this.customCacheWrapper = customCacheWrapper;
        this.kafkaProducerService = kafkaProducerService;
        this.applicationEventPublisher = applicationEventPublisher;  // Inject ApplicationEventPublisher
    }

    @Transactional(rollbackFor = {Exception.class})
    public ChannelMetadataRequest saveOrUpdateChannelMetadata(String countryCode, ChannelMetadataRequest request) {
        ChannelMetadataEntity entity = convertRequestToEntity(countryCode, request);
        log.info("Attempting to save entity to the database for countryCode: {}", countryCode);

        try {
            ChannelMetadataEntity savedEntity = channelMetadataRepository.save(entity);

            applicationEventPublisher.publishEvent(new CacheUpdateEvent(countryCode, mapEntityToModel(savedEntity)));
            log.info("about to return the updated model...");

            return mapEntityToModel(savedEntity);

        } catch (Exception e) {
            log.error("Database connection issue. Fallback to Kafka retry mechanism.", e);
            handleDbConnectionFailure(countryCode, entity);
        }
        return null;
    }


    private void handleDbConnectionFailure(String countryCode, ChannelMetadataEntity entity) {
        try {
            customCacheWrapper.put(countryCode, entity);
            String message = String.format("Database update failed for key: %s, Metadata: %s", countryCode, entity.getMetadata());
            kafkaProducerService.sendMessage("retry-db-write-from-cache", countryCode, message);

        } catch (Exception e) {
            log.error("Error occurred while handling DB connection failure for countryCode: {}", countryCode, e);
            throw e;
        }
    }

    public ChannelMetadataRequest getChannelMetadataByCountryCode(String countryCode) {
        log.info("Fetching channel metadata for countryCode: {}", countryCode);

        ChannelMetadataRequest cachedValue = (ChannelMetadataRequest) customCacheWrapper.get(countryCode);
        if (cachedValue != null) {
            log.info("Found value in cache for countryCode: {}", countryCode);
            return cachedValue;
        }

        ChannelMetadataEntity entity = channelMetadataRepository.findByCountryCode(countryCode);
        if (entity != null) {
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
