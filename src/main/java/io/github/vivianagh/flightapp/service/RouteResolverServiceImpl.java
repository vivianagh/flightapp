package io.github.vivianagh.flightapp.service;

import io.github.vivianagh.flightapp.exception.RateLimitException;
import io.github.vivianagh.flightapp.model.entity.FlightRouteCacheEntity;
import io.github.vivianagh.flightapp.repository.FlightRouteCacheRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.http.MediaType;
import org.springframework.lang.Nullable;
import org.springframework.web.client.RestClientResponseException;

import java.time.*;
import java.time.format.DateTimeParseException;
import java.util.Map;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import static java.util.Map.entry;


@Slf4j
@Service
public class RouteResolverServiceImpl implements RouteResolverService {

    private final FlightRouteCacheRepository cacheRepo;

    // HTTP / API
    private final RestClient http;
    private final String baseUrl;
    private final String apiKey;
    private final String host;

    @Value("${routes.cooldown-seconds:3600}")
    private long defaultCooldownSeconds;

    // mem-cache (solo con datos)
    private record CacheVal(String origin, String destination, Instant exp) {}
    private final Map<String, CacheVal> memCache = new ConcurrentHashMap<>();
    private static final Duration TTL = Duration.ofHours(12);

    // zonas / fechas
    private static final ZoneId LONDON = ZoneId.of("Europe/London");

