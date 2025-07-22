package io.github.vivianagh.flightapp.model;


import io.github.vivianagh.avro.Flight;
import lombok.*;

@Getter
@Setter
@ToString
@NoArgsConstructor
public class FlightData {
    private String icao24;          //Identificador unico de aeronave
    private String transmissionType;
    private String callsign;         //Numero de vuelo
    private Double altitude;
    private Double latitude;
    private Double longitude;
    private Double groundSpeed;     //Velocidad sobre tierra
    private Double track;           //Direccion
    private String squawk;          //Codigo transponder
    private boolean alert;          //Alerta
    private boolean emergency;
    private boolean spi;             // Identificación especial
    private boolean isOnGround;      // En tierra
    private String generatedDate;
    private String generatedTime;
    private String loggedDate;
    private String loggedTime;


    public static FlightData fromCsvLine(String csvLine) {
        String[] parts = csvLine.split(",");
        FlightData data = new FlightData();
        data.transmissionType = safeGet(parts, 1);
        data.icao24 = safeGet(parts, 4);
        data.generatedDate = safeGet(parts, 6);
        data.generatedTime = safeGet(parts, 7);
        data.loggedDate = safeGet(parts, 8);
        data.loggedTime = safeGet(parts, 9);
        data.callsign = safeGet(parts, 10);
        data.altitude = parseDouble(parts, 11);
        data.groundSpeed = parseDouble(parts, 12);
        data.track = parseDouble(parts, 13);
        data.latitude = parseDouble(parts, 14);
        data.longitude = parseDouble(parts, 15);
        data.squawk = safeGet(parts, 17);
        data.alert = parseBoolean(parts, 18);
        data.emergency = parseBoolean(parts, 19);
        data.spi = parseBoolean(parts, 20);
        data.isOnGround = parseBoolean(parts, 21);
        return data;
    }
    /**
     *
     MSG,1 → Identification
     MSG,3 → Airborne Position
     MSG,4 → Airborne Velocity
     */
    

    private static String safeGet(String[] parts, int index) {
        if (index >=parts.length || parts[index].isEmpty()) {
            return null;
        }
        return parts[index];
    }

    private static Double parseDouble(String[] parts, int index) {
        try {
            String value = safeGet(parts, index);
            return (value == null) ? null : Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }
    private static boolean parseBoolean(String[] parts, int index) {
        String value = safeGet(parts, index);
        return value != null && value.equals("1");
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("FlightData{icao24='").append(icao24).append("'");
        sb.append(", type=").append(transmissionType);
        if (callsign != null) sb.append(", callsign='").append(callsign).append("'");
        if (altitude != null) sb.append(", altitude=").append(altitude);
        if (groundSpeed != null) sb.append(", groundSpeed=").append(groundSpeed);
        if (track != null) sb.append(", track=").append(track);
        if (latitude != null) sb.append(", latitude=").append(latitude);
        if (longitude != null) sb.append(", longitude=").append(longitude);
        if (squawk != null) sb.append(", squawk='").append(squawk).append("'");
        if (alert) sb.append(", alert=").append(alert);
        if (emergency) sb.append(", emergency=").append(emergency);
        if (spi) sb.append(", spi=").append(spi);
        if (isOnGround) sb.append(", isOnGround=").append(isOnGround);
        if (generatedDate != null) sb.append(", generatedDate=").append(generatedDate);
        if (generatedTime != null) sb.append(", generatedTime=").append(generatedTime);
        if (loggedDate != null) sb.append(", loggedDate=").append(loggedDate);
        if (loggedTime != null) sb.append(", loggedTime=").append(loggedTime);
        sb.append('}');
        return sb.toString();
    }

    public static FlightData fromAvro(Flight flightAvro) {
        FlightData flightData = new FlightData();
        flightData.setIcao24(flightAvro.getIcao24() != null ? flightAvro.getIcao24().toString() : null);
        flightData.setCallsign(flightAvro.getCallsign() != null ? flightAvro.getCallsign().toString() : null);
        flightData.setAltitude(flightAvro.getAltitude());
        flightData.setLatitude(flightAvro.getLatitude());
        flightData.setLongitude(flightAvro.getLongitude());
        flightData.setGroundSpeed(flightAvro.getGroundSpeed());
        flightData.setSquawk(flightAvro.getSquawk() != null ? flightAvro.getSquawk().toString() : null);
        flightData.setAlert(flightAvro.getAlert());
        flightData.setEmergency(flightAvro.getEmergency());
        flightData.setSpi(flightAvro.getSpi());
        flightData.setOnGround(flightAvro.getIsOnGround());
        flightData.setGeneratedDate(flightAvro.getGeneratedDate() != null ? flightAvro.getGeneratedDate().toString() : null);
        flightData.setGeneratedTime(flightAvro.getGeneratedTime() != null ? flightAvro.getGeneratedTime().toString() : null);
        flightData.setLoggedDate(flightAvro.getLoggedDate() != null ? flightAvro.getLoggedDate().toString() : null);
        flightData.setLoggedTime(flightAvro.getLoggedTime() != null ? flightAvro.getLoggedTime().toString() : null);
        flightData.setTransmissionType(flightAvro.getTransmissionType() != null ? flightAvro.getTransmissionType().toString() : null);
        return flightData;
    }
}
