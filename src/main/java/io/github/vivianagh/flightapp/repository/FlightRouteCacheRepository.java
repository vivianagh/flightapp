package io.github.vivianagh.flightapp.repository;

import io.github.vivianagh.flightapp.model.entity.FlightRouteCacheEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FlightRouteCacheRepository extends JpaRepository<FlightRouteCacheEntity, String> {
    Optional<FlightRouteCacheEntity> findFirstByIcao24OrderByUpdatedAtDesc(String icao24);
}
