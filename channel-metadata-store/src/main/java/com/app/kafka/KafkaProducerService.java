package com.app.kafka;

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

    public void sendMessage(String topic, String key, String value) {
        log.info("Attempting to send message to topic: {} with key: {} and value: {}", topic, key, value);
        ProducerRecord<String, String> record = new ProducerRecord<>(topic, key, value);
        producer.send(record, (metadata, exception) -> {
            if (exception != null) {
                log.info("Failed to send message: " + exception.getMessage());
            } else {
                log.info("Message sent to topic " + topic + " with key " + key);
            }
        });
    }

    public void close() {
        producer.close();
    }
}