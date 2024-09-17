package com.app.controller;

import com.app.entity.DbDowntimeEntity;
import com.app.exceptions.GlobalExceptionHandler;
import com.app.model.ControllerResponse;
import com.app.service.RetryCacheService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class RetryCacheController {

    private final RetryCacheService retryCacheService;

    public RetryCacheController(RetryCacheService retryCacheService) {
        this.retryCacheService = retryCacheService;
    }

    @GetMapping("/trigger-cache-retry")
    public ResponseEntity<ControllerResponse> triggerCacheRetry() {
        try {
            retryCacheService.triggerChannelMetadataUpdate();
            ControllerResponse response = new ControllerResponse("CRS-1000", "Triggering a DB-write retry for channel-metadata-store");
            return new ResponseEntity<>(response, HttpStatus.OK);

        } catch (IllegalArgumentException e) {
            throw new GlobalExceptionHandler.BadRequestException("Invalid request");
        } catch (Exception e) {
            throw new GlobalExceptionHandler.ConflictException("Timeout occurred");
        }
    }

    @GetMapping("/get-downtime-logs")
    public ResponseEntity<List<DbDowntimeEntity>> getDowntimeLogs() {
        try {
            List<DbDowntimeEntity> downtimeLogs = retryCacheService.getAllDowntimeLogs();
            return new ResponseEntity<>(downtimeLogs, HttpStatus.OK);
        } catch (Exception e) {
            throw new GlobalExceptionHandler.ConflictException("Failed to fetch downtime logs");
        }
    }


}