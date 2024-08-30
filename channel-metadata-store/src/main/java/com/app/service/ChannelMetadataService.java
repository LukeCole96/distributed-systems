package com.app.service;

import com.app.entity.ChannelMetadataEntity;
import com.app.model.ChannelMetadataRequest;
import com.app.repository.ChannelMetadataRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ChannelMetadataService {

    private final ChannelMetadataRepository channelMetadataRepository;

    @Autowired
    public ChannelMetadataService(ChannelMetadataRepository channelMetadataRepository) {
        this.channelMetadataRepository = channelMetadataRepository;
    }

    @CachePut(value = "distributed-cache", key = "#entity.id")
    @Transactional
    public void saveOrUpdateChannelMetadata(String countryCode, ChannelMetadataRequest request) {
        // Write-through: Hazelcast automatically handles the cache update
        ChannelMetadataEntity entity = new ChannelMetadataEntity(countryCode, request);
        channelMetadataRepository.save(entity);
    }

    @Cacheable(value = "distributed-cache", key = "#id")
    public ChannelMetadataEntity getChannelMetadataById(Long id) {
        return channelMetadataRepository.findById(id).orElse(null);  // Returns null if not found
    }
}