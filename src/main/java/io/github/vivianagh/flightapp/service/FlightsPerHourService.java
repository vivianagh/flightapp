package io.github.vivianagh.flightapp.service;

import io.github.vivianagh.flightapp.model.DTO.ChartDataDTO;
import io.github.vivianagh.flightapp.model.entity.FlightsPerHourEntity;
import io.github.vivianagh.flightapp.repository.FlightsPerHourRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
@Slf4j
public class FlightsPerHourService {

    private final FlightsPerHourRepository repository;

    /** Cuenta el vuelo solo si (date,hour,icao24) no se vio antes. */
    @Transactional
    public void incrementCount(LocalDate date, int hour, String icao24) {
        if (icao24 == null || icao24.isBlank()) return;

        int inserted = repository.insertSeen(date, hour, icao24.trim().toUpperCase());
        if (inserted == 1) {
            // primera vez que vemos ese avión en esa hora -> incrementa el contador
            repository.upsertIncrement(date, hour);
        }
    }

    /** Devuelve 24 barras (00–23), con 0 donde no hay datos. */
    @Transactional(readOnly = true)
    public List<ChartDataDTO> getFlightsPerHourForDate(LocalDate date) {
        var rows = repository.findByDate(date);

        Map<Integer, Integer> map = rows.stream()
                .collect(Collectors.toMap(e -> e.getId().getHour(), FlightsPerHourEntity::getCount));

        return IntStream.range(0, 24)
                .mapToObj(h -> ChartDataDTO.builder()
                        .name(String.format("%02d:00", h))
                        .value(map.getOrDefault(h, 0))
                        .build())
                .toList();
    }
}