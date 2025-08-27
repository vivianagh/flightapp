package io.github.vivianagh.flightapp.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

import java.io.Serializable;
import java.time.LocalDate;

@Embeddable
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@EqualsAndHashCode
public class FlightsPerHourId implements Serializable {
    @Column(name = "date")
    private LocalDate date;

    @Column(name = "hour")
    private Integer hour;
}
