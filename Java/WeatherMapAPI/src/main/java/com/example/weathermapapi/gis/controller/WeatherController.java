package com.example.weathermapapi.gis.controller;

import com.example.weathermapapi.gis.service.WeatherService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/weather")
@RequiredArgsConstructor
public class WeatherController {

    private final WeatherService weatherService;

    /**
     * 예시 호출:
     * POST /api/weather/fetch?lat=37.5665&lon=126.9780
     */
    @PostMapping("/fetch")
    public ResponseEntity<Void> fetchWeather(@RequestParam double lat, @RequestParam double lon) {
        weatherService.fetchAndSaveHourly(lat, lon);
        return ResponseEntity.ok().build();
    }

}
