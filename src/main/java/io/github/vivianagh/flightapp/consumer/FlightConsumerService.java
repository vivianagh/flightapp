package io.github.vivianagh.flightapp.consumer;

import io.github.vivianagh.avro.Flight;
import io.github.vivianagh.flightapp.model.FlightData;
import io.github.vivianagh.flightapp.model.FlightEntity;
import io.github.vivianagh.flightapp.repository.FlightRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.apache.kafka.clients.consumer.ConsumerRecord;

@Service
@RequiredArgsConstructor
@Slf4j
public class FlightConsumerService {

    private final FlightRepository flightRepository;

    @KafkaListener(topics = "${kafka.topic.flight}", groupId = "flight-consumer-group")
    public void listen(ConsumerRecord<String, Flight> record) {
        Flight flightAvro = record.value();
        log.info("*******Received flight Avro: {}", flightAvro);

        FlightData flightData = FlightData.fromAvro(flightAvro);
        flightRepository.save(toEntity(flightData));
    }

    public static FlightEntity toEntity(FlightData data) {
        FlightEntity entity = new FlightEntity();
        entity.setIcao24(data.getIcao24());
        entity.setCallsign(data.getCallsign());
        entity.setAltitude(data.getAltitude());
        entity.setLatitude(data.getLatitude());
        entity.setLongitude(data.getLongitude());
        entity.setGroundSpeed(data.getGroundSpeed());
        entity.setSquawk(data.getSquawk());
        entity.setAlert(data.isAlert());
        entity.setEmergency(data.isEmergency());
        entity.setSpi(data.isSpi());
        entity.setIsOnGround(data.isOnGround());
        entity.setGeneratedDate(data.getGeneratedDate());
        entity.setGeneratedTime(data.getGeneratedTime());
        entity.setLoggedDate(data.getLoggedDate());
        entity.setLoggedTime(data.getLoggedTime());
        entity.setTransmissionType(data.getTransmissionType());
        return entity;
    }
}
