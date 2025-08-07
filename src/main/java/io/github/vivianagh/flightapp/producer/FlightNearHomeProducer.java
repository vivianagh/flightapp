package io.github.vivianagh.flightapp.producer;

import io.github.vivianagh.avro.Flight;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class FlightNearHomeProducer {
    @Value("${kafka.topic.flightsOverHome}")
    private String flightsOverHomeTopic;

    private final KafkaTemplate<String, Flight> kafkaTemplate;

    public void sendNearFlight(Flight flight) {
        kafkaTemplate.send(flightsOverHomeTopic, flight.getIcao24().toString(), flight);
    }
}
