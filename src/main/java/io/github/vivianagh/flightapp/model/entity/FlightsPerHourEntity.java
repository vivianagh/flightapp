package io.github.vivianagh.flightapp.model.entity;

import jakarta.persistence.*;
import lombok.*;


@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
@Entity
@Table(name = "flights_per_hour")
public class FlightsPerHourEntity {

    @EmbeddedId
    private FlightsPerHourId id;

    @Column(name = "count", nullable = false)
    private Integer count;
}