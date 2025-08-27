package io.github.vivianagh.flightapp.cache;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class TelemetryCache {
    private final Map<String, Double> lastSpeed = new ConcurrentHashMap<>();

    public void rememberSpeed(String icao24, Double speed) {
        if (icao24 == null || speed == null) return;
        lastSpeed.put(icao24.trim().toUpperCase(), speed);
    }
    public Double getSpeed(String icao24) {
        if (icao24 == null) return null;
        return lastSpeed.get(icao24.trim().toUpperCase());
    }
}
