package com.app.metrics;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import com.hazelcast.map.LocalMapStats;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class CustomHazelcastMetrics {

    @Bean
    public CustomCacheWrapper registerHazelcastMetrics(HazelcastInstance hazelcastInstance, MeterRegistry meterRegistry) {
        log.debug("Initializing Hazelcast metrics");

        IMap<String, Object> map = hazelcastInstance.getMap("distributed-cache");
        if (map == null) {
            log.error("Hazelcast map 'distributed-cache' not found");
            return null;
        }
        log.debug("Hazelcast map 'distributed-cache' found");

        LocalMapStats stats = map.getLocalMapStats();
        log.debug("Retrieved LocalMapStats from Hazelcast map");

        try {
            meterRegistry.gauge("hazelcast_cache_size", stats, LocalMapStats::getOwnedEntryCount);
            log.debug("Registered gauge for cache size");

            meterRegistry.gauge("hazelcast_cache_get_latency", stats, LocalMapStats::getTotalGetLatency);
            log.debug("Registered gauge for cache get latency");

            meterRegistry.gauge("hazelcast_cache_put_latency", stats, LocalMapStats::getTotalPutLatency);
            log.debug("Registered gauge for cache put latency");

            meterRegistry.gauge("hazelcast_cache_hits", stats, LocalMapStats::getHits);
            log.debug("Registered gauge for cache hits");

        } catch (Exception e) {
            log.error("Error registering Hazelcast metrics with Micrometer", e);
        }

        return new CustomCacheWrapper(hazelcastInstance, meterRegistry);
    }
}
