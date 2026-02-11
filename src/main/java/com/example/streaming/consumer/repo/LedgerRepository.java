package com.example.streaming.consumer.repo;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
/**
 * Classe ledgerRepository
 *
 * @author Fabrice
 * @version 1.0
 * @since 2026-02-11
 */


@Repository
public class LedgerRepository {

    private final JdbcClient jdbcClient;

    public LedgerRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public boolean exists(String transactionId) {
        Integer count = jdbcClient.sql("""
                SELECT COUNT(*) 
                FROM ledger_transactions 
                WHERE transaction_id = :id
                """)
                .param("id", transactionId)
                .query(Integer.class)
                .single();

        return count != null && count > 0;
    }

    public void save(String transactionId, String payload) {

        jdbcClient.sql("""
                INSERT INTO ledger_transactions (transaction_id, payload)
                VALUES (:id, :payload)
                """)
                .param("id", transactionId)
                .param("payload", payload)
                .update();
    }
}

