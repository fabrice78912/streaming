package com.example.streaming.consumer.service;

import com.example.streaming.consumer.repo.LedgerRepository;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * LedgerListener
 *
 * Chaque thread lit sa partition spécifique et traite les messages en parallèle
 * Commit manuel des offsets après traitement correct
 */
@Service
@Slf4j
@Transactional
public class LedgerListener {

    private static final String TOPIC = "bank-transactions";
    private static final String DLQ_TOPIC = "bank-transactions-dlq";

    private final ConsumerFactory<String, String> consumerFactory;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final LedgerRepository ledgerRepository;

    private static final DateTimeFormatter dtf =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS").withZone(ZoneId.systemDefault());

    public LedgerListener(ConsumerFactory<String, String> consumerFactory,
                          KafkaTemplate<String, String> kafkaTemplate,
                          LedgerRepository ledgerRepository) {
        this.consumerFactory = consumerFactory;
        this.kafkaTemplate = kafkaTemplate;
        this.ledgerRepository = ledgerRepository;

        startPartitionConsumers();
    }

    private void startPartitionConsumers() {
        int numPartitions = 4; // nombre de partitions
        ExecutorService executor = Executors.newFixedThreadPool(numPartitions);
        for (int partitionNumber = 0; partitionNumber < numPartitions; partitionNumber++) {
            final int p = partitionNumber;
            executor.submit(() -> consumePartition(p));
        }
    }

    private void consumePartition(int partitionNumber) {
        KafkaConsumer<String, String> consumer = (KafkaConsumer<String, String>) consumerFactory.createConsumer();
        consumer.assign(List.of(new TopicPartition(TOPIC, partitionNumber)));
        log.info("Thread pour partition {} démarré", partitionNumber);

        while (true) {
            var records = consumer.poll(Duration.ofMillis(100));
            for (ConsumerRecord<String, String> record : records) {
                processRecord(consumer, record, partitionNumber);
            }
        }
    }

    private void processRecord(KafkaConsumer<String, String> consumer,
                               ConsumerRecord<String, String> record,
                               int partitionNumber) {

        Instant startTime = Instant.now(); // ⏱ Début du traitement
        String startStr = dtf.format(startTime);

        String transactionId = record.key();
        String message = record.value();
        String city = getCityFromPartition(partitionNumber);

        log.info("[{}] Début traitement -> Transaction {} | Partition {} | Ville {}",
                startStr, transactionId, partitionNumber, city);

        // 1️⃣ Validation métier
        if (!isValidTransaction(message)) {
            log.warn("[{}] Transaction invalide pour {} : {}", startStr, city, transactionId);
            kafkaTemplate.send(DLQ_TOPIC, transactionId, message)
                    .whenComplete((metadata, ex) -> {
                        if (ex != null) log.error("Erreur envoi DLQ pour {}", transactionId, ex);
                        else log.info("[{}] Transaction envoyée DLQ -> {}", dtf.format(Instant.now()), transactionId);
                    });
            logEnd(transactionId, partitionNumber, city, startTime);
            return;
        }

        // 2️⃣ Idempotence
        if (ledgerRepository.exists(transactionId)) {
            log.info("[{}] Transaction déjà traitée: {}", startStr, transactionId);
            logEnd(transactionId, partitionNumber, city, startTime);
            return;
        }

        // 3️⃣ Sauvegarde en DB
        ledgerRepository.save(transactionId, message);
        log.info("[{}] Transaction enregistrée par {} | Partition {} | ID {}",
                startStr, city, partitionNumber, transactionId);

        // 4️⃣ Commit manuel après succès
        try {
            consumer.commitSync();
            log.info("[{}] Offset commit effectué pour Transaction {}", dtf.format(Instant.now()), transactionId);
        } catch (Exception e) {
            log.error("Erreur commit offset pour {}", transactionId, e);
        }

        // 5️⃣ Fin traitement
        logEnd(transactionId, partitionNumber, city, startTime);
    }

    private void logEnd(String transactionId, int partition, String city, Instant startTime) {
        Instant endTime = Instant.now();
        String endStr = dtf.format(endTime);
        long durationMs = endTime.toEpochMilli() - startTime.toEpochMilli();
        log.info("[{}] Fin traitement -> Transaction {} | Partition {} | Ville {} | Durée totale {} ms",
                endStr, transactionId, partition, city, durationMs);
    }

    private boolean isValidTransaction(String value) {
        if (value == null || value.isEmpty()) return false;
        return !value.contains("\"montant\":-"); // refuse montant négatif
    }

    private String getCityFromPartition(int partition) {
        return switch (partition) {
            case 0 -> "Montréal";
            case 1 -> "Toronto";
            case 2 -> "Vancouver";
            case 3 -> "Calgary";
            default -> "Inconnu";
        };
    }
}
