package com.app.controller;

import com.app.model.ChannelMetadataRequest;
import com.app.service.ChannelMetadataService;
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

    // POST method remains unchanged
    @PostMapping("/{countryCode}")
    public ResponseEntity<String> saveCountryData(@PathVariable String countryCode, @RequestBody ChannelMetadataRequest request) {
        channelMetadataService.saveOrUpdateChannelMetadata(countryCode, request);
        return ResponseEntity.ok("Data successfully posted");
    }

    // Update GET method to fetch by countryCode
    @GetMapping("/{countryCode}")
    public ResponseEntity<ChannelMetadataRequest> getCountryData(@PathVariable String countryCode) {
        log.info("Fetching data for countryCode: {}", countryCode);
        ChannelMetadataRequest channelData = channelMetadataService.getChannelMetadataByCountryCode(countryCode);

        if (channelData != null) {
            return ResponseEntity.ok(channelData);
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}