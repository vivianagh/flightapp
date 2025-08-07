package io.github.vivianagh.flightapp.service;

import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class AirlineLookupService {
    private final Map<String, String> knownAirlines = Map.of(
            "AAL", "American Airlines",
            "UAL", "United Airlines",
            "LAN", "LATAM Airlines"

    );

    public String getAirlineName(String icao24) {
        if (icao24 == null || icao24.length() < 3) return "Uknown";
        String prefix = icao24.substring(0,3).toUpperCase();
        return knownAirlines.getOrDefault(prefix, "Uknown");
    }
}
