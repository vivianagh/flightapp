package io.github.vivianagh.flightapp.repository;

import io.github.vivianagh.flightapp.model.entity.FlightsPerDayEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;

@Repository
public interface FlightsPerDayRepository extends JpaRepository<FlightsPerDayEntity, LocalDate> {
}
