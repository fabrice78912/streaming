package com.example.streaming.producer;

import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Classe BankTransactionProducer
 *
 * @author Fabrice
 * @version 2.0
 * @since 2026-02-11
 */
@Slf4j
@Service
public class BankTransactionProducer {

    private static final String TOPIC = "bank-transactions";

    private final KafkaTemplate<String, String> kafkaTemplate;

    public BankTransactionProducer(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendTransactions(Map<String, String> transactions) {

        transactions.forEach((key, value) -> {

            int partition = PartitionStrategy.getPartitionFromCity(value);

            kafkaTemplate
                    .send(TOPIC, partition, key, value)
                    .whenComplete((result, ex) -> {

                        if (ex != null) {
                            log.error("❌ Failed to publish transaction [{}] to Kafka",
                                    key, ex);
                        } else {
                            log.info("✅ Transaction [{}] published to {} | partition={} offset={}",
                                    key,
                                    TOPIC,
                                    result.getRecordMetadata().partition(),
                                    result.getRecordMetadata().offset());
                        }
                    });
        });
    }
}
