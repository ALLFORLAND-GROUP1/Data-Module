package com.example.weathermapapi.gis.controller;

import com.example.weathermapapi.gis.service.WeatherService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;

@RestController
@RequestMapping("/api/weather")
@RequiredArgsConstructor
public class WeatherController {

    private final WeatherService weatherService;

    /**
     * 기본 5개 역에 대해
     * 현재 기준 48시간 예보 저장
     * POST /api/weather/now/stations
     */
    @PostMapping("/now/stations")
    public ResponseEntity<Void> saveForecastForDefaultStations() {
        weatherService.saveDataForStations();
        return ResponseEntity.ok().build();
    }

    /**
     * 기본 5개 역에 대해
     * start ~ end (1시간 간격)의 데이터 저장
     * POST /api/weather/history/stations?start=2025-11-29T02:00:00Z&end=2025-12-01T01:00:00Z
     */
    @PostMapping("/history/stations")
    public ResponseEntity<Void> saveHistoryForDefaultStations(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime start,
                                                              @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime end) {
        weatherService.saveHistoryDataForStations(start, end);
        return ResponseEntity.ok().build();
    }
}
