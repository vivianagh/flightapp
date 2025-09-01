package io.github.vivianagh.flightapp;

import io.github.vivianagh.flightapp.config.FlightSocketProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties(FlightSocketProperties.class)
@ConfigurationPropertiesScan
public class FlightAppApplication {

    public static void main(String[] args) {
        SpringApplication.run(FlightAppApplication.class, args);
    }

}
