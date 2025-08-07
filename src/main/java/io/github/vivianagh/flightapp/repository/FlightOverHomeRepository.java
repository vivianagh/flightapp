package io.github.vivianagh.flightapp.repository;

import io.github.vivianagh.flightapp.model.entity.FlightOverHomeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FlightOverHomeRepository extends JpaRepository<FlightOverHomeEntity, Long> {
    List<FlightOverHomeEntity> findTop20ByOrderByLoggedDateDesc();

}
