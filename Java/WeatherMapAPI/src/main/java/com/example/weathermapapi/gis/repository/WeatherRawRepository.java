package com.example.weathermapapi.gis.repository;

import com.example.weathermapapi.gis.entity.WeatherRaw;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;

@Repository
public interface WeatherRawRepository extends JpaRepository<WeatherRaw, Long> {

    boolean existsByLatAndLonAndTs(Double lat, Double lon, OffsetDateTime ts);
}
