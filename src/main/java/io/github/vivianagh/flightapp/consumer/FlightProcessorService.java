package io.github.vivianagh.flightapp.consumer;

import io.github.vivianagh.avro.Flight;
import io.github.vivianagh.flightapp.model.FlightData;
import io.github.vivianagh.flightapp.producer.CleanFlightProducer;
import io.github.vivianagh.flightapp.producer.CompletedFlightProducer;
import io.github.vivianagh.flightapp.producer.DeadLetterPublisher;
import io.github.vivianagh.flightapp.producer.FlightNearHomeProducer;
import io.github.vivianagh.flightapp.state.FlightState;
import io.github.vivianagh.flightapp.state.FlightStateStore;
import io.github.vivianagh.flightapp.utils.LocationUtils;
import io.github.vivianagh.flightapp.utils.LogHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class FlightProcessorService {

    private final FlightStateStore stateStore;
    private final CleanFlightProducer cleanFlightProducer;
    private final FlightNearHomeProducer flightNearHomeProducer;
    private final CompletedFlightProducer completedFlightProducer;
    private final DeadLetterPublisher deadLetterPublisher;

    @Value("${kafka.topic.flightsUpdate}")
    private String flightsUpdateTopic;

    @Value("${kafka.topic.flightsOverHome}")
    private String flightsOverHomeTopic;

    @Value("${kafka.topic.completedFlights}")
    private String completedFlightsTopic;

    // Record of aircraft already detected near home during current session
    private final Set<String> seenNearHome = ConcurrentHashMap.newKeySet();


    @KafkaListener(topics = "${kafka.topic.rawFlights}", groupId = "flight-processor")
    public void listenRawFlights(ConsumerRecord<String, Flight> record) {
        // 0. Log received record
        LogHelper.logRecord(record, "Received RAW flight");

        Flight flightAvro = record.value();
        FlightData data = FlightData.fromAvro(flightAvro);


        // 1. Process and update in-memory FlightState
        try {

            int type;
            try {
                type = Integer.parseInt(data.getTransmissionType());
                log.info("🔍 transmissionType raw value: " + data.getTransmissionType(), data.getIcao24());

            } catch (NumberFormatException e) {
                LogHelper.logSkipped("⚠️ Invalid transmissionType: " + data.getTransmissionType(), data.getIcao24());
                return;
            }

            if (type != 3 && type != 4) {
                LogHelper.logSkipped("⏭ Ignored MSG type " + type, data.getIcao24());
                return;
            }

            Flight updated = stateStore.process(data);
            if (updated == null) {
                LogHelper.logSkipped("No update applied", data.getIcao24());
                return;
            }
            // 2. Produce clean/consolidated flight state
            cleanFlightProducer.sendCleanFlight(updated);
            LogHelper.logProduced(flightsUpdateTopic, updated.getIcao24().toString());

            // 3. Detect if aircraft flew near your home
            if (data.getLatitude() != null && data.getLongitude() != null
                    && Boolean.FALSE.equals(data.isOnGround())
                    && LocationUtils.isNearHome(data.getLatitude(), data.getLongitude())
                    && seenNearHome.add(data.getIcao24())
            ) {

                flightNearHomeProducer.sendNearFlight(flightAvro);
                LogHelper.logProduced(flightsOverHomeTopic, data.getIcao24());

            }

        } catch (Exception e) {
            // 5. On error, send to DLQ
            deadLetterPublisher.sendToDlq(flightAvro, e.getMessage());
            LogHelper.logError("processing RAW flight to DLQ", e);
        }

    }
}
