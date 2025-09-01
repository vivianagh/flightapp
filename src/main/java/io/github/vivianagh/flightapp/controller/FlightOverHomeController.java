package io.github.vivianagh.flightapp.controller;

import io.github.vivianagh.flightapp.model.DTO.FlightOverHomeDTO;
import io.github.vivianagh.flightapp.service.FlightOverHomeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class FlightOverHomeController {

    private final FlightOverHomeService service;

    @GetMapping("/flights-over-home")
    public List<FlightOverHomeDTO> getRecentFlights() {
        return service.getAllFlightsOverHome();
    }
}
