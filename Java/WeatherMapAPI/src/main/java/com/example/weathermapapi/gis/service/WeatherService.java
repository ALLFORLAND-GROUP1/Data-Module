package com.example.weathermapapi.gis.service;

public interface WeatherService {
    /**
     * 지정된 위도/경도에 대해 One Call API 호출 후
     * hourly 데이터를 weather_raw 테이블에 저장
     */
    void fetchAndSaveHourly(double lat, double lon);

    void fetchAndSaveForDefaultStations();

}
