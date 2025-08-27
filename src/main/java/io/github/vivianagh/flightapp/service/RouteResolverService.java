package io.github.vivianagh.flightapp.service;

import org.springframework.lang.Nullable;

import java.time.Instant;

public interface RouteResolverService {

    @Nullable Route resolveFromCallsign(String callsign);
    @Nullable Route resolveFromIcao24(String icao24, Instant when);

    default @Nullable Route resolve(@Nullable String callsign, @Nullable String icao24, @Nullable Instant when) {
        if (callsign != null && !callsign.isBlank()) return resolveFromCallsign(callsign);
        if (icao24 != null && !icao24.isBlank())     return resolveFromIcao24(icao24, when);
        return null;
    }

    record Route(String flightNumber, @Nullable String origin, @Nullable String destination) {}

}
