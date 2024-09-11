package com.app.controller;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.DescribeClusterResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.util.Properties;

@Component
public class PrivateKafkaHealthcheck implements HealthIndicator {

    private final AdminClient kafkaAdminClient;

    public PrivateKafkaHealthcheck(@Value("${spring.kafka.bootstrap-servers}") String bootstrapServers) {
        Properties config = new Properties();
        config.put("bootstrap.servers", bootstrapServers);
        this.kafkaAdminClient = AdminClient.create(config);
    }

    @Override
    public Health health() {
        try {
            DescribeClusterResult describeCluster = kafkaAdminClient.describeCluster();
            if (describeCluster.nodes().get().isEmpty()) {
                return Health.down().withDetail("error", "No Kafka brokers available").build();
            } else {
                return Health.up().withDetail("brokers", describeCluster.nodes().get().size()).build();
            }
        } catch (Exception e) {
            return Health.down(e).withDetail("error", "Kafka is not reachable").build();
        }
    }
}
