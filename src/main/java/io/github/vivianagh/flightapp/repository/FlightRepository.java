package io.github.vivianagh.flightapp.repository;

import io.github.vivianagh.flightapp.model.FlightEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FlightRepository extends JpaRepository<FlightEntity, String> {
}
