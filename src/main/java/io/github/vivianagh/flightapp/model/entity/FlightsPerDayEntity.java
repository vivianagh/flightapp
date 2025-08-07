package io.github.vivianagh.flightapp.model.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "flights_per_day")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FlightsPerDayEntity {
    @Id
    private LocalDate date;
    private int count;
}