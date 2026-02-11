package com.example.streaming.config;


import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaAdmin;

import java.util.HashMap;
import java.util.Map;

/**
 * Classe KafkatopicConfig
 *
 * @author Fabrice
 * @version 1.0
 * @since 2026-02-11
 */

@Configuration
public class KafkaTopicConfig {

    private static final String TOPIC = "bank-transactions";

    @Bean
    public KafkaAdmin kafkaAdmin() {
        Map<String, Object> configs = new HashMap<>();
        configs.put(org.apache.kafka.clients.admin.AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        return new KafkaAdmin(configs);
    }

    @Bean
    public NewTopic bankTransactionsTopic() {
        return new NewTopic(TOPIC, 4, (short) 1); // 4 partitions, 1 réplica
    }

    @Bean
    public NewTopic bankTransactionsDLQTopic() {
        return new NewTopic("bank-transactions-dlq", 4, (short) 1); // même logique pour DLQ
    }
}
