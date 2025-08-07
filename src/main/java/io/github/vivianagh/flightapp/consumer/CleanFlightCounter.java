package io.github.vivianagh.flightapp.consumer;

import io.github.vivianagh.avro.Flight;
import io.github.vivianagh.flightapp.service.DailyFlightCounterService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class CleanFlightCounter {
    private final DailyFlightCounterService service;

    public CleanFlightCounter(DailyFlightCounterService service) {
        this.service = service;
    }

    @KafkaListener(topics = "${kafka.topic.flightsUpdate}", groupId = "daily-flight-counter")
    public void onCleanFlight(Flight flight) {
        service.incrementDailyCount();
    }
}
