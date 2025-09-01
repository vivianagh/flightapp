package io.github.vivianagh.flightapp.service;

import io.github.vivianagh.flightapp.exception.ResourceNotFoundException;
import io.github.vivianagh.flightapp.model.DTO.FlightOverHomeDTO;
import io.github.vivianagh.flightapp.model.entity.FlightOverHomeEntity;
import io.github.vivianagh.flightapp.repository.FlightOverHomeRepository;
import io.github.vivianagh.flightapp.repository.FlightRouteCacheRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import org.springframework.lang.Nullable;

import static java.util.Map.entry;

@Service
@RequiredArgsConstructor
public class FlightOverHomeService {

    private final FlightOverHomeRepository repository;
    private final AirlineLookupService lookupService;
    private final FlightRouteCacheRepository routeCacheRepo; // ⬅️ NUEVO

    // ---------- Normalizador de callsign -> flightNumber (BAW123 -> BA123) ----------
    private static final Pattern CALLSIGN_RX = Pattern.compile("^([A-Z]{2,3})(\\d+).*");

    // Mapa ICAO->IATA (3 letras -> 2 letras) para prefijos de callsign
    private static final Map<String, String> ICAO_TO_IATA = Map.ofEntries(
            entry("BAW","BA"), entry("VIR","VS"), entry("EZY","U2"), entry("RYR","FR"),
            entry("WZZ","W6"), entry("EXS","LS"), entry("TOM","BY"), entry("IBE","IB"),
            entry("VLG","VY"), entry("AEA","UX"), entry("AFR","AF"), entry("KLM","KL"),
            entry("DLH","LH"), entry("SWR","LX"), entry("AUA","OS"), entry("SAS","SK"),
            entry("FIN","AY"), entry("LOT","LO"), entry("BEL","SN"),
            entry("THY","TK"), entry("QTR","QR"), entry("UAE","EK"), entry("ETD","EY"),
            entry("AAL","AA"), entry("UAL","UA"), entry("DAL","DL"), entry("ACA","AC"),
            entry("JBU","B6"), entry("AMX","AM"), entry("LAN","LA"), entry("AVA","AV"),
            entry("EWG","EW"),  // Eurowings: EWG45Z -> EW45
            entry("HVN","VN"),  // Vietnam Airlines: HVN51 -> VN51
            entry("BBC","BG"),
            entry("CLX","CV"),  // Cargolux
            entry("DHK","QY"),  // DHL Air UK
            entry("GTI","5Y"),  // Atlas Air
            entry("BTI","BT"),  // Air Baltic
            entry("BOO","B0")
    );

    public List<FlightOverHomeDTO> getAllFlightsOverHome() {
        // Trae 40 “completables” más recientes y después mapeá a 20
        var rows = repository.findLatestCompleteLike(40);
        if (rows.isEmpty()) {
            // fallback a puro “últimos” si no hay suficientes completables
            rows = repository.findLatest(40);
            if (rows.isEmpty()) {
                throw new ResourceNotFoundException("No se encontraron vuelos sobre casa.");
            }
        }

        // Mapear/enriquecer y quedarte con los primeros 20
        return rows.stream()
                .map(this::mapAndEnrich)
                .sorted(Comparator.comparing(FlightOverHomeDTO::getTimestamp).reversed())
                .limit(20)
                .toList();
    }

    private FlightOverHomeDTO mapAndEnrich(FlightOverHomeEntity e) {
        String callsign = trimOrNull(e.getCallsign());
        String icao24   = trimOrNull(e.getIcao24());

        // 1) Aerolínea amigable
        String airline = Optional.ofNullable(lookupService.getAirlineName(callsign, icao24))
                .filter(s -> !s.isBlank()).orElse("Unknown");

        // 2) Origen/destino: primero lo que ya tenga la fila
        String origin = e.getOrigin();
        String destination = e.getDestination();

        // 3) Si aún no hay, consulto cache persistente (sin pegarle a la API)
        if (origin == null && destination == null) {
            // 3.a) por flightNumber (callsign normalizado)
            String flightNumber = normalizeFlightNumber(callsign);
            if (flightNumber != null) {
                var rc = routeCacheRepo.findById(flightNumber).orElse(null);
                if (rc != null) {
                    origin = rc.getOrigin();
                    destination = rc.getDestination();
                }
            }

            // 3.b) por icao24 (último registro)
            if (origin == null && destination == null && icao24 != null) {
                var rc2 = routeCacheRepo.findFirstByIcao24OrderByUpdatedAtDesc(icao24.toUpperCase()).orElse(null);
                if (rc2 != null) {
                    origin = rc2.getOrigin();
                    destination = rc2.getDestination();
                }
            }
        }

        return FlightOverHomeDTO.builder()
                .icao24(icao24)
                .callsign(callsign)
                .airline(airline)
                .origin(origin)
                .destination(destination)
                .altitude(e.getAltitude())
                .speed(e.getSpeed())          // o null si tu entity no lo tiene
                .latitude(e.getLatitude())
                .longitude(e.getLongitude())
                .hasAlert(false)
                .hasEmergency(false)
                .timestamp(toIsoTimestamp(e))
                .build();
    }


    // ---------- Helpers ----------

    private static String trimOrNull(String s) {
        if (s == null) return null;
        var t = s.trim();
        return t.isEmpty() ? null : t;
    }

    @Nullable
    private static String normalizeFlightNumber(@Nullable String callsign) {
        if (callsign == null || callsign.isBlank()) return null;
        String cs = callsign.trim().toUpperCase().replaceAll("\\s+","");
        var m = CALLSIGN_RX.matcher(cs);
        if (!m.matches()) return null;
        String prefix = m.group(1);
        String digits = m.group(2);
        if (prefix.length() == 3) prefix = ICAO_TO_IATA.getOrDefault(prefix, prefix);
        return prefix + digits;
    }

    private String toIsoTimestamp(FlightOverHomeEntity e) {
        var dateStr = e.getLoggedDate();
        var timeStr = e.getLoggedTime();

        List<String> datePatterns = List.of("yyyy-MM-dd", "yyyy/MM/dd");
        List<String> timePatterns = List.of("HH:mm:ss.SSS", "HH:mm:ss");

        for (var dp : datePatterns) {
            for (var tp : timePatterns) {
                try {
                    var date = java.time.LocalDate.parse(dateStr, java.time.format.DateTimeFormatter.ofPattern(dp));
                    var time = java.time.LocalTime.parse(timeStr, java.time.format.DateTimeFormatter.ofPattern(tp));
                    var zdt  = java.time.ZonedDateTime.of(date, time, java.time.ZoneId.of("Europe/London"));
                    return zdt.toInstant().toString();
                } catch (Exception ignore) {}
            }
        }
        return (dateStr != null ? dateStr : "") + "T" + (timeStr != null ? timeStr : "");
    }


}
