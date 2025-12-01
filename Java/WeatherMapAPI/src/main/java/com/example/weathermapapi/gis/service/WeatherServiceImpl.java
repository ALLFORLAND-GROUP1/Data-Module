package com.example.weathermapapi.gis.service;

import com.example.weathermapapi.gis.dto.HourlyWeatherDTO;
import com.example.weathermapapi.gis.dto.OpenWeatherResponse;
import com.example.weathermapapi.gis.entity.WeatherRaw;
import com.example.weathermapapi.gis.repository.WeatherRawRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.*;
import java.util.List;

@Service
@RequiredArgsConstructor
public class WeatherServiceImpl implements WeatherService {
    private final RestTemplate restTemplate;
    private final WeatherRawRepository weatherRawRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${openweather.api.key}")
    private String apiKey;

    @Value("${openweather.api.base-url:https://api.openweathermap.org/data/3.0/onecall}")
    private String baseUrl;

    @Value("${openweather.api.timemachine-url:https://api.openweathermap.org/data/3.0/onecall/timemachine}")
    private String timemachineUrl;

    // 공통: 지정된 5개 역 좌표
    private List<double[]> getDefaultStations() {
        return List.of(
                new double[]{37.553150, 126.972533}, // 서울역
                new double[]{37.567336, 126.829497}, // 마곡나루역
                new double[]{37.521624, 126.924191}, // 여의도역
                new double[]{37.497958, 127.027539}, // 강남역
                new double[]{37.540408, 127.069231}  // 건대입구역
        );
    }

    // 공통: URL 호출 및 저장
    private void callAndSave(String url) {
        OpenWeatherResponse response = restTemplate.getForObject(url, OpenWeatherResponse.class);
        if (response == null) {
            return;
        }

        List<HourlyWeatherDTO> list = null;

        // 현재 날씨 응답
        if (response.getHourly() != null && !response.getHourly().isEmpty()) {
            list = response.getHourly();
        }
        // 과거 날씨 응답
        else if (response.getData() != null && !response.getData().isEmpty()) {
            list = response.getData();
        }

        if (list != null) {
            for (HourlyWeatherDTO h : list) {
                OffsetDateTime ts = OffsetDateTime.ofInstant(java.time.Instant.ofEpochSecond(h.getDt()), ZoneOffset.UTC);

                Double lat = response.getLat();
                Double lon = response.getLon();

                boolean exists = weatherRawRepository.existsByLatAndLonAndTs(lat, lon, ts);
                if (exists) {
                    continue;
                }

                WeatherRaw entity = new WeatherRaw();
                entity.setLat(response.getLat());
                entity.setLon(response.getLon());
                entity.setTs(ts);
                entity.setTemp(h.getTemp());

                Double rainAmount = null;
                if (h.getRain() != null && h.getRain().getOneHour() != null) {
                    rainAmount = h.getRain().getOneHour();
                }
                entity.setRain(rainAmount);

                try {
                    String rawJson = objectMapper.writeValueAsString(response);
                    entity.setRawJson(rawJson);
                } catch (JsonProcessingException e) {
                    entity.setRawJson(null);
                }

                weatherRawRepository.save(entity);
            }
        }
    }

    // 1) 역 1개 예보(48시간) 호출
    private void fetchForecastForStation(double lat, double lon) {
        String url = UriComponentsBuilder.fromHttpUrl(baseUrl)
                .queryParam("lat", lat)
                .queryParam("lon", lon)
                .queryParam("units", "metric")           // 섭씨
                .queryParam("exclude", "minutely,daily,alerts") // current/hourly만 사용
                .queryParam("appid", apiKey)
                .toUriString();

        callAndSave(url);
    }

    // 2) 역 1개 과거 구간(start~end, 1시간 간격) 호출
    private void fetchHistoryForStationInRange(double lat, double lon, OffsetDateTime start, OffsetDateTime end) {
        for (OffsetDateTime t = start; !t.isAfter(end); t = t.plusHours(1)) {
            long epochSecond = t.toEpochSecond(); // start/end 를 +00 기준으로 넘긴다고 가정

            String url = UriComponentsBuilder.fromHttpUrl(timemachineUrl)
                    .queryParam("lat", lat)
                    .queryParam("lon", lon)
                    .queryParam("units", "metric")
                    .queryParam("dt", epochSecond)
                    .queryParam("appid", apiKey)
                    .toUriString();

            callAndSave(url);
        }
    }

    // 기본 5개 역에 대해 48시간 예보 저장
    @Override
    public void saveDataForStations() {
        for (double[] c : getDefaultStations()) {
            fetchForecastForStation(c[0], c[1]);
        }
    }

    // 기본 5개 역에 대해 start~end(1시간 간격) 데이터 저장
    @Override
    public void saveHistoryDataForStations(OffsetDateTime start, OffsetDateTime end) {
        for (double[] c : getDefaultStations()) {
            fetchHistoryForStationInRange(c[0], c[1], start, end);
        }
    }

}
