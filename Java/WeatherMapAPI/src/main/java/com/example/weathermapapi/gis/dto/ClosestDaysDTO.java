package com.example.weathermapapi.gis.dto;

import lombok.*;

import java.time.LocalDate;

@Getter
@Data
@AllArgsConstructor
public class ClosestDaysDTO {
    private LocalDate weekday;
    private LocalDate saturday;
    private LocalDate sunday;
}
