package io.github.vivianagh.flightapp.utils;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;

@Slf4j
public class LogHelper {
    public static void logProduced(String topic, String key) {
        log.info("✅ Produced to [{}] - key: {}", topic, key);
    }

    public static void logReceived(String topic, String key) {
        log.info("📩 Received from [{}] - key: {}", topic, key);
    }

    public static void logSavedToDb(String table, String key) {
        log.info("💾 Saved [{}] to DB - key: {}", table, key);
    }

    public static void logSkipped(String reason, String key) {
        log.warn("⏭ Skipped message - reason: {} - key: {}", reason, key);
    }

    public static void logError(String context, Exception e) {
        log.error("❌ Error during {} - {}", context, e.getMessage(), e);
    }

    // Extra: para loguear ConsumerRecord
    public static <K, V> void logRecord(ConsumerRecord<K, V> record, String action) {
        log.info("📦 {}: topic={}, partition={}, offset={}, key={}, value={}",
                action, record.topic(), record.partition(), record.offset(), record.key(), record.value());
    }
}
