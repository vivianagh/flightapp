package io.github.vivianagh.flightapp.producer;

import io.github.vivianagh.avro.Flight;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CleanFlightProducer {
    private final KafkaTemplate<String, Flight > kafkaTemplate;

    public void sendCleanFlight(Flight flight) {


        kafkaTemplate.send("flights-update", flight.getIcao24().toString(), flight);
    }

}
