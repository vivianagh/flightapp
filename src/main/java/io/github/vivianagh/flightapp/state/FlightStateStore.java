package io.github.vivianagh.flightapp.state;

import io.github.vivianagh.avro.Flight;
import io.github.vivianagh.flightapp.model.FlightData;
import lombok.Getter;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Getter
@Component
public class FlightStateStore {

    private final Map<String, FlightState> stateMap = new ConcurrentHashMap<>();

    public Flight process(FlightData data) {
        String icao24 = data.getIcao24();
        if (icao24 == null) return null;

        FlightState currentState = stateMap.computeIfAbsent(icao24, k -> {
            FlightState fs = new FlightState();
            fs.setIcao24(icao24);
            return fs;
        });

        boolean changed = currentState.applyAndDetectChange(data);

        if (!changed) {
            return null; // no emitir si no cambió nada
        }

        return currentState.toAvroFlight();
    }

    // Útil para testing
    public void clear() {
        stateMap.clear();
    }

}
