package io.github.vivianagh.flightapp.model.DTO;

public record RouteResolutionDto(
        String callsign,
        String flightNumber,   // BA123
        String origin,         // LHR
        String destination     // MAD
) {
}
