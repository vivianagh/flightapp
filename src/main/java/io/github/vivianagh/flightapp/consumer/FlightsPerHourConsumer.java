package io.github.vivianagh.flightapp.consumer;

import io.github.vivianagh.avro.Flight;
import io.github.vivianagh.flightapp.service.FlightsPerHourService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
@Slf4j
public class FlightsPerHourConsumer {

    private final FlightsPerHourService service;

    @KafkaListener(topics = "${kafka.topic.flightsOverHome}", groupId = "flights-per-hour")
    public void listen(ConsumerRecord<String, Flight> record) {
        Flight flight = record.value();

        try {
            String dateStr = flight.getLoggedDate().toString();
            String timeStr = flight.getLoggedTime().toString();

            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy/MM/dd");
            DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

            LocalDate date = LocalDate.parse(dateStr, dateFormatter);
            int hour = LocalTime.parse(timeStr, timeFormatter).getHour();

            service.incrementCount(date, hour, flight.getIcao24().toString());
            log.info("✅ Counted flight at hour {} on {}", hour, date);

        } catch (Exception e) {
            log.error("❌ Failed to process flight for hourly stats", e);
        }
    }
}
