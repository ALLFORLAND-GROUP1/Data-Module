package com.example.weathermapapi.gis.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;

@Entity
@Table(name = "weather_raw")
@Getter
@Setter
@NoArgsConstructor
public class WeatherRaw {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Double lat;

    private Double lon;

    @Column(name = "ts")
    private OffsetDateTime ts;

    private Double temp;

    private Double rain;

    @Column(columnDefinition = "jsonb")
    private String rawJson;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
