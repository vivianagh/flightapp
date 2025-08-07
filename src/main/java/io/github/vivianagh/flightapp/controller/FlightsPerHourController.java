package io.github.vivianagh.flightapp.controller;

import io.github.vivianagh.flightapp.model.DTO.ChartDataDTO;
import io.github.vivianagh.flightapp.service.FlightsPerHourService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class FlightsPerHourController {

    private final FlightsPerHourService service;

    @GetMapping("/flights-per-hour")
    public ResponseEntity<List<ChartDataDTO>> getFlightsPerHour(@RequestParam String date) {
        try {
            LocalDate parsedDate = LocalDate.parse(date);
            List<ChartDataDTO> results = service.getFlightsPerHourForDate(parsedDate);
            return ResponseEntity.ok(results);
        } catch (DateTimeParseException e) {
            return ResponseEntity.badRequest().build(); // 400 si el formato es inválido
        }
    }
}
