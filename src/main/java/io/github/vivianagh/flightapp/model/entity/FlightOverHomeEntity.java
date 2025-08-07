package io.github.vivianagh.flightapp.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "flight_over_home")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FlightOverHomeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String icao24;
    private String callsign;
    private Double latitude;
    private Double longitude;
    private Double altitude;

    private String loggedDate;
    private String loggedTime;
}
