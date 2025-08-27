package io.github.vivianagh.flightapp.repository;

import io.github.vivianagh.flightapp.model.DTO.ChartDataDTO;
import io.github.vivianagh.flightapp.model.entity.FlightsPerHourEntity;
import io.github.vivianagh.flightapp.model.entity.FlightsPerHourId;
import jakarta.persistence.Table;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface FlightsPerHourRepository extends JpaRepository<FlightsPerHourEntity, FlightsPerHourId> {

    // Para el chart (ordenado por hora)
    @Query("SELECT f FROM FlightsPerHourEntity f WHERE f.id.date = :date ORDER BY f.id.hour")
    List<FlightsPerHourEntity> findByDate(@Param("date") LocalDate date);

    // Dedupe: un icao24 por hora y día se cuenta una sola vez
    @Modifying
    @Query(value = """
        INSERT INTO flights_per_hour_seen(date, hour, icao24)
        VALUES (:date, :hour, :icao24)
        ON CONFLICT DO NOTHING
        """, nativeQuery = true)
    int insertSeen(@Param("date") LocalDate date,
                   @Param("hour") int hour,
                   @Param("icao24") String icao24);

    // Upsert del contador por (date,hour)
    @Modifying
    @Query(value = """
        INSERT INTO flights_per_hour(date, hour, count)
        VALUES (:date, :hour, 1)
        ON CONFLICT (date, hour) DO UPDATE SET count = flights_per_hour.count + 1
        """, nativeQuery = true)
    void upsertIncrement(@Param("date") LocalDate date,
                         @Param("hour") int hour);

    // (Opcional) Finder puntual por (date,hour) si lo necesitás:
    @Query("SELECT f FROM FlightsPerHourEntity f WHERE f.id.date = :date AND f.id.hour = :hour")
    Optional<FlightsPerHourEntity> findByDateAndHour(@Param("date") LocalDate date,
                                                     @Param("hour") int hour);
}
