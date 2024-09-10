package com.app.metrics;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import com.hazelcast.map.LocalMapStats;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class CustomCacheWrapper {

    private final HazelcastInstance hazelcastInstance;
    private final Counter cacheMissCounter;
    private final Counter cacheEvictionCounter;
    private final Counter cacheHits;

    public CustomCacheWrapper(HazelcastInstance hazelcastInstance, MeterRegistry meterRegistry) {
        this.hazelcastInstance = hazelcastInstance;
        this.cacheMissCounter = meterRegistry.counter("custom_cache_misses", "cache", "distributed-cache");
        this.cacheEvictionCounter = meterRegistry.counter("custom_cache_evictions", "cache", "distributed-cache");
        this.cacheHits = meterRegistry.counter("custom_cache_hits", "cache", "distributed-cache");
    }

    public Object get(String key) {
        IMap<String, Object> map = null;
        Object value = null;
        try {
            map = hazelcastInstance.getMap("distributed-cache");
            value = map.get(key);
            log.info("Successful getting CACHE KEY: " + key);
            cacheHits.increment();
            if (value == null) {
                log.info("LC: CACHE MISS, COUNTER SHOULD INCREMENT");
                cacheMissCounter.increment(); // Increment miss counter on cache miss
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
            cacheEvictionCounter.increment(); // Increment eviction counter on evict
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
            log.info("LC: Cache size: {}", size);
        } catch (Exception e) {
            log.error("Error retrieving cache size", e);
        }
        return size;
    }
}

