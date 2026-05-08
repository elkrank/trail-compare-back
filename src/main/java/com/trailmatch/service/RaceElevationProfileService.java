package com.trailmatch.service;

import com.trailmatch.dto.ElevationProfilePointResponse;
import com.trailmatch.dto.RaceElevationProfileResponse;
import com.trailmatch.entity.Race;
import com.trailmatch.entity.RaceElevationProfilePoint;
import com.trailmatch.exception.ApiException;
import com.trailmatch.repository.RaceElevationProfilePointRepository;
import com.trailmatch.repository.RaceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RaceElevationProfileService {
    private final RaceRepository raceRepository;
    private final RaceElevationProfilePointRepository pointRepository;

    public RaceElevationProfileResponse findByRaceId(Long raceId) {
        Race race = raceRepository.findById(raceId)
                .orElseThrow(() -> new ApiException(404, "race_not_found"));

        List<RaceElevationProfilePoint> profilePoints = pointRepository.findByRaceIdOrderByPointIndexAsc(raceId);
        if (profilePoints.isEmpty()) {
            throw new ApiException(404, "elevation_profile_not_found");
        }

        List<ElevationProfilePointResponse> points = profilePoints.stream()
                .map(point -> new ElevationProfilePointResponse(
                        point.getPointIndex(),
                        point.getDistanceKm(),
                        point.getElevationM()))
                .toList();

        return new RaceElevationProfileResponse(
                race.getId(),
                race.getDistanceKm(),
                race.getElevationGainM(),
                race.getElevationLossM(),
                race.getMinElevationM(),
                race.getMaxElevationM(),
                points);
    }
}
