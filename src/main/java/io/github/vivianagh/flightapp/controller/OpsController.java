package io.github.vivianagh.flightapp.controller;

import io.github.vivianagh.flightapp.jobs.BackfillRouteJob;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/ops")
public class OpsController {
    private final BackfillRouteJob job;

    @PostMapping("/backfill/routes")
    public ResponseEntity<String> runBackfill() {
        job.runManual();
        return ResponseEntity.ok("Backfill ejecutado");
    }
}
