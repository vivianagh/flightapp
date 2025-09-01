package io.github.vivianagh.flightapp.jobs;

import io.github.vivianagh.flightapp.exception.RateLimitException;
import io.github.vivianagh.flightapp.model.entity.FlightOverHomeEntity;
import io.github.vivianagh.flightapp.model.entity.FlightRouteCacheEntity;
import io.github.vivianagh.flightapp.repository.FlightOverHomeRepository;
import io.github.vivianagh.flightapp.repository.FlightRouteCacheRepository;
import io.github.vivianagh.flightapp.service.RouteResolverService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;



@Component
public class BackfillRouteJob {

    private static final Logger log = LoggerFactory.getLogger(BackfillRouteJob.class);

    private final FlightOverHomeRepository repo;
    private final RouteResolverService resolver;

    public BackfillRouteJob(FlightOverHomeRepository repo, RouteResolverService resolver) {
        this.repo = repo;
        this.resolver = resolver;
    }

    @Value("${routes.batch-size:10}")
    private int batchSize;

    @Value("${routes.sleep-ms:1200}")
    private long sleepMs;

    @Scheduled(cron = "0 */5 * * * *") // cada 5 minutos; ajustá a gusto
    public void runScheduled() { runInternal(false); }

    // endpoint /ops/backfill/routes puede llamar a runInternal(true)
    @Transactional
    public void runManual() { runInternal(true); }

    private void runInternal(boolean manual) {
        if (resolver.isCoolingDown()) {
            log.info("BackfillRouteJob: en cooldown, no proceso batch");
            return;
        }

        List<FlightOverHomeEntity> rows =
                repo.findPendingWithCallsignAsc(PageRequest.of(0, batchSize));

        int attempts = 0, hits = 0, updated = 0;

        for (FlightOverHomeEntity row : rows) {
            if (resolver.isCoolingDown()) break; // por si se activó durante el loop
            try {
                attempts++;
                LocalDate day = LocalDate.parse(row.getLoggedDate()); // asumimos campo LocalDate
                RouteResolverService.RouteResult rr = resolver.resolve(row.getCallsign(), day);
                if (rr.found()) {
                    hits++;
                    int n = repo.updateRoute(row.getId(), rr.origin(), rr.destination());
                    updated += n;
                }
                // dormimos siempre (aunque no haya hit) para no quemar cuota
                if (sleepMs > 0) {
                    try { Thread.sleep(sleepMs); } catch (InterruptedException ignored) {}
                }
            } catch (RateLimitException rle) {
                // Cortamos el batch al primer 429
                log.info("BackfillRouteJob: rate limited; corto el batch");
                break;
            } catch (Exception ex) {
                log.warn("BackfillRouteJob: error en fila id={}: {}", row.getId(), ex.toString());
            }
        }

        log.info("BackfillRouteJob: rows={}, apiAttempts={}, apiHits={}, updated={}",
                rows.size(), attempts, hits, updated);
    }
}