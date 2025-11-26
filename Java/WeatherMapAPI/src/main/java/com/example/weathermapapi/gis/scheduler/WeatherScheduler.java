package com.example.weathermapapi.gis.scheduler;

import com.example.weathermapapi.gis.repository.WeatherRawRepository;
import com.example.weathermapapi.gis.service.WeatherService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
@RequiredArgsConstructor
public class WeatherScheduler {

    private final WeatherService weatherService;

    // 매일 새벽 4시에 실행 (cron: 초 분 시 일 월 요일)
    @Scheduled(cron = "0 23 11 * * *")
    public void ensureTodayMorningData() {
        weatherService.fetchAndSaveForDefaultStations();
    }
}
