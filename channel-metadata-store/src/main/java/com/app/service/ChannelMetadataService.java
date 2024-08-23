package com.app.service;

import com.app.entity.ChannelMetadataEntity;
import com.app.model.ChannelMetadataRequest;
import com.app.repository.ChannelMetadataRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;


public class ChannelMetadataService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ChannelMetadataRepository channelMetadataRepository;

    @Autowired
    public ChannelMetadataService(RedisTemplate<String, Object> redisTemplate, ChannelMetadataRepository channelMetadataRepository) {
        this.redisTemplate = redisTemplate;
        this.channelMetadataRepository = channelMetadataRepository;
    }

    public void createChannelMetadata(String countryCode, ChannelMetadataRequest request) {
        // Construct the Redis key
        String redisKey = "countryData:" + countryCode;

        // Write-through: Write to Redis first
        redisTemplate.opsForValue().set(redisKey, request);

        // Then update the database
        ChannelMetadataEntity entity = new ChannelMetadataEntity(countryCode, request);
        channelMetadataRepository.save(entity);
    }
}