package io.github.vivianagh.flightapp.service;

import io.github.vivianagh.flightapp.model.DTO.ChartDataDTO;
import io.github.vivianagh.flightapp.model.entity.FlightsPerHourEntity;
import io.github.vivianagh.flightapp.repository.FlightsPerHourRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class FlightsPerHourService {

    private final FlightsPerHourRepository repository;

    // 🧠 Map in-memory para evitar duplicados (por hora y avión)
    private final Map<LocalDate, Map<Integer, Set<String>>> seenPerHour = new ConcurrentHashMap<>();

    public synchronized void incrementCount(LocalDate date, int hour, String icao24) {
        seenPerHour.putIfAbsent(date, new ConcurrentHashMap<>());
        seenPerHour.get(date).putIfAbsent(hour, ConcurrentHashMap.newKeySet());

        Set<String> seenSet = seenPerHour.get(date).get(hour);
        if (seenSet.contains(icao24)) {
            return; // ya fue contado este avión esta hora
        }

        seenSet.add(icao24);

        int updated = repository.incrementCount(date, hour);
        if (updated == 0) {
            // no existía, lo creamos
            FlightsPerHourEntity newEntry = FlightsPerHourEntity.builder()
                    .date(date)
                    .hour(hour)
                    .count(1)
                    .build();
            repository.save(newEntry);
        }

    }

    // Persiste los datos acumulados en la base
    @Scheduled(cron = "0 0 * * * *")
    public void persistCounts() {
        seenPerHour.forEach((date, hourlyMap) -> {
            hourlyMap.forEach((hour, icaoSet) -> {
                int count = icaoSet.size(); // ✅ ahora es un Set<String>, así que usamos .size()

                repository.findByDateAndHour(date, hour).ifPresentOrElse(
                        existing -> {
                            existing.setCount(existing.getCount() + count);
                            repository.save(existing);
                        },
                        () -> {
                            FlightsPerHourEntity entity = FlightsPerHourEntity.builder()
                                    .date(date)
                                    .hour(hour)
                                    .count(count)
                                    .build();
                            repository.save(entity);
                        }
                );
            });
        });

        seenPerHour.clear(); // Opcional: limpia después de guardar
        log.info("✅ Persisted hourly flight counts to DB");

    }

    public List<ChartDataDTO> getFlightsPerHourForDate(LocalDate date) {
        return repository.findByDate(date).stream()
                .sorted(Comparator.comparing(FlightsPerHourEntity::getHour))
                .map( e -> ChartDataDTO.builder()
                        .name(String.format("%02d:00", e.getHour()))
                        .value(e.getCount())
                        .build())
                .toList();
    }

}
