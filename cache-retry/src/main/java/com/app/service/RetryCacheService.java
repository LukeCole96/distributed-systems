package com.app.service;

import com.app.entity.DbDowntimeEntity;
import com.app.repository.DbDowntimeStoreRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;

@Slf4j
@Service
public class RetryCacheService {

    private static final Logger logger = LoggerFactory.getLogger(RetryCacheService.class);

    private final WebClient webClient;
    private final DbDowntimeStoreRepository downtimeRepo;
    private final Counter dbWriteCounter;
    private final Timer dbWriteTimer;

    public RetryCacheService(WebClient.Builder webClientBuilder, DbDowntimeStoreRepository downtimeRepo, MeterRegistry meterRegistry) {
        this.webClient = webClientBuilder.baseUrl("http://channel-metadata-store:8080").build();
        this.downtimeRepo = downtimeRepo;
        this.dbWriteCounter = meterRegistry.counter("db_write_total");
        this.dbWriteTimer = meterRegistry.timer("db_write_duration");
    }

    @KafkaListener(topics = "retry-db-write-from-cache", groupId = "channel-metadata-group")
    @CircuitBreaker(name = "dbService", fallbackMethod = "fallbackConsumeKafkaMessage")
    @Retry(name = "dbRetry")
    public void consumeKafkaMessage(String message) throws InterruptedException {
        try {
            log.info("Received Kafka message: {}", message);
            DbDowntimeEntity downtimeStore = new DbDowntimeEntity();

            String timestamp = extractTimestampFromLog(message);
            downtimeStore.setDowntimeTimestamp(timestamp);

            dbWriteTimer.record(() -> downtimeRepo.save(downtimeStore));

            dbWriteCounter.increment();

            log.info("Successfully updated database with downtime timestamp: {}", timestamp);
        } catch (Exception e) {
            log.error("Failed to update database with downtime timestamp.", e);
            throw e;
        }
    }

    public List<DbDowntimeEntity> getAllDowntimeLogs() {
        try {
            return downtimeRepo.findAll();
        } catch (Exception e) {
            log.error("Error occurred while fetching downtime logs", e);
            throw new RuntimeException("Failed to fetch downtime logs", e);
        }
    }

    @CircuitBreaker(name = "metadataService", fallbackMethod = "fallbackMetadataUpdate")
    public void triggerChannelMetadataUpdate() throws InterruptedException {
        webClient.post()
                .uri("/api/channel-metadata/force-update-all")
                .retrieve()
                .toBodilessEntity()
                .doOnSuccess(response -> log.info("Successfully triggered channel metadata update. Response: {}", response))
                .doOnError(error -> log.error("Failed to trigger channel metadata update.", error))
                .retryWhen(reactor.util.retry.Retry.fixedDelay(1, Duration.ofSeconds(5)))
                .subscribe();
    }

    private String extractTimestampFromLog(String logMessage) {
        int timestampIndex = logMessage.indexOf("Timestamp: ");
        if (timestampIndex != -1) {
            String timestamp = logMessage.substring(timestampIndex + 11, timestampIndex + 30);
            log.info("Extracted timestamp: {}", timestamp);
            return timestamp;
        }
        log.warn("Timestamp not found in log message: {}", logMessage);
        return null;
    }

    public void fallbackConsumeKafkaMessage(String message, Throwable t) {
        log.error("Circuit breaker opened for consumeKafkaMessage. Fallback executed. Message: {}, Error: {}", message, t.getMessage());
    }

    public Mono<Void> fallbackMetadataUpdate(Throwable t) {
        log.error("Circuit breaker opened for triggerChannelMetadataUpdate. Fallback executed. Error: {}", t.getMessage());
        return Mono.empty();
    }
}
