package com.app.controller;

import com.app.controller.requesthelper.AuthValidator;
import com.app.exceptions.GlobalExceptionHandler;
import com.app.model.ChannelMetadataRequest;
import com.app.service.ChannelMetadataService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;
import java.util.regex.Pattern;

@Slf4j
@Validated
@RestController
@RequestMapping("/api/channel-metadata")
public class ChannelMetadataController {

    private final ChannelMetadataService channelMetadataService;
    private final AuthValidator authValidator;
    private static final Pattern UUID_PATTERN = Pattern.compile("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");

    @Autowired
    public ChannelMetadataController(ChannelMetadataService channelMetadataService, AuthValidator authValidator) {
        log.info("ChannelMetadataController initialized");
        this.channelMetadataService = channelMetadataService;
        this.authValidator = authValidator;
    }

    @PostMapping("/force-update-all")
    public ResponseEntity<String> forceUpdateAllFromCache(HttpServletRequest httpRequest) {
        log.info("Force the update of database with all cached metadata.");

        String requestId = getRequestIdOrGenerate(httpRequest);

        if (!authValidator.validate(httpRequest.getHeader("Authorization"))) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .header("request-id", requestId)
                    .body("Invalid or missing credentials");
        }

        try {
            channelMetadataService.forceUpdateAllFromCache();
            return ResponseEntity.ok()
                    .header("request-id", requestId)
                    .body("Successfully updated the database with all cached metadata.");
        } catch (GlobalExceptionHandler.ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .header("request-id", requestId)
                    .body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .header("request-id", requestId)
                    .body("Failed to update database: " + e.getMessage());
        }
    }

    @PostMapping("/{countryCode}")
    public ResponseEntity<String> saveCountryData(@PathVariable String countryCode,
                                                  @Valid @RequestBody ChannelMetadataRequest request, HttpServletRequest httpRequest) {
        log.info("Received request to save data for countryCode: {}", countryCode);
        String requestId = getRequestIdOrGenerate(httpRequest);

        if (!authValidator.validate(httpRequest.getHeader("Authorization"))) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .header("request-id", requestId)
                    .body("Invalid or missing credentials");
        }

        if (countryCode == null || countryCode.trim().isEmpty()) {
            throw new GlobalExceptionHandler.BadRequestException("Country code cannot be empty");
        }

        try {
            channelMetadataService.saveOrUpdateChannelMetadata(countryCode, request);
            return ResponseEntity.ok()
                    .header("request-id", requestId)
                    .body("Data successfully posted");
        } catch (IllegalArgumentException e) {
            throw new GlobalExceptionHandler.BadRequestException("Invalid request data");
        } catch (GlobalExceptionHandler.ResourceNotFoundException e) {
            throw new GlobalExceptionHandler.ResourceNotFoundException("Country not found");
        } catch (Exception e) {
            throw new GlobalExceptionHandler.ConflictException("Timeout occurred");
        }
    }

    @GetMapping("/{countryCode}")
    public ResponseEntity<?> getCountryData(@PathVariable String countryCode, HttpServletRequest httpRequest) {
        if (countryCode == null || countryCode.trim().isEmpty()) {
            throw new GlobalExceptionHandler.BadRequestException("Country code cannot be empty");
        }

        log.info("Fetching data for countryCode: {}", countryCode);

        String requestId = getRequestIdOrGenerate(httpRequest);

        if (!authValidator.validate(httpRequest.getHeader("Authorization"))) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .header("request-id", requestId)
                    .body("Invalid or missing credentials");
        }

        ChannelMetadataRequest channelData = channelMetadataService.getChannelMetadataByCountryCode(countryCode);

        if (channelData != null) {
            return ResponseEntity.ok()
                    .header("request-id", requestId)
                    .body(channelData);
        } else {
            throw new GlobalExceptionHandler.ResourceNotFoundException("Channel metadata not found for countryCode: " + countryCode);
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
