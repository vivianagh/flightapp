package io.github.vivianagh.flightapp.model.DTO;


import java.time.Instant;

public record FlightDto(
        String callsign,
        String airline,
        String origin,
        String destination,
        Integer altitude,
        Integer speed,
        Double latitude,
        Double longitude,
        boolean hasAlert,
        boolean hasEmergency,
        Instant timestamp
) {

}
