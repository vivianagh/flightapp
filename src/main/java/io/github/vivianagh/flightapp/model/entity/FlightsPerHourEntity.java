package io.github.vivianagh.flightapp.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name ="flights_per_hour")
@IdClass(FlightsPerHourId.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FlightsPerHourEntity {
    @Id
    private LocalDate date;

    @Id
    private int hour;

    private int count;
}
