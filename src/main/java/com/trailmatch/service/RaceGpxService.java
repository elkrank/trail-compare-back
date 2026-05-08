package com.trailmatch.service;

import com.trailmatch.entity.Race;
import com.trailmatch.entity.RaceElevationProfilePoint;
import com.trailmatch.exception.ApiException;
import com.trailmatch.repository.RaceElevationProfilePointRepository;
import com.trailmatch.repository.RaceRepository;
import com.trailmatch.service.gpx.ElevationProfile;
import com.trailmatch.service.gpx.ElevationProfileCalculator;
import com.trailmatch.service.gpx.GpxParser;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RaceGpxService {
    private static final int MAX_PROFILE_POINTS = 500;

    private final RaceRepository raceRepository;
    private final RaceElevationProfilePointRepository pointRepository;
    private final GpxParser gpxParser;
    private final ElevationProfileCalculator elevationProfileCalculator;

    @Transactional
    public ElevationProfile upload(Long raceId, MultipartFile file) {
        Race race = raceRepository.findById(raceId)
                .orElseThrow(() -> new ApiException(404, "race_not_found"));

        if (file == null || file.isEmpty()) {
            throw new ApiException(400, "gpx_file_required");
        }

        String filename = file.getOriginalFilename();
        if (filename == null || !filename.toLowerCase().endsWith(".gpx")) {
            throw new ApiException(400, "invalid_gpx_file_extension");
        }

        ElevationProfile profile = parseProfile(file);
        persistRaceSummary(race, filename, profile);
        replaceProfilePoints(race, profile);

        return profile;
    }

    private ElevationProfile parseProfile(MultipartFile file) {
        try {
            return elevationProfileCalculator.calculate(gpxParser.parse(file.getInputStream()), MAX_PROFILE_POINTS);
        } catch (IOException ex) {
            throw new ApiException(400, "invalid_gpx");
        }
    }

    private void persistRaceSummary(Race race, String filename, ElevationProfile profile) {
        race.setDistanceKm(profile.distanceKm());
        race.setElevationGainM(roundNullable(profile.elevationGainM()));
        race.setElevationLossM(roundNullable(profile.elevationLossM()));
        race.setMinElevationM(roundNullable(profile.minElevationM()));
        race.setMaxElevationM(roundNullable(profile.maxElevationM()));
        race.setGpxFileName(filename);
        race.setGpxImportedAt(Instant.now());
        raceRepository.save(race);
    }

    private void replaceProfilePoints(Race race, ElevationProfile profile) {
        pointRepository.deleteByRaceId(race.getId());

        List<RaceElevationProfilePoint> points = new ArrayList<>();
        int pointIndex = 0;
        for (var profilePoint : profile.points()) {
            if (profilePoint.elevationM() == null) {
                continue;
            }
            points.add(RaceElevationProfilePoint.builder()
                    .race(race)
                    .pointIndex(pointIndex++)
                    .distanceKm(profilePoint.cumulativeDistanceKm())
                    .elevationM(roundNullable(profilePoint.elevationM()))
                    .build());
        }
        pointRepository.saveAll(points);
    }

    private Integer roundNullable(Double value) {
        return value == null ? null : (int) Math.round(value);
    }

    private Integer roundNullable(double value) {
        return (int) Math.round(value);
    }
}
