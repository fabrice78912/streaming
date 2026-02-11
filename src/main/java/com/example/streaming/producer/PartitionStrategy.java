package com.example.streaming.producer;

/**
 * Classe PartitionStrategy
 *
 * @author Fabrice
 * @version 1.0
 * @since 2026-02-11
 */
public class PartitionStrategy {

    public static int getPartitionFromCity(String jsonValue) {

        if (jsonValue.contains("\"ville\":\"Montréal\"")) {
            return 0;
        } else if (jsonValue.contains("\"ville\":\"Toronto\"")) {
            return 1;
        } else if (jsonValue.contains("\"ville\":\"Vancouver\"")) {
            return 2;
        } else if (jsonValue.contains("\"ville\":\"Calgary\"")) {
            return 3;
        } else {
            return 0;
        }
    }
}

