package io.github.vivianagh.flightapp.producer;

import io.github.vivianagh.avro.Flight;
import io.github.vivianagh.flightapp.model.entity.DeadLetterEntity;
import io.github.vivianagh.flightapp.repository.DeadLetterRepository;
import io.github.vivianagh.flightapp.utils.JsonUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class DeadLetterPublisher {

    private final KafkaTemplate<String, Flight> kafkaTemplate;
    private final DeadLetterRepository deadLetterRepository;

    @Value("${kafka.topic.deadLetter}")
    private String deadLetterTopic;

    public void sendToDlq(Flight flight, String reason) {
        // 1. Send to DLQ topic in Kafka
        kafkaTemplate.send(deadLetterTopic, flight.getIcao24().toString(), flight);
        log.warn("☠️ Sent to DLQ - ICAO24: {}, reason: {}", flight.getIcao24(), reason);

        // 2. Save failed event in the database
        DeadLetterEntity entity = DeadLetterEntity.builder()
                .topic(deadLetterTopic)
                .key(flight.getIcao24().toString())
                .payload(JsonUtils.toJson(flight)) // JSON serialized version of the Avro message
                .error(reason)
                .timestamp(LocalDateTime.now())
                .build();

        deadLetterRepository.save(entity);
        log.info("💾 Saved DLQ event to DB - key: {}", entity.getKey());
    }

}
