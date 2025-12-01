package com.example.weathermapapi.gis.service;

import java.time.OffsetDateTime;

public interface WeatherService {

    /**
     * 기본 5개 역에 대해
     * One Call API 호출 후 hourly를 저장
     */
    void saveDataForStations();

    /**
     * 기본 5개 역에 대해
     * start~end(1시간 간격) 구간의 데이터를 저장
     */
    void saveHistoryDataForStations(OffsetDateTime start, OffsetDateTime end);

}
