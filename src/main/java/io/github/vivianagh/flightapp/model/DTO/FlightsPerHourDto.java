package io.github.vivianagh.flightapp.model.DTO;

public record FlightsPerHourDto(
        int hour,   // 0..23
        long count
) {
}
