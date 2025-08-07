package io.github.vivianagh.flightapp.consumer;


import io.github.vivianagh.avro.Flight;
import io.github.vivianagh.flightapp.model.FlightData;
import io.github.vivianagh.flightapp.model.entity.FlightOverHomeEntity;
import io.github.vivianagh.flightapp.producer.DeadLetterPublisher;
import io.github.vivianagh.flightapp.repository.FlightOverHomeRepository;
import io.github.vivianagh.flightapp.service.FlightsPerHourService;
import io.github.vivianagh.flightapp.utils.LogHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.time.LocalTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class FlightOverHomeConsumer {

    private final FlightOverHomeRepository repository;
    private final DeadLetterPublisher deadLetterPublisher;
    private final FlightsPerHourService perHourService;

    @Value("${kafka.topic.flightsOverHome}")
    private String flightsOverHomeTopic;

    @KafkaListener(topics = "${kafka.topic.flightsOverHome}", groupId = "flight-over-home")
    public void listen(ConsumerRecord<String, Flight> record) {
        log.info("FlightOverHomeConsumer recibido");

        Flight flightAvro = record.value();
        LogHelper.logReceived(flightsOverHomeTopic, record.key());

        try {
            FlightData data = FlightData.fromAvro(flightAvro);

            FlightOverHomeEntity entity = FlightOverHomeEntity.builder()
                    .icao24(data.getIcao24())
                    .callsign(data.getCallsign())
                    .latitude(data.getLatitude())
                    .longitude(data.getLongitude())
                    .altitude(data.getAltitude())
                    .loggedDate(data.getLoggedDate())
                    .loggedTime(data.getLoggedTime())
                    .build();

            repository.save(entity);
            LogHelper.logSavedToDb("flight_over_home", entity.getIcao24());


        } catch (Exception e) {
            deadLetterPublisher.sendToDlq(flightAvro, e.getMessage());
            LogHelper.logError("processing flight over home", e);
        }
    }
}
