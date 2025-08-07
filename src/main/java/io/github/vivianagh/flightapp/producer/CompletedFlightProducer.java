package io.github.vivianagh.flightapp.producer;

import io.github.vivianagh.avro.Flight;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CompletedFlightProducer {

    private final KafkaTemplate<String, Flight> kafkaTemplate;

    public void sendCompletedFlight(Flight flight) {
        kafkaTemplate.send("flights-completed", flight.getIcao24().toString(), flight);
    }
}
