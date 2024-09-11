package com.app.kafka;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.util.Properties;

@Slf4j
@Service
public class KafkaProducerService {

    private final KafkaProducer<String, String> producer;

    public KafkaProducerService(@Value("${spring.kafka.bootstrap-servers}") String bootstrapServers) {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        producer = new KafkaProducer<>(props);
    }

    // Circuit breaker and retry mechanism
    @CircuitBreaker(name = "kafkaService", fallbackMethod = "fallbackSendMessage")
    @Retry(name = "kafkaRetry")
    public void sendMessage(String topic, String key, String value) {
        log.info("Attempting to send message to topic: {} with key: {} and value: {}", topic, key, value);
        ProducerRecord<String, String> record = new ProducerRecord<>(topic, key, value);
        producer.send(record, (metadata, exception) -> {
            if (exception != null) {
                log.error("Failed to send message: {}", exception.getMessage());
            } else {
                log.info("Message sent to topic {} with key {}", topic, key);
            }
        });
    }

    public void fallbackSendMessage(String topic, String key, String value, Throwable t) {
        log.error("Kafka is down. Fallback method called. Message could not be sent: {}", value, t);
    }

    public void close() {
        producer.close();
    }
}