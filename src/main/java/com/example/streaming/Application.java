package com.example.streaming;

import com.example.streaming.producer.BankTransactionProducer;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.util.HashMap;
import java.util.Map;

@SpringBootApplication
public class Application {

	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}

	@Bean
	CommandLineRunner run(BankTransactionProducer producer) {
		return args -> {

			Map<String, String> transactions = new HashMap<>();
			transactions.put("1001", "{\"client\":\"Alice\",\"ville\":\"Montréal\",\"montant\":200,\"type\":\"DEPOSIT\"}");
			transactions.put("1002", "{\"client\":\"Bob\",\"ville\":\"Toronto\",\"montant\":50,\"type\":\"WITHDRAW\"}");
			transactions.put("1003", "{\"client\":\"Carol\",\"ville\":\"Vancouver\",\"montant\":150,\"type\":\"DEPOSIT\"}");
			transactions.put("1004", "{\"client\":\"Dave\",\"ville\":\"Calgary\",\"montant\":500,\"type\":\"TRANSFER\"}");
			transactions.put("1005", "{\"client\":\"Eve\",\"ville\":\"Montréal\",\"montant\":100,\"type\":\"WITHDRAW\"}");
			transactions.put("1006", "{\"client\":\"Eve\",\"ville\":\"Montréal\",\"montant\":-100,\"type\":\"WITHDRAW\"}");
			transactions.put("1007", "{\"client\":\"Gerard\",\"ville\":\"Calgary\",\"montant\":-8,\"type\":\"TRANSFER\"}");
			transactions.put("10011", "{\"client\":\"Test\",\"ville\":\"Vancouver\",\"montant\":18,\"type\":\"DEPOSIT\"}");
			transactions.put("10012", "{\"client\":\"kiki\",\"ville\":\"Montréal\",\"montant\":90,\"type\":\"DEPOSIT\"}");

			producer.sendTransactions(transactions);
		};
	}
}
