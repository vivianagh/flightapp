package io.github.vivianagh.flightapp.socket;

import io.github.vivianagh.flightapp.config.FlightSocketProperties;
import io.github.vivianagh.flightapp.model.FlightData;
import io.github.vivianagh.flightapp.producer.RawFlightProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.Socket;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

@Slf4j
@Component
@RequiredArgsConstructor
public class FlightSocketReader implements CommandLineRunner {

    private final FlightSocketProperties props;

    private final RawFlightProducer producer;

    private String host;

    private int port;

    @Override
    public void run(String... args) throws Exception {
        if (!props.enabled()) {
            log.info("✋ FlightSocketReader deshabilitado (flight.socket.enabled=false)");
            return;
        }

        final InetSocketAddress addr = new InetSocketAddress(props.host(), props.port());
        log.info("🔌 Conectando a feeder en {}:{}", props.host(), props.port());

        // Bucle de reconexión básico
        while (true) {
            try (Socket socket = new Socket()) {
                socket.connect(addr, props.connectTimeoutMs());
                socket.setSoTimeout(props.readTimeoutMs());
                try (BufferedReader br = new BufferedReader(
                        new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))) {

                    String line;
                    while ((line = br.readLine()) != null) {
                        // TODO: parsear tu formato (SBS/BEAST/lo que uses)
                        // y enviar al tópico raw con Avro
                        FlightData data = FlightData.fromCsvLine(line);
                        producer.sendRawFlight(data);
                    }
                }
            } catch (Exception ex) {
                log.error("⚠️ Error leyendo del socket ({}). Reintento en {} ms",
                        ex.getMessage(), props.reconnectBackoffMs(), ex);
                try {
                    Thread.sleep(props.reconnectBackoffMs());
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }


    }
}
