package com.example.weathermapapi.gis.service;

import com.example.weathermapapi.gis.dto.ClosestDaysDTO;
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

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class WeatherServiceImpl implements WeatherService {
    private final RestTemplate restTemplate;
    private final WeatherRawRepository weatherRawRepository;

    @Value("${openweather.api.key}")
    private String apiKey;

    @Value("${openweather.api.base-url:https://api.openweathermap.org/data/3.0/onecall}")
    private String baseUrl;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void fetchAndSaveHourly(double lat, double lon) {

        String url = UriComponentsBuilder.fromHttpUrl(baseUrl)
                .queryParam("lat", lat)
                .queryParam("lon", lon)
                .queryParam("units", "metric")           // 섭씨
                .queryParam("exclude", "minutely,daily,alerts") // current/hourly만 사용
                .queryParam("appid", apiKey)
                .toUriString();

        OpenWeatherResponse response = restTemplate.getForObject(url, OpenWeatherResponse.class);

        if (response == null || response.getHourly() == null) {
            return;
        }

        for (HourlyWeatherDTO h : response.getHourly()) {

            WeatherRaw entity = new WeatherRaw();
            entity.setLat(response.getLat());
            entity.setLon(response.getLon());

            // dt(Unix seconds) -> UTC 기준 OffsetDateTime
            OffsetDateTime ts = OffsetDateTime.ofInstant(
                    java.time.Instant.ofEpochSecond(h.getDt()),
                    ZoneOffset.UTC
            );
            entity.setTs(ts);

            entity.setTemp(h.getTemp());

            Double rainAmount = null;
            if (h.getRain() != null && h.getRain().getOneHour() != null) {
                rainAmount = h.getRain().getOneHour();
            }
            entity.setRain(rainAmount);

            try {
                ObjectMapper mapper = new ObjectMapper();
                String rawJson = mapper.writeValueAsString(response);
                entity.setRawJson(rawJson);
            } catch (JsonProcessingException e) {
                entity.setRawJson(null);
            }

            weatherRawRepository.save(entity);
        }
    }

    @Override
    public void fetchAndSaveForDefaultStations() {
        List<double[]> coords = List.of(
                new double[]{37.553150, 126.972533}, // 서울역
                new double[]{37.567336, 126.829497}, // 마곡나루역
                new double[]{37.521624, 126.924191}, // 여의도역
                new double[]{37.497958, 127.027539}, // 강남역
                new double[]{37.540408, 127.069231}  // 건대입구역
        );

        for (double[] c : coords) {
            fetchAndSaveHourly(c[0], c[1]);
        }
    }

    public ClosestDaysDTO calculateClosestDays() {

        LocalDate today = LocalDate.now();
        DayOfWeek dow = today.getDayOfWeek();

        LocalDate weekdayDate;
        LocalDate saturdayDate;
        LocalDate sundayDate;

        switch (dow) {
            case SATURDAY:
                weekdayDate = today.plusDays(2); // 월요일
                saturdayDate = today;
                sundayDate = today.plusDays(1);
                break;

            case SUNDAY:
                weekdayDate = today.plusDays(1); // 월요일
                saturdayDate = today.minusDays(1);
                sundayDate = today;
                break;

            default: // 평일
                weekdayDate = today;
                saturdayDate = today.with(DayOfWeek.SATURDAY);
                sundayDate = today.with(DayOfWeek.SUNDAY);
                break;
        }

        return new ClosestDaysDTO(weekdayDate, saturdayDate, sundayDate);
    }

    public Map<String, List<WeatherRaw>> getWeatherForClosestDays(String time) {
        ClosestDaysDTO days = calculateClosestDays();

        List<WeatherRaw> weekdayData = weatherRawRepository
                .findByDateAndTime(days.getWeekday(), time);

        List<WeatherRaw> saturdayData = weatherRawRepository
                .findByDateAndTime(days.getSaturday(), time);

        List<WeatherRaw> sundayData = weatherRawRepository
                .findByDateAndTime(days.getSunday(), time);

        Map<String, List<WeatherRaw>> result = new HashMap<>();
        result.put("weekday", weekdayData);
        result.put("saturday", saturdayData);
        result.put("sunday", sundayData);

        return result;
    }

}
