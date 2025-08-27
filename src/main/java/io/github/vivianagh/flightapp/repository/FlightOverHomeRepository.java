package io.github.vivianagh.flightapp.repository;

import io.github.vivianagh.flightapp.model.entity.FlightOverHomeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.LocalTime;

import java.util.List;
import java.util.Optional;

@Repository
public interface FlightOverHomeRepository extends JpaRepository<FlightOverHomeEntity, Long> {
    List<FlightOverHomeEntity> findTop20ByOrderByLoggedDateDesc();

    Optional<FlightOverHomeEntity> findFirstByIcao24AndLoggedDateAndLoggedTime(
            String icao24, String loggedDate, String loggedTime);


    @Modifying
    @Transactional
    @Query(value = """
      UPDATE flight_over_home
         SET callsign = :callsign
       WHERE icao24 = :icao24
         AND callsign IS NULL
         AND logged_date = :date
         AND logged_time BETWEEN (:time - interval '10 minutes') AND (:time + interval '1 minute')
            """, nativeQuery = true)
    int backfillCallsign(@Param("icao24") String icao24,
                         @Param("callsign") String callsign,
                         @Param("date") LocalDate date,
                         @Param("time") LocalTime time);
}
