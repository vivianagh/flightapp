package io.github.vivianagh.flightapp.repository;

import io.github.vivianagh.flightapp.model.entity.FlightRouteCacheEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FlightRouteCacheRepository extends JpaRepository<FlightRouteCacheEntity, String> {
    Optional<FlightRouteCacheEntity> findFirstByIcao24OrderByUpdatedAtDesc(String icao24);


    @Query("SELECT c FROM FlightRouteCacheEntity c WHERE c.flightNumber = :fn")
    Optional<FlightRouteCacheEntity> findByFlightNumber(@Param("fn") String flightNumber);

    @Query("""
     SELECT c FROM FlightRouteCacheEntity c
      WHERE c.icao24 = :icao24
      ORDER BY c.updatedAt DESC
     """)
    Optional<FlightRouteCacheEntity> findLatestByIcao24(@Param("icao24") String icao24);


}
