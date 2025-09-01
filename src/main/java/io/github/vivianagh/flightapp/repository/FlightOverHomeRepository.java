package io.github.vivianagh.flightapp.repository;

import io.github.vivianagh.flightapp.model.entity.FlightOverHomeEntity;
import org.springframework.data.domain.Pageable;
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

    // ==== Lecturas para API ====

    /** Últimos N por fecha+hora (con fallback por id). */
    @Query(value = """
        SELECT *
          FROM flight_over_home f
      ORDER BY f.logged_date DESC,
               f.logged_time DESC NULLS LAST,
               f.id DESC
         LIMIT :limit
    """, nativeQuery = true)
    List<FlightOverHomeEntity> findLatest(@Param("limit") int limit);

    /** Últimos N "completables": ya tienen O/D o lo obtienen de la cache por icao24. */
    @Query(value = """
        SELECT f.*
          FROM flight_over_home f
          LEFT JOIN LATERAL (
                SELECT c.origin, c.destination
                  FROM flight_route_cache c
                 WHERE c.icao24 = f.icao24
              ORDER BY c.updated_at DESC
                 LIMIT 1
          ) rc ON TRUE
         WHERE (f.origin IS NOT NULL OR f.destination IS NOT NULL
             OR rc.origin IS NOT NULL OR rc.destination IS NOT NULL)
      ORDER BY f.logged_date DESC,
               f.logged_time DESC NULLS LAST,
               f.id DESC
         LIMIT :limit
    """, nativeQuery = true)
    List<FlightOverHomeEntity> findLatestCompleteLike(@Param("limit") int limit);

    // ==== Backfill / utilitarios ====

    Optional<FlightOverHomeEntity> findFirstByIcao24AndLoggedDateAndLoggedTime(
            String icao24, String loggedDate, String loggedTime);

    // FlightOverHomeRepository.java
    @Query(value = """
  SELECT * 
    FROM flight_over_home
   WHERE (origin IS NULL OR destination IS NULL)
     AND callsign IS NOT NULL
ORDER BY logged_date ASC, logged_time ASC NULLS LAST, id ASC
   LIMIT :limit
""", nativeQuery = true)
List<FlightOverHomeEntity> findRecentMissingRouteWithCallsign(@Param("limit") int limit);

    @Query(value = """
  SELECT *
    FROM flight_over_home
   WHERE (origin IS NULL OR destination IS NULL)
     AND callsign IS NULL
ORDER BY logged_date ASC, logged_time ASC NULLS LAST, id ASC
   LIMIT :limit
""", nativeQuery = true)
List<FlightOverHomeEntity> findRecentMissingRouteNoCallsign(@Param("limit") int limit);


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

    @Modifying
    @Query("""
        UPDATE FlightOverHomeEntity f
           SET f.origin = :origin,
               f.destination = :destination
         WHERE f.icao24 = :icao24
           AND f.loggedDate = :date
           AND f.loggedTime = :time
    """)
    int updateRouteForSample(@Param("icao24") String icao24,
                             @Param("date") String loggedDate,
                             @Param("time") String loggedTime,
                             @Param("origin") String origin,
                             @Param("destination") String destination);

    @Query("""
        SELECT f FROM FlightOverHomeEntity f
         WHERE f.icao24 = :icao24
      ORDER BY f.id DESC
    """)
    List<FlightOverHomeEntity> findRecentByIcao(@Param("icao24") String icao24);



    @Query("""
    SELECT f
      FROM FlightOverHomeEntity f
     WHERE (f.origin IS NULL OR f.destination IS NULL)
       AND f.callsign IS NOT NULL
     ORDER BY f.loggedDate ASC, f.loggedTime ASC NULLS LAST, f.id ASC
  """)
    List<FlightOverHomeEntity> findPendingWithCallsignAsc(Pageable pageable);

    // opcional: para marcar actualizaciones de origin/destination
    @Modifying
    @Query("""
    UPDATE FlightOverHomeEntity f
       SET f.origin = :origin, f.destination = :destination
     WHERE f.id = :id
  """)
    int updateRoute(@Param("id") Long id,
                    @Param("origin") String origin,
                    @Param("destination") String destination);
}
