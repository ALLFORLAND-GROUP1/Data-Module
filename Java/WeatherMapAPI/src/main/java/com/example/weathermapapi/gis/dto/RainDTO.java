package com.example.weathermapapi.gis.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RainDTO {

    // JSON 필드 이름이 "1h"라서 이렇게 매핑
    @JsonProperty("1h")
    private Double oneHour;
}
