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
@Table( name="daily_flight_count")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DailyFlightCountEntity {

    @Id
    private LocalDate date;

    private int count;
}
