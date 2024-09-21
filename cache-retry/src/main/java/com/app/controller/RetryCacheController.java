package com.app.controller;

import com.app.controller.requesthelper.AuthValidator;
import com.app.entity.DbDowntimeEntity;
import com.app.exceptions.GlobalExceptionHandler;
import com.app.model.ControllerResponse;
import com.app.service.RetryCacheService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

@RestController
public class RetryCacheController {

    private final RetryCacheService retryCacheService;
    private final AuthValidator authValidator;
    private final Counter kafkaMessageReadCounter;
    private final Counter dbReadCounter;
    private final Timer dbReadTimer;
    private static final Pattern UUID_PATTERN = Pattern.compile("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");

    public RetryCacheController(RetryCacheService retryCacheService, MeterRegistry meterRegistry, AuthValidator authValidator) {
        this.retryCacheService = retryCacheService;
        this.authValidator = authValidator;
        this.kafkaMessageReadCounter = meterRegistry.counter("kafka_messages_read_total");
        this.dbReadCounter = meterRegistry.counter("db_read_total");
        this.dbReadTimer = meterRegistry.timer("db_read_duration");
    }

    @GetMapping("/trigger-cache-retry")
    public ResponseEntity<ControllerResponse> triggerCacheRetry(HttpServletRequest httpRequest) {
        String requestId = getRequestIdOrGenerate(httpRequest);

        if (!authValidator.validate(httpRequest.getHeader("Authorization"))) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .header("request-id", requestId)
                    .body(new ControllerResponse("401", "Invalid or missing credentials"));
        }

        try {
            kafkaMessageReadCounter.increment();
            retryCacheService.triggerChannelMetadataUpdate();
            ControllerResponse response = new ControllerResponse("CRS-1000", "Triggering a DB-write retry for channel-metadata-store");
            return ResponseEntity.ok().header("request-id", requestId).body(response);

        } catch (IllegalArgumentException e) {
            throw new GlobalExceptionHandler.BadRequestException("Invalid request");
        } catch (Exception e) {
            throw new GlobalExceptionHandler.ConflictException("Timeout occurred");
        }
    }

    @GetMapping("/get-downtime-logs")
    public ResponseEntity<?> getDowntimeLogs(HttpServletRequest httpRequest) {
        String requestId = getRequestIdOrGenerate(httpRequest);

        try {
            if (!authValidator.validate(httpRequest.getHeader("Authorization"))) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .header("request-id", requestId)
                        .body("Invalid or missing credentials");
            }

            List<DbDowntimeEntity> downtimeLogs = dbReadTimer.record(() -> retryCacheService.getAllDowntimeLogs());
            dbReadCounter.increment();

            return ResponseEntity.ok().header("request-id", requestId).body(downtimeLogs);

        } catch (Exception e) {
            throw new GlobalExceptionHandler.ConflictException("Failed to fetch downtime logs");
        }
    }

    private String getRequestIdOrGenerate(HttpServletRequest request) {
        String requestId = request.getHeader("request-id");
        if (requestId == null || !UUID_PATTERN.matcher(requestId).matches()) {
            requestId = UUID.randomUUID().toString();
        }
        return requestId;
    }
}
