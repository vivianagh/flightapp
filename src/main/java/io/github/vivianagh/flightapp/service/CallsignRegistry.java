package io.github.vivianagh.flightapp.service;

import org.springframework.stereotype.Service;
import org.springframework.lang.Nullable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class CallsignRegistry {
    private static final Duration TTL = Duration.ofHours(12);
    private static final class Entry { final String cs; final Instant ts; Entry(String c, Instant t){cs=c;ts=t;} }
    private final Map<String, Entry> map = new ConcurrentHashMap<>();

    public void put(String icao24, String callsign) {
        if (icao24 == null || callsign == null) return;
        String cs = callsign.trim().toUpperCase();
        if (cs.isEmpty()) return;
        map.put(icao24.trim().toUpperCase(), new Entry(cs, Instant.now()));
    }

    @Nullable public String get(String icao24) {
        if (icao24 == null) return null;
        Entry e = map.get(icao24.trim().toUpperCase());
        if (e == null || e.ts.isBefore(Instant.now().minus(TTL))) return null;
        return e.cs;
    }

    @Scheduled(fixedDelay = 60 * 60 * 1000) // 1h
    public void sweep() {
        Instant limit = Instant.now().minus(TTL);
        map.entrySet().removeIf(en -> en.getValue().ts.isBefore(limit));
    }
}

