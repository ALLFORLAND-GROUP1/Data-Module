package com.example.weathermapapi.gis.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class HourlyWeatherDTO {
    private long dt;          // Unix time (초 단위)
    private double temp;      // 온도 (units=metric이면 섭씨)

    private Double pop;       // 강수 확률 (0~1)

    private RainDTO rain;     // {"1h": 0.15} 이런 구조
}
