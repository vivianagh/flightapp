package io.github.vivianagh.flightapp.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AirlineInfo {
    private String icao;    // e.g. BAW
    private String iata;    // e.g. BA
    private String name;    // e.g. British Airways
    private String country; // optional

    public AirlineInfo() {}

    public AirlineInfo(String icao, String iata, String name, String country) {
        this.icao = icao; this.iata = iata; this.name = name; this.country = country;
    }

    public String getIcao() { return icao; }
    public void setIcao(String icao) { this.icao = icao; }
    public String getIata() { return iata; }
    public void setIata(String iata) { this.iata = iata; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }
}
