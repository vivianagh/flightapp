package io.github.vivianagh.flightapp.service;

import org.springframework.stereotype.Component;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.vivianagh.flightapp.model.AirlineInfo;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.*;


@Component
public class AirlineDirectory {
    private final Map<String, AirlineInfo> byIcao;
    private final Map<String, AirlineInfo> byIata;

    public AirlineDirectory(ObjectMapper mapper) {
        try {
            ClassPathResource res = new ClassPathResource("airlines.json");
            if (!res.exists()) throw new IllegalStateException("Missing airlines.json in resources/");
            List<AirlineInfo> list;
            try (InputStream is = res.getInputStream()) {
                list = mapper.readValue(is, new TypeReference<List<AirlineInfo>>() {});
            }
            Map<String, AirlineInfo> icao = new HashMap<>();
            Map<String, AirlineInfo> iata = new HashMap<>();
            for (AirlineInfo a : list) {
                if (a.getIcao() != null && !a.getIcao().isBlank())
                    icao.put(a.getIcao().trim().toUpperCase(Locale.ROOT), a);
                if (a.getIata() != null && !a.getIata().isBlank())
                    iata.put(a.getIata().trim().toUpperCase(Locale.ROOT), a);
            }
            this.byIcao = Collections.unmodifiableMap(icao);
            this.byIata = Collections.unmodifiableMap(iata);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load airlines.json", e);
        }
    }

    public Optional<AirlineInfo> findByIcao(String c) {
        if (c == null) return Optional.empty();
        return Optional.ofNullable(byIcao.get(c.trim().toUpperCase(Locale.ROOT)));
    }
    public Optional<AirlineInfo> findByIata(String c) {
        if (c == null) return Optional.empty();
        return Optional.ofNullable(byIata.get(c.trim().toUpperCase(Locale.ROOT)));
    }
}
