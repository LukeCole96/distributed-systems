package com.app.metrics;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class CustomCacheWrapper {

    private final HazelcastInstance hazelcastInstance;

    private final Counter cacheMissCounter;
    private final Counter cacheHitsCounter;
    private final Counter cacheEvictionCounter;

    public CustomCacheWrapper(HazelcastInstance hazelcastInstance, MeterRegistry meterRegistry) {
        this.hazelcastInstance = hazelcastInstance;
        this.cacheMissCounter = meterRegistry.counter("custom_cache_misses", "cache", "distributed-cache");
        this.cacheHitsCounter = meterRegistry.counter("custom_cache_hits", "cache", "distributed-cache");
        this.cacheEvictionCounter = meterRegistry.counter("custom_cache_evictions", "cache", "distributed-cache");
    }

    public Object get(String key) {
        IMap<String, Object> map = null;
        Object value = null;
        try {
            map = hazelcastInstance.getMap("distributed-cache");
            value = map.get(key);
            log.info("Successful getting CACHE KEY: " + key);
            cacheHitsCounter.increment();
            if (value == null) {
                log.info("Cache miss detected!");
                cacheMissCounter.increment();
            }
        } catch (Exception e) {
            log.error("Error accessing cache for key: {}", key, e);
        }
        return value;
    }

    public void put(String key, Object value) {
        try {
            IMap<String, Object> map = hazelcastInstance.getMap("distributed-cache");
            log.info("Adding value to cache! K/v is: {} {}", key, value);
            map.put(key, value);
        } catch (Exception e) {
            log.error("Error putting value into cache for key: {} value: {}", key, value, e);
        }
    }

    public void evict(String key) {
        try {
            IMap<String, Object> map = hazelcastInstance.getMap("distributed-cache");
            map.delete(key);
            cacheEvictionCounter.increment();
            log.info("Evicted key: {} from cache", key);
        } catch (Exception e) {
            log.error("Error evicting cache for key: {}", key, e);
        }
    }

    public long size() {
        long size = 0;
        try {
            IMap<String, Object> map = hazelcastInstance.getMap("distributed-cache");
            size = map.size();
            log.info("Cache size: {}", size);
        } catch (Exception e) {
            log.error("Error retrieving cache size", e);
        }
        return size;
    }
}

