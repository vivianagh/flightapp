package io.github.vivianagh.flightapp.state;

import io.github.vivianagh.avro.Flight;
import io.github.vivianagh.flightapp.model.FlightData;
import lombok.Data;

import java.util.Objects;

@Data
public class FlightState {
    private String icao24;
    private String callsign;
    private String transmissionType;
    private Double altitude;
    private Double latitude;
    private Double longitude;
    private Double groundSpeed;
    private Double track;
    private String squawk;
    private Boolean alert;
    private Boolean emergency;
    private Boolean spi;
    private Boolean isOnGround;
    private String generatedDate;
    private String generatedTime;
    private String loggedDate;
    private String loggedTime;
    private Boolean previousIsOnGround;

    public void apply(FlightData data) {
        String type = data.getTransmissionType();
        this.icao24 = data.getIcao24();

        this.previousIsOnGround = this.isOnGround;

        switch (type) {
            case "1" -> applyMsg1(data);
            case "2" -> applyMsg2(data);
            case "3" -> applyMsg3(data);
            case "4" -> applyMsg4(data);
            case "5" -> applyMsg5(data);
            case "6" -> applyMsg6(data);
            case "7" -> applyMsg7(data);
            case "8" -> applyMsg8(data);

        }
    }

    public void applyMsg1(FlightData data) {
        this.callsign = data.getCallsign();
    }

    public void applyMsg2(FlightData data) {
        this.altitude = data.getAltitude();
        this.latitude = data.getLatitude();
        this.longitude = data.getLongitude();
        this.isOnGround = data.isOnGround();
    }
    public void applyMsg3(FlightData data) {
        this.groundSpeed = data.getGroundSpeed();
        this.track = data.getTrack();
        this.altitude = data.getAltitude();
        this.isOnGround = data.isOnGround();
    }
    public void applyMsg4(FlightData data) {
        this.squawk = data.getSquawk();
        this.alert = data.isAlert();
        this.emergency = data.isEmergency();
        this.spi = data.isSpi();
        this.isOnGround = data.isOnGround();
    }
    public void applyMsg5(FlightData data) {this.isOnGround = data.isOnGround();}
    public void applyMsg6(FlightData data) {this.isOnGround = data.isOnGround();}
    public void applyMsg7(FlightData data) {this.isOnGround = data.isOnGround();}
    public void applyMsg8(FlightData data) {this.isOnGround = data.isOnGround();}

    public Flight toAvroFlight() {
        if (icao24 == null || icao24.isBlank()) {
            throw new IllegalStateException("Invalid FlightState: icao24 is null");
        }
        return Flight.newBuilder()
                .setIcao24(icao24)
                .setTransmissionType(transmissionType)
                .setCallsign(callsign)
                .setAltitude(altitude)
                .setLatitude(latitude)
                .setLongitude(longitude)
                .setGroundSpeed(groundSpeed)
                .setTrack(track)
                .setSquawk(squawk)
                .setAlert(alert != null && alert)
                .setEmergency(emergency != null && emergency)
                .setSpi(spi != null && spi)
                .setIsOnGround(isOnGround != null && isOnGround)
                .setGeneratedDate(generatedDate)
                .setGeneratedTime(generatedTime)
                .setLoggedDate(loggedDate)
                .setLoggedTime(loggedTime)
                .build();
    }

    public boolean justLanded() {
        boolean landed = previousIsOnGround != null
                && !previousIsOnGround && Boolean.TRUE.equals(this.isOnGround);
        previousIsOnGround = this.isOnGround;
        return landed;
    }

    public boolean applyAndDetectChange(FlightData newData) {
        boolean changed = false;

        if (!Objects.equals(this.callsign, newData.getCallsign())) {
            this.callsign = newData.getCallsign();
            changed = true;
        }
        if (!Objects.equals(this.altitude, newData.getAltitude())) {
            this.altitude = newData.getAltitude();
            changed = true;
        }

        if (!Objects.equals(latitude, newData.getLatitude())) {
            latitude = newData.getLatitude();
            changed = true;
        }

        if (!Objects.equals(longitude, newData.getLongitude())) {
            longitude = newData.getLongitude();
            changed = true;
        }

        if (!Objects.equals(groundSpeed, newData.getGroundSpeed())) {
            groundSpeed = newData.getGroundSpeed();
            changed = true;
        }

        if (!Objects.equals(track, newData.getTrack())) {
            track = newData.getTrack();
            changed = true;
        }

        if (!Objects.equals(squawk, newData.getSquawk())) {
            squawk = newData.getSquawk();
            changed = true;
        }

        if (!Objects.equals(alert, newData.isAlert())) {
            alert = newData.isAlert();
            changed = true;
        }

        if (!Objects.equals(emergency, newData.isEmergency())) {
            emergency = newData.isEmergency();
            changed = true;
        }

        if (!Objects.equals(spi, newData.isSpi())) {
            spi = newData.isSpi();
            changed = true;
        }

        if (!Objects.equals(isOnGround, newData.isOnGround())) {
            isOnGround = newData.isOnGround();
            changed = true;
        }

        if (!Objects.equals(generatedDate, newData.getGeneratedDate())) {
            generatedDate = newData.getGeneratedDate();
            changed = true;
        }

        if (!Objects.equals(generatedTime, newData.getGeneratedTime())) {
            generatedTime = newData.getGeneratedTime();
            changed = true;
        }

        if (!Objects.equals(loggedDate, newData.getLoggedDate())) {
            loggedDate = newData.getLoggedDate();
            changed = true;
        }

        if (!Objects.equals(loggedTime, newData.getLoggedTime())) {
            loggedTime = newData.getLoggedTime();
            changed = true;
        }
         return changed;
    }

}
