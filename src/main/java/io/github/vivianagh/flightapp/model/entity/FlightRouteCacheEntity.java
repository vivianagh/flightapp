package io.github.vivianagh.flightapp.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;


@Entity
@Table(name = "flight_route_cache")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class FlightRouteCacheEntity {
    @Id
    @Column(name = "flight_number", length = 10)
    private String flightNumber; // p.ej. BA123

    @Column(name = "icao24", length = 6)       // <-- NUEVO
    private String icao24;

    @Column(name = "origin", length = 4)       // IATA/ICAO
    private String origin;

    @Column(name = "destination", length = 4)
    private String destination;

    @Column(name = "updated_at")
    private Instant updatedAt;

}
