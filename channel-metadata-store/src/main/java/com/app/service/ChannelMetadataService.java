package com.app.service;

import com.app.entity.ChannelMetadataEntity;
import com.app.kafka.KafkaProducerService;
import com.app.metrics.CustomCacheWrapper;
import com.app.model.ChannelMetadataRequest;
import com.app.repository.ChannelMetadataRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Set;

@Slf4j
@Service
public class ChannelMetadataService {

    private final ChannelMetadataRepository channelMetadataRepository;
    private final CustomCacheWrapper customCacheWrapper;
    private final KafkaProducerService kafkaProducerService;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final Counter dbWriteCounter;
    private final Timer dbWriteTimer;
    private final Counter dbReadCounter;
    private final Timer dbReadTimer;

    public ChannelMetadataService(ChannelMetadataRepository channelMetadataRepository,
                                  CustomCacheWrapper customCacheWrapper,
                                  KafkaProducerService kafkaProducerService,
                                  ApplicationEventPublisher applicationEventPublisher,
                                  MeterRegistry meterRegistry) {
        this.channelMetadataRepository = channelMetadataRepository;
        this.customCacheWrapper = customCacheWrapper;
        this.kafkaProducerService = kafkaProducerService;
        this.applicationEventPublisher = applicationEventPublisher;
        this.dbWriteCounter = meterRegistry.counter("db_write_total");
        this.dbWriteTimer = meterRegistry.timer("db_write_duration");
        this.dbReadCounter = meterRegistry.counter("db_read_total");
        this.dbReadTimer = meterRegistry.timer("db_read_duration");
    }

    @Transactional(rollbackFor = {Exception.class})
    @CircuitBreaker(name = "dbService", fallbackMethod = "fallbackToKafka")
    @Retry(name = "dbRetry")
    public void forceUpdateAllFromCache() {
        Set<Object> keySet = customCacheWrapper.getCache().keySet();

        for (Object key : keySet) {
            if (key instanceof String) {
                String cacheKey = (String) key;
                try {
                    ChannelMetadataRequest request = (ChannelMetadataRequest) customCacheWrapper.get(cacheKey);
                    ChannelMetadataEntity entity = convertRequestToEntity(request);

                    dbWriteTimer.record(() -> channelMetadataRepository.save(entity));
                    dbWriteCounter.increment();

                    log.info("Successfully updated database from cache for key: {}", cacheKey);
                } catch (Exception e) {
                    log.error("Error occurred while force updating from cache to the database.", e);
                    throw new RuntimeException("Failed to update database: " + e.getMessage(), e);
                }
            } else {
                log.warn("Cache key is not a string: {}", key);
            }
        }
    }

    @Transactional(rollbackFor = {Exception.class})
    @CircuitBreaker(name = "dbService", fallbackMethod = "fallbackToKafka")
    @Retry(name = "dbRetry")
    public ChannelMetadataRequest saveOrUpdateChannelMetadata(String countryCode, ChannelMetadataRequest request) throws InterruptedException {
        try {
            ChannelMetadataEntity entity = convertRequestToEntity(request);
            ChannelMetadataEntity savedEntity = dbWriteTimer.record(() -> channelMetadataRepository.save(entity));
            dbWriteCounter.increment();

            ChannelMetadataRequest updatedModel = mapEntityToModel(savedEntity);
            customCacheWrapper.put(countryCode, updatedModel);

            log.info("Successfully saved and cached metadata for countryCode: {}", countryCode);
            return updatedModel;
        } catch (Exception e) {
            log.error("Error occurred while saving/updating metadata for countryCode: {}", countryCode, e);
            throw e;
        }
    }

    // Fallback to Kafka when db is down
    public ChannelMetadataRequest fallbackToKafka(String countryCode, ChannelMetadataRequest request, Throwable t) {
        log.warn("Database is unavailable, falling back to Kafka. Reason: {}", t.getMessage());
        kafkaProducerService.sendMessage("retry-db-write-from-cache", countryCode, request.toString());
        return null;
    }

    public ChannelMetadataRequest getChannelMetadataByCountryCode(String countryCode) {
        log.info("Fetching channel metadata for countryCode: {}", countryCode);

        Object cachedValue = customCacheWrapper.get(countryCode);
        if (cachedValue instanceof ChannelMetadataRequest) {
            log.info("Found value in cache for countryCode: {}", countryCode);
            return (ChannelMetadataRequest) cachedValue;
        }

        ChannelMetadataEntity entity = dbReadTimer.record(() -> channelMetadataRepository.findByCountryCode(countryCode));
        dbReadCounter.increment();

        if (entity != null) {
            ChannelMetadataRequest model = mapEntityToModel(entity);
            customCacheWrapper.put(countryCode, model);
            log.info("Updated cache with database value for countryCode: {}", countryCode);
            return model;
        }

        log.warn("No channel metadata found for countryCode: {}", countryCode);
        return null;
    }

    private ChannelMetadataEntity convertRequestToEntity(ChannelMetadataRequest request) {
        ChannelMetadataEntity entity = new ChannelMetadataEntity();
        entity.setCountryCode(request.getCountryCode());
        entity.setProduct(request.getProduct());
        entity.setMetadata(convertToJson(request.getMetadata()));
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
