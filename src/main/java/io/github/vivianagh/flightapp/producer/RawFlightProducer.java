package io.github.vivianagh.flightapp.producer;

import io.github.vivianagh.flightapp.model.FlightData;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import io.github.vivianagh.avro.Flight;

@Service
@RequiredArgsConstructor
public class RawFlightProducer {

    private final KafkaTemplate<String,Flight> kafkaTemplate;

    @Value("${kafka.topic.rawFlights}")
    private String flightTopic;

    public void sendRawFlight(FlightData data) {
        Flight flightAvro = Flight.newBuilder()
                .setIcao24(data.getIcao24())
                .setCallsign(data.getCallsign())
                .setAltitude(data.getAltitude())
                .setLatitude(data.getLatitude())
                .setLongitude(data.getLongitude())
                .setGroundSpeed(data.getGroundSpeed())
                .setSquawk(data.getSquawk())
                .setAlert(data.isAlert())
                .setEmergency(data.isEmergency())
                .setSpi(data.isSpi())
                .setIsOnGround(data.isOnGround())
                .setGeneratedDate(data.getGeneratedDate())
                .setGeneratedTime(data.getGeneratedTime())
                .setLoggedDate(data.getLoggedDate())
                .setLoggedTime(data.getLoggedTime())
                .setTransmissionType(data.getTransmissionType())
                .build();
        kafkaTemplate.send(flightTopic, data.getIcao24(), flightAvro);
    }

}
