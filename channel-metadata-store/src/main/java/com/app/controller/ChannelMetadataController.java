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
        this.channelMetadataService = channelMetadataService;
    }

    @PostMapping("/{countryCode}")
    public ResponseEntity<String> saveCountryData(@PathVariable String countryCode, @RequestBody ChannelMetadataRequest request) {
        channelMetadataService.saveOrUpdateChannelMetadata(countryCode, request);
        return ResponseEntity.ok("Data successfully posted");
    }

    @GetMapping("/{id}")
    public ResponseEntity<String> getCountryData(@PathVariable String id) {
        log.info("hitttt");
        return ResponseEntity.ok("bleh");
    }
}