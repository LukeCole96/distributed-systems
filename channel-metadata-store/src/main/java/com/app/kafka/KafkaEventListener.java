package com.app.kafka;

import com.app.cache.CacheUpdateEvent;
import com.app.entity.ChannelMetadataEntity;
import com.app.metrics.CustomCacheWrapper;
import com.app.model.ChannelMetadataRequest;
import com.app.repository.ChannelMetadataRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;

@Slf4j
@Component
public class KafkaEventListener {

    private final ChannelMetadataRepository channelMetadataRepository;
    private final CustomCacheWrapper customCacheWrapper;
    private final KafkaProducerService kafkaProducerService;

    public KafkaEventListener(ChannelMetadataRepository channelMetadataRepository, CustomCacheWrapper customCacheWrapper, KafkaProducerService kafkaProducerService) {
        this.channelMetadataRepository = channelMetadataRepository;
        this.customCacheWrapper = customCacheWrapper;
        this.kafkaProducerService = kafkaProducerService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMPLETION)
    public void onTransactionRollback(CacheUpdateEvent event) {
        try {
            ChannelMetadataEntity entity = convertRequestToEntity(event.getCountryCode(), event.getRequest());

            String message = String.format("Database update failed for key: %s, Metadata: %s", event.getCountryCode(), event.getRequest().getMetadata());
            customCacheWrapper.put(event.getCountryCode(), entity);

            kafkaProducerService.sendMessage("retry-db-write-from-cache", event.getCountryCode(), message);

        } catch (Exception e) {
            log.error("Error occurred while handling transaction rollback for countryCode: {}", event.getCountryCode(), e);
        }
    }

    private ChannelMetadataEntity convertRequestToEntity(String countryCode, ChannelMetadataRequest request) {
        ChannelMetadataEntity entity = new ChannelMetadataEntity();
        entity.setCountryCode(countryCode);
        entity.setProduct(request.getProduct());
        entity.setMetadata(convertToJson(request.getMetadata()));
        return entity;
    }

    private String convertToJson(Object obj) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            if (obj instanceof List<?>) {
                return mapper.writeValueAsString(obj);
            } else if (obj instanceof String) {
                return (String) obj;
            } else {
                throw new IllegalArgumentException("Unsupported type for JSON conversion");
            }
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to convert object to JSON", e);
        }
    }
}
