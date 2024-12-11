package com.example;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;

import com.fasterxml.jackson.databind.ObjectMapper;

public class KafkaLogConsumer {
    private final KafkaConsumer<String, String> consumer;
    private final ObjectMapper objectMapper;
    private final BlockingQueue<LogEntry> logQueue;
    private volatile boolean running = true;
    private Thread consumerThread;

    public KafkaLogConsumer(String bootstrapServers, String groupId, String topic) {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");

        this.consumer = new KafkaConsumer<>(props);
        this.consumer.subscribe(Collections.singletonList(topic));
        this.objectMapper = new ObjectMapper();
        this.logQueue = new LinkedBlockingQueue<>();
    }

    public void start() {
        consumerThread = new Thread(() -> {
            try {
                while (running) {
                    ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100));
                    records.forEach(record -> {
                        try {
                            LogEntry logEntry = objectMapper.readValue(record.value(), LogEntry.class);
                            logQueue.put(logEntry);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    });
                }
            } finally {
                consumer.close();
            }
        });
        consumerThread.start();
    }

    public void stop() {
        running = false;
        if (consumerThread != null) {
            try {
                consumerThread.join(5000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        if (consumer != null) {
            consumer.close();
        }
        logQueue.clear();
    }

    public List<LogEntry> getNewLogs() {
        List<LogEntry> newLogs = new ArrayList<>();
        logQueue.drainTo(newLogs);
        return newLogs;
    }

    public BlockingQueue<LogEntry> getLogQueue() {
        return logQueue;
    }
}