package io.github.vivianagh.flightapp.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@RequiredArgsConstructor
public class FlightEntity {

    @Id
    private String icao24;
    private String transmissionType;
    private String callsign;
    private Double altitude;
    private Double latitude;
    private Double longitude;
    private Double groundSpeed;
    private String squawk;
    private Boolean alert;
    private Boolean emergency;
    private Boolean spi;
    private Boolean isOnGround;
    private String generatedDate;
    private String generatedTime;
    private String loggedDate;
    private String loggedTime;

}
