package io.github.vivianagh.flightapp.service;

import io.github.vivianagh.flightapp.model.AirlineInfo;
import org.springframework.stereotype.Service;
import org.springframework.lang.Nullable;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import static java.util.Map.entry;

@Service
public class AirlineLookupService {

    private final AirlineDirectory  directory;


    public AirlineLookupService(AirlineDirectory directory) {
        this.directory = directory;
    }

    public String getAirlineName(@Nullable String callsign, @Nullable String icao24) {
        String name = lookupByCallsignPrefix(callsign);
        return (name != null && !name.isBlank()) ? name : "Unknown";
    }

    @Nullable
    public String lookupByCallsignPrefix(String callsign) {
        if (callsign == null || callsign.isBlank()) return null;

        Optional<String> prefixOpt = CallsignParser.extractPrefix(callsign);
        if (prefixOpt.isEmpty()) return null;
        String prefix = prefixOpt.get();

        // Intentar 3 letras (ICAO), si no, 2 (IATA)
        Optional<AirlineInfo> info =
                (prefix.length() == 3)
                        ? directory.findByIcao(prefix).or(() -> directory.findByIata(prefix))
                        : directory.findByIata(prefix).or(() -> directory.findByIcao(prefix));

        return info.map(AirlineInfo::getName).orElse(null);
    }

}
