package com.example.weathermapapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class WeatherMapApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(WeatherMapApiApplication.class, args);
    }

}
