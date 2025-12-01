package com.example.weathermapapi.gis.scheduler;

import com.example.weathermapapi.gis.service.WeatherService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class WeatherScheduler {

    private final WeatherService weatherService;

    // 매일 새벽 4시에 기본 5개 역 48시간 예보 저장
    @Scheduled(cron = "0 0 4 * * *", zone = "Asia/Seoul")
    public void saveForecastDaily() {
        log.info("Scheduler 실행");
        weatherService.saveDataForStations();
    }
}
