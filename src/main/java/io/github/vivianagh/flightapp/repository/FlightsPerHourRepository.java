package io.github.vivianagh.flightapp.repository;

import io.github.vivianagh.flightapp.model.DTO.ChartDataDTO;
import io.github.vivianagh.flightapp.model.entity.FlightsPerHourEntity;
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
public interface FlightsPerHourRepository extends JpaRepository<FlightsPerHourEntity, Long> {
    List<FlightsPerHourEntity> findByDate(LocalDate date);

    Optional<FlightsPerHourEntity> findByDateAndHour( LocalDate date, int hour);

    @Modifying
    @Transactional
    @Query("UPDATE FlightsPerHourEntity f SET f.count = f.count + 1 WHERE f.date = :date AND f.hour = :hour")
    int incrementCount(@Param("date") LocalDate date, @Param("hour") int hour);

}
