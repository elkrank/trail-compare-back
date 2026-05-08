package com.trailmatch.repository;

import com.trailmatch.entity.RaceElevationProfilePoint;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RaceElevationProfilePointRepository extends JpaRepository<RaceElevationProfilePoint, Long> {
    List<RaceElevationProfilePoint> findByRaceIdOrderByPointIndexAsc(Long raceId);
}
