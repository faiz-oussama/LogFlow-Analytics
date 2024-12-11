package com.example;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Properties;

public class KafkaLogProducer {
    private final KafkaProducer<String, String> producer;
    private final ObjectMapper objectMapper;
    private final String topic;

    public KafkaLogProducer(String bootstrapServers, String topic) {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());

        this.producer = new KafkaProducer<>(props);
        this.objectMapper = new ObjectMapper();
        this.topic = topic;
    }

    public void sendLog(LogEntry logEntry) {
        try {
            String jsonLog = objectMapper.writeValueAsString(logEntry);
            ProducerRecord<String, String> record = new ProducerRecord<>(topic, jsonLog);
            producer.send(record);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void close() {
        producer.close();
    }
}
