package io.github.vivianagh.flightapp.consumer;

import io.github.vivianagh.avro.Flight;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class DLQConsumer {
    @KafkaListener(topics = "${kafka.topic.deadLetter}", groupId = "dlq-monitor")
    public void listenDlq(ConsumerRecord<String, Flight> record) {
        log.error("☠️ DLQ message received - key: {}, value: {}", record.key(), record.value());
    }
}
