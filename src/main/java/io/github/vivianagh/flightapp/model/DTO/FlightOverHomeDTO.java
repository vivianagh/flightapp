package io.github.vivianagh.flightapp.model.DTO;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class FlightOverHomeDTO {
    private String callsign;
    private String airline;
    private String origin;
    private String destination;
    private Double altitude;
    private Double speed;
    private Double latitude;
    private Double longitude;
    private Boolean hasAlert;
    private Boolean hasEmergency;
    private String timestamp;
}
