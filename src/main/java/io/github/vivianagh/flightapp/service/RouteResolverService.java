package io.github.vivianagh.flightapp.service;

import io.github.vivianagh.flightapp.exception.RateLimitException;
import org.springframework.lang.Nullable;

import java.time.Instant;
import java.time.LocalDate;

public interface RouteResolverService {

    @Nullable Route resolveFromCallsign(String callsign) throws RateLimitException;
    @Nullable Route resolveFromIcao24(String icao24, Instant when)throws RateLimitException;

    // Queda para otros usos internos
    default @Nullable Route resolve(@Nullable String callsign, @Nullable String icao24, @Nullable Instant when) throws RateLimitException {
        if (callsign != null && !callsign.isBlank()) return resolveFromCallsign(callsign);
        if (icao24 != null && !icao24.isBlank())     return resolveFromIcao24(icao24, when);
        return null;
    }

    // >>> Overload que el Job espera <<<
    RouteResult resolve(String rawCallsign, LocalDate flightDate) throws RateLimitException;

    record Route(String flightNumber, @Nullable String origin, @Nullable String destination) {}

    boolean isCoolingDown();

    record RouteResult(String flightNumber, String origin, String destination, boolean found) {}
}
