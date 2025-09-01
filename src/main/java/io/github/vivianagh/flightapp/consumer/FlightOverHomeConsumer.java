package io.github.vivianagh.flightapp.consumer;


import io.github.vivianagh.avro.Flight;
import io.github.vivianagh.flightapp.model.FlightData;
import io.github.vivianagh.flightapp.model.entity.FlightOverHomeEntity;
import io.github.vivianagh.flightapp.producer.DeadLetterPublisher;
import io.github.vivianagh.flightapp.repository.FlightOverHomeRepository;
import io.github.vivianagh.flightapp.service.AirlineLookupService;
import io.github.vivianagh.flightapp.service.CallsignRegistry;
import io.github.vivianagh.flightapp.service.FlightsPerHourService;
import io.github.vivianagh.flightapp.service.RouteResolverService;
import io.github.vivianagh.flightapp.utils.LogHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class FlightOverHomeConsumer {


    private final FlightOverHomeRepository repository;
    private final DeadLetterPublisher deadLetterPublisher;
    private final FlightsPerHourService perHourService;
    private final CallsignRegistry registry;
    private final RouteResolverService routeResolver;
    private final AirlineLookupService airlineLookup;

    @Value("${kafka.topic.flightsOverHome}")
    private String flightsOverHomeTopic;

    @KafkaListener(topics = "${kafka.topic.flightsOverHome}", groupId = "flight-over-home")
    public void listen(ConsumerRecord<String, Flight> record) {
        LogHelper.logReceived(flightsOverHomeTopic, record.key());
        Flight flightAvro = record.value();

        try {
            final FlightData d = FlightData.fromAvro(flightAvro);

            // ---- 1) Basic normalization / validation ----
            final String icao24     = trim(d.getIcao24());
            final String loggedDate = d.getLoggedDate();
            final String loggedTime = d.getLoggedTime();
            Double speed      = d.getGroundSpeed();

            // ---- 2) Callsign completion using short-lived IDENT cache ----
            String callsign = normalizeAndCacheCallsign(icao24, d.getCallsign());

            // ---- 3) Upsert row (update only missing fields) ----
            FlightOverHomeEntity saved = upsertRow(d, icao24, callsign);
            Instant when = toInstant(loggedDate, loggedTime);
            var route = routeResolver.resolve(callsign, icao24, when);
            if (route != null && (route.origin() != null || route.destination() != null)) {
                // usar exactamente los strings que guardaste al insertar
                int n = repository.updateRouteForSample(
                        icao24,
                        d.getLoggedDate(),      // ej: "2025/08/31" si así lo guardaste
                        d.getLoggedTime(),      // ej: "16:33:57.544" (con o sin .SSS según tu columna)
                        route.origin(),
                        route.destination()
                );
                log.info("🏷️ route set {} -> {} for {} (rows updated = {})",
                        route.origin(), route.destination(), icao24, n);
            }

            LogHelper.logSavedToDb("flight_over_home", icao24);

            // ---- 4) Increment per-hour counter (idempotent in service layer) ----

            LocalDate londonDate = LocalDateTime.ofInstant(when, ZoneId.of("Europe/London")).toLocalDate();
            int londonHour = LocalDateTime.ofInstant(when, ZoneId.of("Europe/London")).getHour();
            //perHourService.incrementCount(londonDate, londonHour, icao24);

            // ---- 5) Resolve route (non-blocking cache warmup) if we have callsign ----
            if (callsign != null) {
                log.info("Resolving route: icao24={} callsign={} when={}", icao24, callsign, loggedDate + " " + loggedTime);
                routeResolver.resolve(callsign, icao24, when);
            }

        } catch (Exception e) {
            deadLetterPublisher.sendToDlq(flightAvro, e.getMessage());
            LogHelper.logError("processing flight over home", e);
        }
    }

    // ---------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------

    /** Put into cache if callsign arrived; otherwise try to complete from cache. */
    private String normalizeAndCacheCallsign(String icao24, String rawCallsign) {
        String cs = trim(rawCallsign);
        if (cs != null) {
            registry.put(icao24, cs);
            log.debug("Callsign cached from message: {} -> {}", icao24, cs);
            return cs;
        }
        String known = registry.get(icao24);
        if (known != null) {
            log.debug("Callsign filled from cache: {} -> {}", icao24, known);
        }
        return known;
    }

    /** Upsert semantics: if row exists for (icao24, loggedDate, loggedTime), only fill missing fields; otherwise insert. */
    private FlightOverHomeEntity upsertRow(FlightData d, String icao24, String callsign) {
        Optional<FlightOverHomeEntity> existingOpt =
                repository.findFirstByIcao24AndLoggedDateAndLoggedTime(icao24, d.getLoggedDate(), d.getLoggedTime());

        Double speed = d.getGroundSpeed();   // may be null (MSG=4 usually has it)
        Double lat   = d.getLatitude();
        Double lng   = d.getLongitude();
        Double alt   = d.getAltitude();

        if (existingOpt.isPresent()) {
            FlightOverHomeEntity e = existingOpt.get();

            // fill missing pieces only
            if (e.getCallsign()  == null && callsign != null) e.setCallsign(callsign);
            if (e.getAltitude()  == null && alt != null)      e.setAltitude(alt);
            if (e.getLatitude()  == null && lat != null)      e.setLatitude(lat);
            if (e.getLongitude() == null && lng != null)      e.setLongitude(lng);

            // speed is optional — update if we didn’t have it yet
            if (e.getSpeed() == null && speed != null) e.setSpeed(speed);

            // optional: airline friendly name (if you want to enrich here)
            if (e.getAirline() == null && callsign != null) {
                String airline = airlineLookup.lookupByCallsignPrefix(callsign);
                if (airline != null) e.setAirline(airline);
            }

            return repository.save(e);
        }

        // INSERT
        FlightOverHomeEntity entity = FlightOverHomeEntity.builder()
                .icao24(icao24)
                .callsign(callsign)
                .latitude(lat)
                .longitude(lng)
                .altitude(alt)
                .loggedDate(d.getLoggedDate())
                .loggedTime(d.getLoggedTime())
                .build();

        // set speed only if field exists (method below handles both cases)
        if (speed != null) setSpeed(entity, speed);

        // optional: airline based on callsign prefix
        if (callsign != null) {
            String airline = airlineLookup.lookupByCallsignPrefix(callsign);
            if (airline != null) entity.setAirline(airline);
        }

        return repository.save(entity);
    }

    /** Safe setter for speed to tolerate different field names in the entity (speed vs groundSpeed). */
    private void setSpeed(FlightOverHomeEntity e, Double val) {
        // If your entity has getSpeed()/setSpeed(Double)
        try {
            e.getClass().getMethod("setSpeed", Double.class).invoke(e, val);
            return;
        } catch (Exception ignore) { /* fallthrough */ }

        // Or if your entity has getGroundSpeed()/setGroundSpeed(Double)
        try {
            e.getClass().getMethod("setGroundSpeed", Double.class).invoke(e, val);
        } catch (Exception ignore) {
            // If neither exists, nothing to set. See migration note below.
            log.debug("Speed field not present on entity, skipping speed update");
        }
    }

    private boolean hasNoSpeed(FlightOverHomeEntity e) {
        try {
            Object v = e.getClass().getMethod("getSpeed").invoke(e);
            return v == null;
        } catch (Exception ignore) { /* fallthrough */ }
        try {
            Object v = e.getClass().getMethod("getGroundSpeed").invoke(e);
            return v == null;
        } catch (Exception ignore) { /* fallthrough */ }
        return true;
    }

    private static String trim(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    /** Accepts yyyy-MM-dd or yyyy/MM/dd and HH:mm:ss[.SSS]. Falls back to now if parsing fails. */
    private static Instant toInstant(String date, String time) {
        List<String> dp = List.of("yyyy-MM-dd", "yyyy/MM/dd");
        List<String> tp = List.of("HH:mm:ss.SSS", "HH:mm:ss");
        for (var dpat : dp) for (var tpat : tp) {
            try {
                var d = LocalDate.parse(date, DateTimeFormatter.ofPattern(dpat));
                var t = LocalTime.parse(time, DateTimeFormatter.ofPattern(tpat));
                return ZonedDateTime.of(d, t, ZoneId.of("Europe/London")).toInstant();
            } catch (Exception ignore) {}
        }
        return Instant.now();
    }
}
