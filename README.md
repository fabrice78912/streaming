# 📌 Kafka Streaming – Producer / Consumer avec 4 partitions et DLQ

## 🏗 Architecture

```
Producer  --->  Topic: bank-transactions (4 partitions)
                      |
                      |-- Partition 0 → Thread 1 (Montréal)
                      |-- Partition 1 → Thread 2 (Toronto)
                      |-- Partition 2 → Thread 3 (Vancouver)
                      |-- Partition 3 → Thread 4 (Calgary)
                      
Messages invalides  --->  bank-transactions-dlq
```

---

## 🎯 Objectif

* Distribuer les messages sur **4 partitions**
* Avoir **1 thread dédié par partition**
* Traiter les messages **en parallèle**
* Faire un **commit manuel uniquement après succès**
* Envoyer les messages invalides vers une **Dead Letter Queue (DLQ)**
* Mesurer :

    * Heure de début
    * Heure de fin
    * Temps total de traitement

---

# 🧱 1️⃣ Création des Topics

Créer le topic principal avec **4 partitions** :

```bash
kafka-topics.sh \
  --create \
  --topic bank-transactions \
  --bootstrap-server localhost:9092 \
  --partitions 4 \
  --replication-factor 1
```

Créer la DLQ :

```bash
kafka-topics.sh \
  --create \
  --topic bank-transactions-dlq \
  --bootstrap-server localhost:9092 \
  --partitions 4 \
  --replication-factor 1
```

---

# 🚀 2️⃣ Producer

Le producer envoie les transactions avec une clé (`transactionId`) afin d’assurer la cohérence de partition.

Exemple :

```json
{
  "transactionId": "TX123",
  "montant": 100,
  "ville": "Montreal"
}
```

Si le montant est négatif, le message sera envoyé vers la DLQ par le consumer.

---

# ⚙️ 3️⃣ Consumer – Fonctionnement

## 🔹 4 Threads – 4 Partitions

Au démarrage :

```java
int numPartitions = 4;
ExecutorService executor = Executors.newFixedThreadPool(numPartitions);
```

Chaque thread :

```java
TopicPartition partition = new TopicPartition(TOPIC, partitionNumber);
consumer.assign(List.of(partition));
```

Cela garantit :

* 1 partition = 1 thread
* Traitement parallèle réel
* Pas de compétition entre threads

---

# 🔄 4️⃣ Commit Manuel

Auto-commit désactivé :

```java
config.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
```

Commit après succès :

```java
consumer.commitSync();
```

👉 Si le service crash avant commit → message rejoué
👉 Si commit réussi → message marqué comme consommé

---

# 📦 5️⃣ Logique de Traitement

Pour chaque message :

1. ⏱ Capture heure de début
2. ✅ Validation métier
3. 💾 Sauvegarde en base
4. 📌 Commit offset
5. ⏱ Capture heure de fin
6. 🧮 Calcul durée totale

Exemple de log :

```
[2026-02-12 14:32:10.123] Début traitement -> Transaction TX123 | Partition 0 | Ville Montréal
[2026-02-12 14:32:10.140] Transaction enregistrée
[2026-02-12 14:32:10.142] Offset commit effectué
[2026-02-12 14:32:10.145] Fin traitement -> Durée totale 22 ms
```

---

# ❌ 6️⃣ Gestion des Messages Invalides (DLQ)

Condition :

```java
if (!value.contains("\"montant\":-"))
```

Si invalide :

```java
kafkaTemplate.send("bank-transactions-dlq", key, value);
```

Les messages invalides ne sont pas insérés en base.

---

# 🧵 7️⃣ Parallélisme

✔ Chaque partition est consommée indépendamment
✔ Chaque thread poll simultanément
✔ Le traitement est réellement parallèle
✔ Les offsets sont commit indépendamment

---

# 🛡 8️⃣ Idempotence

Avant insertion :

```java
if (!ledgerRepository.exists(transactionId))
```

Évite les doublons si Kafka rejoue un message.

---

# 📊 9️⃣ Mesure du Temps de Traitement

Calcul :

```java
long durationMs = endTime.toEpochMilli() - startTime.toEpochMilli();
```

Permet de mesurer :

* Temps total du traitement métier
* Performance par partition
* Performance par ville

---

# 🏎 10️⃣ Lancement

### Démarrer Kafka

### Lancer le producer

### Lancer le consumer :

```bash
mvn spring-boot:run
```

---

# 📌 Résumé Technique

| Élément              | Implémentation      |
| -------------------- | ------------------- |
| Topic                | 4 partitions        |
| Consumer             | 4 threads           |
| Assignation          | Manuelle (assign)   |
| Commit               | Manuel (commitSync) |
| DLQ                  | Oui                 |
| Idempotence          | Oui                 |
| Traitement parallèle | Oui                 |
| Mesure performance   | Oui                 |

---

# 🏁 Conclusion

Cette implémentation respecte les bonnes pratiques production :

* Isolation par partition
* Contrôle total des offsets
* Résilience aux crash
* DLQ pour gestion d’erreurs
* Traitement parallèle scalable
* Mesure de performance intégrée
