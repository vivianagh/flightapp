package io.github.vivianagh.flightapp.socket;

import io.github.vivianagh.flightapp.model.FlightData;
import io.github.vivianagh.flightapp.producer.FlightProducerService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.Socket;

@Component
@RequiredArgsConstructor
public class FlightSocketReader implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(FlightSocketReader.class);

    private final FlightProducerService producerService;

    @Value("${flight.socket.host}")
    private String host;

    @Value("${flight.socket.port}")
    private int port;

    @Override
    public void run(String... args) throws Exception {


        try (Socket socket = new Socket(host, port);
             BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("MSG")) {
                    FlightData flightData = FlightData.fromCsvLine(line);
                    if (flightData.getIcao24() != null) {
                        producerService.sendFlightData(flightData);
                        log.info("Sent to Kafka: {}", flightData.toString());
                    }
                }
            }

        } catch (Exception e) {
            log.error("Error reading from socket", e);
        }
    }
}