    // normalizador ICAO→IATA
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
            entry("KAL","KE"), entry("CFE","BA")
    );

    // rate limit
    private final Map<String, Instant> perFlightCooldown = new ConcurrentHashMap<>();
    private static final Duration COOLDOWN_TTL = Duration.ofMinutes(45);
    private final AtomicReference<Instant> globalCooldownUntil = new AtomicReference<>(Instant.EPOCH);

    @Autowired
    public RouteResolverServiceImpl(
            FlightRouteCacheRepository cacheRepo,
            @Value("${external.aerodatabox.baseUrl}") String baseUrl,
            @Value("${external.aerodatabox.apiKey:}") String apiKey,
            @Value("${external.aerodatabox.host:aerodatabox.p.rapidapi.com}") String host
    ) {
        this.cacheRepo = cacheRepo;
        this.baseUrl = baseUrl;
        this.apiKey = apiKey == null ? "" : apiKey;
        this.host = host;

        this.http = RestClient.builder()
                .requestInterceptor((req, body, exec) -> {
                    HttpHeaders h = req.getHeaders();
                    h.setAccept(List.of(MediaType.APPLICATION_JSON));
                    if (!this.apiKey.isBlank()) {
                        h.set("x-rapidapi-key", this.apiKey);
                        h.set("x-rapidapi-host", this.host);
                    }
                    return exec.execute(req, body);
                })
                .build();

        log.info("AeroDataBox READY? {}", !this.apiKey.isBlank());
    }

    // ---- RouteResolverService ----
    @Override
    public boolean isCoolingDown() {
        return Instant.now().isBefore(globalCooldownUntil.get());
    }

    @Override
    public @Nullable Route resolveFromCallsign(String callsign) throws RateLimitException{
        return coreResolve(callsign, null, null);
    }

    @Override
    public @Nullable Route resolveFromIcao24(String icao24, Instant when) throws RateLimitException{
        return coreResolve(null, icao24, when);
    }

    /** Overload que el Job usa: toma callsign + fecha y devuelve RouteResult, tirando RateLimitException. */
    @Override
    public RouteResult resolve(String rawCallsign, LocalDate flightDate) throws RateLimitException {
        if (isCoolingDown()) throw new RateLimitException("Cooling down");
        // usamos el mediodía local para evitar problemas de zonas
        Instant when = (flightDate != null)
                ? flightDate.atTime(12, 0).atZone(LONDON).toInstant()
                : null;

        Route r = coreResolve(rawCallsign, null, when);
        String fn = normalizeFlightNumber(rawCallsign);
        if (r != null) {
            return new RouteResult(r.flightNumber(), r.origin(), r.destination(), true);
        }
        return new RouteResult(fn, null, null, false);
    }

    // ---- core ----
    private @Nullable Route coreResolve(@Nullable String callsign, @Nullable String icao24, @Nullable Instant when)
            throws RateLimitException {
        final Instant now = Instant.now();

        if (isCoolingDown()) {
            log.debug("GLOBAL COOLDOWN activo hasta {}", globalCooldownUntil.get());
            throw new RateLimitException("Cooling down");
        }

        final String flightNumber = normalizeFlightNumber(callsign);
        log.debug("Resolve start — callsign={}, icao24={}, when={}, normalizedFN={}",
                callsign, icao24, when, flightNumber);

        // 1) mem-cache
        if (flightNumber != null) {
            var cv = memCache.get(flightNumber);
            if (cv != null && now.isBefore(cv.exp())) {
                log.debug("HIT mem-cache — fn={}, origin={}, dest={}", flightNumber, cv.origin(), cv.destination());
                return new Route(flightNumber, cv.origin(), cv.destination());
            }
            log.debug("MISS mem-cache — fn={}", flightNumber);
        }

        // 2) DB by fn
        if (flightNumber != null) {
            var db = cacheRepo.findById(flightNumber).orElse(null);
            if (db != null) {
                boolean hasRoute = (db.getOrigin() != null) || (db.getDestination() != null);
                if (hasRoute) {
                    memCache.put(flightNumber, new CacheVal(db.getOrigin(), db.getDestination(), now.plus(TTL)));
                    log.debug("HIT db by fn — fn={}, origin={}, dest={}, icao24(db)={}",
                            flightNumber, db.getOrigin(), db.getDestination(), db.getIcao24());
                    return new Route(flightNumber, db.getOrigin(), db.getDestination());
                } else {
                    log.debug("DB by fn is EMPTY — fn={}, seguimos a API", flightNumber);
                }
            } else {
                log.debug("MISS db by fn — fn={}", flightNumber);
            }
        }

        // 3) DB by icao24 (solo si no tengo fn)
        if (flightNumber == null && notBlank(icao24)) {
            var opt = cacheRepo.findFirstByIcao24OrderByUpdatedAtDesc(icao24.toUpperCase());
            if (opt.isPresent()) {
                var ent = opt.get();
                if (ent.getOrigin() != null || ent.getDestination() != null) {
                    memCache.put(ent.getFlightNumber(), new CacheVal(ent.getOrigin(), ent.getDestination(), now.plus(TTL)));
                    log.debug("HIT db by icao24 — icao24={}, fn={}, origin={}, dest={}",
                            icao24, ent.getFlightNumber(), ent.getOrigin(), ent.getDestination());
                    return new Route(ent.getFlightNumber(), ent.getOrigin(), ent.getDestination());
                } else {
                    log.debug("DB by icao24 is EMPTY — icao24={}", icao24);
                }
            } else {
                log.debug("MISS db by icao24 — icao24={}", icao24);
            }
        }

        // 4) API externa
        String origin = null, destination = null;

        if (flightNumber != null && !apiKey.isBlank()) {
            var until = perFlightCooldown.get(flightNumber);
            if (until != null && now.isBefore(until)) {
                log.debug("COOLDOWN por fn — fn={} hasta {}", flightNumber, until);
            } else {
                try {
                    LocalDate base = (when != null ? when.atZone(LONDON).toLocalDate() : LocalDate.now(LONDON));
                    for (LocalDate d : List.of(base, base.minusDays(1), base.plusDays(1))) {
                        String url = baseUrl + "/flights/number/" + flightNumber + "/" + d + "/" + d;
                        log.info("CALL API — fn={}, url={}", flightNumber, url);

                        List<Map<String, Object>> list = http.get()
                                .uri(url)
                                .retrieve()
                                .body(new ParameterizedTypeReference<>() {});

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
                        perFlightCooldown.put(flightNumber, now.plus(COOLDOWN_TTL));
                        enterGlobalCooldownFrom429(ex);
                        log.warn("API 429 — fn={}; per-flight cooldown hasta {}", flightNumber, perFlightCooldown.get(flightNumber));
                        throw new RateLimitException("Rate limited");
                    } else {
                        log.warn("API error — fn={}, status={}, msg={}", flightNumber, ex.getStatusCode(), ex.getMessage());
                        perFlightCooldown.put(flightNumber, now.plus(Duration.ofMinutes(10)));
                    }
                } catch (Exception ex) {
                    log.warn("API exception — fn={}, msg={}", flightNumber, ex.toString());
                    perFlightCooldown.put(flightNumber, now.plus(Duration.ofMinutes(10)));
                }
            }
        } else {
            log.debug("SKIP API — fn={}, apiKeyPresent={}", flightNumber, !apiKey.isBlank());
        }

        // 5) persistir solo si hay datos
        if (flightNumber != null && (origin != null || destination != null)) {
            cacheRepo.save(FlightRouteCacheEntity.builder()
                    .flightNumber(flightNumber)
                    .icao24(notBlank(icao24) ? icao24.toUpperCase() : null)
                    .origin(origin)
                    .destination(destination)
                    .updatedAt(now)
                    .build());

            memCache.put(flightNumber, new CacheVal(origin, destination, now.plus(TTL)));
            log.debug("SAVED db — fn={}, icao24={}, origin={}, dest={}", flightNumber, icao24, origin, destination);
            return new Route(flightNumber, origin, destination);
        }

        log.debug("Resolve end — sin datos");
        return null;
    }

    // helpers
    private static boolean notBlank(@Nullable String s) {
        return s != null && !s.trim().isEmpty();
    }

    /** BAW123 -> BA123 (mapea ICAO→IATA si corresponde). */
    private @Nullable String normalizeFlightNumber(@Nullable String callsign) {
        if (!notBlank(callsign)) return null;
        String cs = callsign.trim().toUpperCase().replaceAll("\\s+","");
        Matcher m = CALLSIGN_RX.matcher(cs);
        if (!m.matches()) return null;
        String prefix = m.group(1);
        String digits = m.group(2);
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

    /** Lee Retry-After y activa cooldown GLOBAL. */
    private void enterGlobalCooldownFrom429(RestClientResponseException ex) {
        var headers = ex.getResponseHeaders();
        Instant until = null;

        if (headers != null) {
            String ra = headers.getFirst("Retry-After");
            if (ra != null && !ra.isBlank()) {
                try {
                    long secs = Long.parseLong(ra.trim());
                    until = Instant.now().plusSeconds(secs);
                } catch (NumberFormatException nfe) {
                    try {
                        until = ZonedDateTime.parse(ra.trim()).toInstant();
                    } catch (DateTimeParseException ignored) {}
                }
            }
        }
        if (until == null) until = Instant.now().plusSeconds(defaultCooldownSeconds);
        globalCooldownUntil.set(until);
        log.warn("API 429 — GLOBAL cooldown hasta {}", until);
    }
}