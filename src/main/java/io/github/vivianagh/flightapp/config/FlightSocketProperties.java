package io.github.vivianagh.flightapp.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "flight.socket")
public record FlightSocketProperties(
        boolean enabled,            // activar/desactivar lector
        String host,                // host del socket (antena/feeder)
        int port,                   // puerto (p.ej. 30003 dump1090)
        int connectTimeoutMs,       // timeout de conexión
        int readTimeoutMs,          // timeout de lectura
        int reconnectBackoffMs      // backoff entre reintentos
) {
    public FlightSocketProperties {
        // Defaults “seguros”
        if (host == null || host.isBlank()) host = "localhost";
        if (port <= 0) port = 30003;                // típico dump1090
        if (connectTimeoutMs <= 0) connectTimeoutMs = 3000;
        if (readTimeoutMs <= 0) readTimeoutMs = 5000;
        if (reconnectBackoffMs <= 0) reconnectBackoffMs = 1000;
    }
}