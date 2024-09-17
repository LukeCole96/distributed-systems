package com.app.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.util.retry.Retry;

import java.time.Duration;

@Slf4j
@Service
public class RetryCacheService {

    private static final Logger logger = LoggerFactory.getLogger(RetryCacheService.class);

    private final WebClient webClient;

    public RetryCacheService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.baseUrl("http://channel-metadata-store:8080").build();
    }

    @KafkaListener(topics = "retry-db-write-from-cache", groupId = "channel-metadata-group")
    public void consumeRetryCacheMessage(ConsumerRecord<String, String> record) {
        logger.info("Received message from retry-db-write-from-cache: {}", record.value());
        triggerChannelMetadataUpdate();
    }

    public void triggerChannelMetadataUpdate() {
            webClient.post() //uses webflux for non-blocking / reactive implementation
                    .uri("/api/channel-metadata/force-update-all")
                    .retrieve()
                    .toBodilessEntity()
                    .doOnSuccess(response -> log.info("Successfully triggered channel metadata update. Response: {}", response))
                    .doOnError(error -> log.error("Failed to trigger channel metadata update.", error))
                    .retryWhen(Retry.fixedDelay(1, Duration.ofSeconds(10))) // Retry once after 5 seconds on failure
                    .subscribe();
    }
}