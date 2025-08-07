package io.github.vivianagh.flightapp.repository;

import io.github.vivianagh.flightapp.model.entity.DeadLetterEntity;
import org.springframework.data.repository.CrudRepository;

public interface DeadLetterRepository extends CrudRepository<DeadLetterEntity, Long> {
}
