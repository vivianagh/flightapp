package io.github.vivianagh.flightapp.model.DTO;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ChartDataDTO {
    private String name;  // Hora formateada como "14:00"
    private int value;    // Cantidad de vuelos
}
