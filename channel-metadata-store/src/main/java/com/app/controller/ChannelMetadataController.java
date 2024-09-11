package com.app.controller;

import com.app.exceptions.GlobalExceptionHandler;
import com.app.model.ChannelMetadataRequest;
import com.app.service.ChannelMetadataService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/channel-metadata")
public class ChannelMetadataController {

    private final ChannelMetadataService channelMetadataService;

    @Autowired
    public ChannelMetadataController(ChannelMetadataService channelMetadataService) {
        log.info("ChannelMetadataController initialized");
        this.channelMetadataService = channelMetadataService;
    }

    @PostMapping("/{countryCode}")
    public ResponseEntity<String> saveCountryData(@PathVariable String countryCode,
                                                  @Valid @RequestBody ChannelMetadataRequest request) {
        log.info("Received request to save data for countryCode: {}", countryCode);

        if (countryCode == null || countryCode.trim().isEmpty()) {
            throw new GlobalExceptionHandler.BadRequestException("Country code cannot be empty");
        }

        try {
            channelMetadataService.saveOrUpdateChannelMetadata(countryCode, request);
            return ResponseEntity.ok("Data successfully posted");
        } catch (IllegalArgumentException e) {
            throw new GlobalExceptionHandler.BadRequestException("Invalid request data");
        } catch (GlobalExceptionHandler.ResourceNotFoundException e) {
            throw new GlobalExceptionHandler.ResourceNotFoundException("Country not found");
        } catch (GlobalExceptionHandler.MethodArgumentNotValidException e) {
            throw new GlobalExceptionHandler.MethodArgumentNotValidException(e.getMessage());
        } catch (Exception e) {
            throw new GlobalExceptionHandler.ConflictException("Timeout occurred");
        }
    }

    @GetMapping("/{countryCode}")
    public ResponseEntity<ChannelMetadataRequest> getCountryData(@PathVariable String countryCode) {
        log.info("Fetching data for countryCode: {}", countryCode);
        ChannelMetadataRequest channelData = channelMetadataService.getChannelMetadataByCountryCode(countryCode);

        if (channelData != null) {
            return ResponseEntity.ok(channelData);
        } else {
            throw new GlobalExceptionHandler.ResourceNotFoundException("Channel metadata not found for countryCode: " + countryCode);
        }
    }
}