package com.example.weathermapapi.gis.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class OpenWeatherResponse {

    private double lat;
    private double lon;

    private List<HourlyWeatherDTO> hourly;
}
