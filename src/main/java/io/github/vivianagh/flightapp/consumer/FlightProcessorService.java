package io.github.vivianagh.flightapp.consumer;

import io.github.vivianagh.avro.Flight;
import io.github.vivianagh.flightapp.model.FlightData;
import io.github.vivianagh.flightapp.producer.CleanFlightProducer;
import io.github.vivianagh.flightapp.producer.CompletedFlightProducer;
import io.github.vivianagh.flightapp.producer.DeadLetterPublisher;
import io.github.vivianagh.flightapp.producer.FlightNearHomeProducer;
import io.github.vivianagh.flightapp.service.CallsignRegistry;
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

    //Message types we care about
    private static final int MSG_IDENTIFICATION     = 1; // callsign / ident
    private static final int MSG_POSITION           = 3; // position / altitude
    private static final int MSG_VELOCITY           = 4; // speed / track
    private static final int MSG_SURVEILLANCE_ALT   = 5;

    private final FlightStateStore stateStore;
    private final CleanFlightProducer cleanFlightProducer;
    private final FlightNearHomeProducer flightNearHomeProducer;
    private final CompletedFlightProducer completedFlightProducer; // kept for future "completed" pipeline
    private final DeadLetterPublisher deadLetterPublisher;
    private final CallsignRegistry registry;

    @Value("${kafka.topic.flightsUpdate}")    private String flightsUpdateTopic;
    @Value("${kafka.topic.flightsOverHome}")  private String flightsOverHomeTopic;
    @Value("${kafka.topic.completedFlights}") private String completedFlightsTopic;

    //Simple in-memory dedupe so we dont spam the over-home topic
    private final Set<String> seenNearHome = ConcurrentHashMap.newKeySet();

    @KafkaListener(topics = "${kafka.topic.rawFlights}", groupId = "flight-processor")
    public void listenRawFlights(ConsumerRecord<String, Flight> record) {
        LogHelper.logRecord(record, "📦 Received RAW flight");
        final Flight avro = record.value();
        final FlightData data = FlightData.fromAvro(avro);

        try {
            final int msgType = parseMsgType(data.getTransmissionType(), data.getIcao24());
            if (msgType == -1) return;
            switch (msgType) {
                case MSG_IDENTIFICATION -> handleIdentification(data);
                case MSG_POSITION, MSG_VELOCITY -> handleMotionMessage(data, avro);
                case MSG_SURVEILLANCE_ALT ->  cacheIdentIfAny(data);
                default -> LogHelper.logSkipped("⏭ Ignored unsupported MSG type " + msgType, data.getIcao24());
            }

        } catch (Exception ex) {
            // Any failure goes to DLQ with context
            deadLetterPublisher.sendToDlq(avro, ex.getMessage());
            LogHelper.logError("processing RAW flight to DLQ", ex);
        }
    }

    /* ------------------------- Handlers ------------------------- */

    /** MSG=1 — cache callsign (ident) for later fills and stop pipeline here. */
    private void handleIdentification(FlightData d) {
        final String cs = trim(d.getCallsign());
        if (cs != null) {
            registry.put(d.getIcao24(), cs);
            log.info("🆔 IDENT cached | icao24={} callsign={}", d.getIcao24(), cs);
        } else {
            LogHelper.logSkipped("IDENT without callsign", d.getIcao24());
        }
    }

    /** MSG=3/4 — update state, publish clean update, and maybe emit near-home event. */
    private void handleMotionMessage(FlightData d, Flight originalAvro) {
        // Fill callsign from the in-memory registry if missing
        backfillCallsignFromRegistry(d);

        // Update consolidated state (position/velocity/altitude/etc.)
        final Flight updated = stateStore.process(d);
        if (updated == null) {
            LogHelper.logSkipped("No update applied (state unchanged)", d.getIcao24());
            return;
        }

        // Publish normalized "clean flight" update
        cleanFlightProducer.sendCleanFlight(updated);
        LogHelper.logProduced(flightsUpdateTopic, updated.getIcao24().toString());

        // If this sample is near home and not on ground, emit a single event per ICAO (dedup)
        if (isNearHomeCandidate(d) && seenNearHome.add(d.getIcao24())) {
            final Flight toSend = ensureCallsignOnAvro(updated, originalAvro, d);
            flightNearHomeProducer.sendNearFlight(toSend);
            LogHelper.logProduced(flightsOverHomeTopic, d.getIcao24());
        }



        // (Optional) If you later add "completed flight" semantics in stateStore:
        // if (stateStore.isCompleted(d.getIcao24())) {
        //     completedFlightProducer.sendCompleted(toSend);
        //     LogHelper.logProduced(completedFlightsTopic, d.getIcao24());
        // }
    }

    private void cacheIdentIfAny(FlightData raw) {
        String cs = raw.getCallsign();
        if (cs != null && !cs.isBlank()) {
            String trimmed = cs.trim();
            registry.put(raw.getIcao24(), trimmed);
            log.info("🆔 IDENT cached | icao24={} callsign={}", raw.getIcao24(), trimmed);
        }
    }


    /* ------------------------- Helpers ------------------------- */

    /** Parse and log a nice message; return -1 when invalid. */
    private int parseMsgType(String rawType, String icao24) {
        try {
            int t = Integer.parseInt(rawType);
            log.info("🔍 MSG type={} | icao24={}", t, icao24);
            return t;
        } catch (Exception ex) {
            LogHelper.logSkipped("⚠️ Invalid transmissionType: " + rawType, icao24);
            return -1;
        }
    }

    /** If callsign is empty, try to fill it from the identification cache. */
    private void backfillCallsignFromRegistry(FlightData d) {
        if (trim(d.getCallsign()) != null) return;
        final String cached = registry.get(d.getIcao24());
        if (cached != null) {
            d.setCallsign(cached);
            log.debug("✍️ Callsign filled from registry | icao24={} callsign={}", d.getIcao24(), cached);
        }
    }

    /** Decide whether this sample qualifies for a one-shot "near home" event. */
    private boolean isNearHomeCandidate(FlightData d) {
        return d.getLatitude() != null
                && d.getLongitude() != null
                && Boolean.FALSE.equals(d.isOnGround())
                && LocationUtils.isNearHome(d.getLatitude(), d.getLongitude());
    }

    /**
     * Prefer the consolidated 'updated' avro; if for any reason it has no callsign
     * and the sample does, copy it (SpecificRecord is mutable).
     */
    private Flight ensureCallsignOnAvro(Flight updated, Flight originalAvro, FlightData d) {
        final Flight chosen = (updated != null ? updated : originalAvro);
        if (chosen.getCallsign() == null && d.getCallsign() != null) {
            chosen.setCallsign(d.getCallsign()); // CharSequence in Avro
        }
        return chosen;
    }

    private static String trim(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

}
