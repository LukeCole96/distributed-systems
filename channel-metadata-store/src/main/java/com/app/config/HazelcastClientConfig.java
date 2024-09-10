package com.app.config;

import com.hazelcast.client.HazelcastClient;
import com.hazelcast.client.config.ClientConfig;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.spring.cache.HazelcastCacheManager;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class HazelcastClientConfig {

    @Bean
    public HazelcastInstance hazelcastInstance() {
        ClientConfig clientConfig = new ClientConfig();
        clientConfig.setClusterName("dev");
        clientConfig.getNetworkConfig().addAddress("hazelcast:5701"); // Update with actual Hazelcast server addresses
        clientConfig.getSerializationConfig().setUseNativeByteOrder(true);
        clientConfig.getSerializationConfig().setAllowUnsafe(true);
        clientConfig.getSerializationConfig().setEnableCompression(false);
        return HazelcastClient.newHazelcastClient(clientConfig);
    }

    @Bean
    public CacheManager cacheManager(HazelcastInstance hazelcastInstance) {
        return new HazelcastCacheManager(hazelcastInstance);
    }
}