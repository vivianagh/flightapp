package io.github.vivianagh.flightapp.service;

import io.github.vivianagh.flightapp.model.entity.FlightsPerDayEntity;
import io.github.vivianagh.flightapp.repository.FlightsPerDayRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
public class DailyFlightCounterService {

    private final Map<LocalDate, Integer> dailyCount = new ConcurrentHashMap<>();

    private final FlightsPerDayRepository repository;

    public DailyFlightCounterService(FlightsPerDayRepository repository) {
        this.repository = repository;
    }

    public void incrementDailyCount() {
        LocalDate today = LocalDate.now();
        dailyCount.merge(today, 1, Integer::sum);
    }

    @Scheduled(cron = "0 0 1 * * *") // cada día a la 1 a.m.
    public void persistDailyCounts() {
        dailyCount.forEach((date, count) -> {
            repository.findById(date).ifPresentOrElse(
                    existing -> {
                        existing.setCount(existing.getCount() + count);
                        repository.save(existing);
                    },
                    () -> {
                        repository.save(FlightsPerDayEntity.builder().date(date).count(count).build());
                    }
            );
        });
        dailyCount.clear();
        log.info("✅ Persisted daily flight counts");
    }

}
