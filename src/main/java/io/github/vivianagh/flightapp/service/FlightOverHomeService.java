package io.github.vivianagh.flightapp.service;

import io.github.vivianagh.flightapp.exception.ResourceNotFoundException;
import io.github.vivianagh.flightapp.model.DTO.FlightOverHomeDTO;
import io.github.vivianagh.flightapp.model.entity.FlightOverHomeEntity;
import io.github.vivianagh.flightapp.repository.FlightOverHomeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class FlightOverHomeService {

    private final FlightOverHomeRepository repository;
    private final AirlineLookupService lookupService;

    public List<FlightOverHomeDTO> getAllFlightsOverHome() {
        List<FlightOverHomeEntity> flights = repository.findTop20ByOrderByLoggedDateDesc();
        if (flights.isEmpty()) {
            throw new ResourceNotFoundException("No se encontraron vuelos sobre casa.");
        }
        return flights.stream()
                .map(entity -> FlightOverHomeDTO.builder()
                        .callsign(entity.getCallsign())
                        .airline(lookupService.getAirlineName(entity.getIcao24()))
                        .origin("Unknown")       // hardcoded for now
                        .destination("EZE")      // hardcoded for now
                        .altitude(entity.getAltitude())
                        .speed(null)             // optional
                        .latitude(entity.getLatitude())
                        .longitude(entity.getLongitude())
                        .hasAlert(false)         // default until data available
                        .hasEmergency(false)
                        .timestamp(entity.getLoggedTime())
                        .build())
                .toList();
    }

    public Optional<FlightOverHomeEntity> getById(Long id) {
        return repository.findById(id);
    }
}
