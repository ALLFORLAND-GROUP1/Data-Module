package com.example.weathermapapi.gis.repository;

import com.example.weathermapapi.gis.entity.WeatherRaw;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface WeatherRawRepository extends JpaRepository<WeatherRaw, Long> {

    @Query(value = """
        SELECT *
        FROM weather_raw
        WHERE DATE(ts) = :date AND TO_CHAR(ts, 'HH24:MI') = :time
        """,
            nativeQuery = true)
    List<WeatherRaw> findByDateAndTime(
            @Param("date") LocalDate date,
            @Param("time") String time
    );
}
