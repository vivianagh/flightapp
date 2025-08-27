package io.github.vivianagh.flightapp.service;

import io.github.vivianagh.flightapp.model.entity.FlightRouteCacheEntity;
import io.github.vivianagh.flightapp.repository.FlightRouteCacheRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.http.MediaType;
import org.springframework.lang.Nullable;
import org.springframework.web.client.RestClientResponseException;

import java.time.*;
import java.util.Map;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import static java.util.Map.entry;

@Slf4j
@Service
public class RouteResolverServiceImpl implements RouteResolverService {


    private final FlightRouteCacheRepository cacheRepo;

    private final RestClient http;
    private final String baseUrl;
    private final String apiKey;
    private final String host;

    // ---- caché en memoria ----
    private record CacheVal(String origin, String destination, Instant exp) {}
    private final Map<String, CacheVal> cache = new ConcurrentHashMap<>();
    private static final Duration TTL = Duration.ofHours(12);
    private static final ZoneId LONDON = ZoneId.of("Europe/London");

    // ---- normalizador (BAW123 → BA123) ----
    private static final Pattern CALLSIGN_RX = Pattern.compile("^([A-Z]{2,3})(\\d+).*");
    private static final Map<String, String> ICAO_TO_IATA = Map.ofEntries(
            entry("BAW","BA"), entry("VIR","VS"), entry("EZY","U2"), entry("RYR","FR"),
            entry("WZZ","W6"), entry("EXS","LS"), entry("TOM","BY"),
            entry("IBE","IB"), entry("VLG","VY"), entry("AEA","UX"),
            entry("AFR","AF"), entry("KLM","KL"), entry("DLH","LH"), entry("SWR","LX"),
            entry("AUA","OS"), entry("SAS","SK"), entry("FIN","AY"), entry("LOT","LO"),
            entry("BEL","SN"), entry("AEE","A3"),
            entry("THY","TK"), entry("QTR","QR"), entry("UAE","EK"), entry("ETD","EY"),
            entry("AAL","AA"), entry("UAL","UA"), entry("DAL","DL"), entry("ACA","AC"),
            entry("JBU","B6"), entry("AMX","AM"), entry("LAN","LA"), entry("AVA","AV"),
            entry("KAL","KE"),           // Korean Air
            entry("CFE","BA")
    );

    private final Map<String, Instant> cooldown = new ConcurrentHashMap<>();
    private static final Duration COOLDOWN_TTL = Duration.ofMinutes(45); // elige 15-60 min


    public RouteResolverServiceImpl(
            FlightRouteCacheRepository cacheRepo,
            @Value("${external.aerodatabox.baseUrl}") String baseUrl,
            @Value("${external.aerodatabox.apiKey:}") String apiKey,
            @Value("${external.aerodatabox.host:aerodatabox.p.rapidapi.com}") String host
    ) {
        this.cacheRepo = cacheRepo;
        this.baseUrl = baseUrl;
        this.apiKey = apiKey;
        this.host = host;

        this.http = RestClient.builder()
                .requestInterceptor((req, body, exec) -> {
                    HttpHeaders h = req.getHeaders();
                    h.setAccept(List.of(MediaType.APPLICATION_JSON));
                    if (!apiKey.isBlank()) {
                        h.set("x-rapidapi-key", apiKey);
                        h.set("x-rapidapi-host", host);
                    }
                    return exec.execute(req, body);
                })
                .build();

        log.info("AeroDataBox READY? {}", !apiKey.isBlank());
    }

    // -------- API interfaz --------
    @Override
    public @Nullable Route resolve(@Nullable String callsign, @Nullable String icao24, @Nullable Instant when) {
        return coreResolve(callsign, icao24, when);
    }

    @Override
    public @Nullable Route resolveFromCallsign(String callsign) {
        return coreResolve(callsign, null, null);
    }

    @Override
    public @Nullable Route resolveFromIcao24(String icao24, Instant when) {
        return coreResolve(null, icao24, when);
    }

    // -------- Lógica central --------
    private @Nullable Route coreResolve(@Nullable String callsign, @Nullable String icao24, @Nullable Instant when) {
        Instant now = Instant.now();
        String flightNumber = normalizeFlightNumber(callsign);

        log.debug("Resolve start — callsign={}, icao24={}, when={}, normalizedFN={}",
                callsign, icao24, when, flightNumber);

        // 1) Mem-cache por flightNumber (SOLO si tiene datos)
        if (flightNumber != null) {
            var cv = cache.get(flightNumber);
            if (cv != null && now.isBefore(cv.exp())) {
                log.debug("HIT mem-cache — fn={}, origin={}, dest={}", flightNumber, cv.origin(), cv.destination());
                return new Route(flightNumber, cv.origin(), cv.destination());
            }
            log.debug("MISS mem-cache — fn={}", flightNumber);
        }

        // 2) DB-cache por flightNumber (b)
        if (flightNumber != null) {
            var db = cacheRepo.findById(flightNumber).orElse(null);
            if (db != null) {
                boolean hasRoute = (db.getOrigin() != null) || (db.getDestination() != null);
                if (hasRoute) {
                    // ✅ solo consideramos HIT si trae datos
                    cache.put(flightNumber, new CacheVal(db.getOrigin(), db.getDestination(), now.plus(TTL)));
                    log.debug("HIT db by fn — fn={}, origin={}, dest={}, icao24(db)={}",
                            flightNumber, db.getOrigin(), db.getDestination(), db.getIcao24());
                    return new Route(flightNumber, db.getOrigin(), db.getDestination());
                } else {
                    log.debug("DB by fn is EMPTY — fn={}, seguimos a API", flightNumber);
                    // seguimos abajo a API para completar
                }
            } else {
                log.debug("MISS db by fn — fn={}", flightNumber);
            }
        }

        // 3) DB-cache por icao24 (fallback) — si no tengo flightNumber
        if (flightNumber == null && notBlank(icao24)) {
            var opt = cacheRepo.findFirstByIcao24OrderByUpdatedAtDesc(icao24.toUpperCase());
            if (opt.isPresent()) {
                var ent = opt.get();
                if (ent.getOrigin() != null || ent.getDestination() != null) {
                    cache.put(ent.getFlightNumber(), new CacheVal(ent.getOrigin(), ent.getDestination(), now.plus(TTL)));
                    log.debug("HIT db by icao24 — icao24={}, fn={}, origin={}, dest={}",
                            icao24, ent.getFlightNumber(), ent.getOrigin(), ent.getDestination());
                    return new Route(ent.getFlightNumber(), ent.getOrigin(), ent.getDestination());
                } else {
                    log.debug("DB by icao24 is EMPTY — icao24={}, continuamos", icao24);
                }
            } else {
                log.debug("MISS db by icao24 — icao24={}", icao24);
            }
        }

        // 4) API externa (c) — buscar día base y ±1 día
        String origin = null, destination = null;

        if (flightNumber != null && !apiKey.isBlank()) {
            var until = cooldown.get(flightNumber);
            if (until != null && Instant.now().isBefore(until)) {
                log.debug("COOLDOWN — fn={} hasta {}", flightNumber, until);
            } else {
                try {
                    LocalDate base = (when != null ? when.atZone(LONDON).toLocalDate() : LocalDate.now(LONDON));
                    for (LocalDate d : List.of(base, base.minusDays(1), base.plusDays(1))) {
                        String url = baseUrl + "/flights/number/" + flightNumber + "/" + d + "/" + d;
                        log.info("CALL API — fn={}, url={}", flightNumber, url);
                        var list = http.get().uri(url)
                                .retrieve()
                                .body(new org.springframework.core.ParameterizedTypeReference<List<Map<String,Object>>>() {});
                        var od = extractOD(list);
                        if (od.origin() != null || od.destination() != null) {
                            origin = od.origin();
                            destination = od.destination();
                            log.info("API result — fn={}, origin={}, dest={}, date={}", flightNumber, origin, destination, d);
                            break;
                        }
                    }
                } catch (RestClientResponseException ex) {
                    if (ex.getStatusCode().value() == 429) {
                        cooldown.put(flightNumber, Instant.now().plus(COOLDOWN_TTL));
                        log.warn("API 429 — fn={}; cooldown hasta {}", flightNumber, cooldown.get(flightNumber));
                    } else {
                        log.warn("API error — fn={}, status={}, msg={}", flightNumber, ex.getStatusCode(), ex.getMessage());
                    }
                } catch (Exception ex) {
                    // opcional: cooldown corto para otros errores
                    cooldown.put(flightNumber, Instant.now().plus(Duration.ofMinutes(10)));
                    log.warn("API error — fn={}, msg={}", flightNumber, ex.getMessage());
                }
            }
        } else {
            log.debug("SKIP API — fn={}, apiKeyPresent={}", flightNumber, !apiKey.isBlank());
        }

        // 5) Persistir SIEMPRE que haya flightNumber; cachear SOLO si hay datos (b)
        if (flightNumber != null && (origin != null || destination != null)) {
            cacheRepo.save(FlightRouteCacheEntity.builder()
                    .flightNumber(flightNumber)
                    .icao24(notBlank(icao24) ? icao24.toUpperCase() : null)
                    .origin(origin)
                    .destination(destination)
                    .updatedAt(now)
                    .build());
            // mem-cache solo si hay datos
            cache.put(flightNumber, new CacheVal(origin, destination, now.plus(TTL)));
            log.debug("SAVED db — fn={}, icao24={}, origin={}, dest={}", flightNumber, icao24, origin, destination);
            return new Route(flightNumber, origin, destination);
        }

        log.debug("Resolve end — no flightNumber and no db hit by icao24.");
        return null;
    }

    // -------- helpers --------
    private static boolean notBlank(@Nullable String s) {
        return s != null && !s.trim().isEmpty();
    }

    /** BAW123 -> BA123 (si prefijo 3 letras mapea ICAO→IATA). */
    private @Nullable String normalizeFlightNumber(@Nullable String callsign) {
        if (!notBlank(callsign)) return null;
        String cs = callsign.trim().toUpperCase().replaceAll("\\s+","");
        Matcher m = CALLSIGN_RX.matcher(cs);
        if (!m.matches()) return null;
        String prefix = m.group(1);
        String digits  = m.group(2);
        if (prefix.length() == 3) {
            prefix = ICAO_TO_IATA.getOrDefault(prefix, prefix);
        }
        return prefix + digits;
    }

    private record OD(String origin, String destination) {}

    @SuppressWarnings("unchecked")
    private OD extractOD(List<Map<String,Object>> list) {
        String orig = null, dest = null;
        if (list != null) {
            for (var f : list) {
                var dep = (Map<String,Object>) f.get("departure");
                var arr = (Map<String,Object>) f.get("arrival");
                if (dep != null && arr != null) {
                    var depA = (Map<String,Object>) dep.get("airport");
                    var arrA = (Map<String,Object>) arr.get("airport");
                    if (depA != null) orig = (String) depA.getOrDefault("iata", depA.get("icao"));
                    if (arrA != null) dest = (String) arrA.getOrDefault("iata", arrA.get("icao"));
                    if (orig != null || dest != null) break;
                }
            }
        }
        return new OD(orig, dest);
    }
}
