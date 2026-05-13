package com.trailmatch.service;

import com.trailmatch.dto.RaceGpxUploadResponse;
import com.trailmatch.entity.Race;
import com.trailmatch.entity.RaceElevationProfilePoint;
import com.trailmatch.exception.ApiException;
import com.trailmatch.repository.RaceElevationProfilePointRepository;
import com.trailmatch.repository.RaceRepository;
import com.trailmatch.service.gpx.ElevationProfile;
import com.trailmatch.service.gpx.ElevationProfileCalculator;
import com.trailmatch.service.gpx.GpxParser;
import com.trailmatch.service.gpx.GpxTrack;
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
    static final long MAX_GPX_FILE_SIZE_BYTES = 10 * 1024 * 1024;
    private static final int MAX_PROFILE_POINTS = 500;

    private final RaceRepository raceRepository;
    private final RaceElevationProfilePointRepository pointRepository;
    private final GpxParser gpxParser;
    private final ElevationProfileCalculator elevationProfileCalculator;

    @Transactional
    public RaceGpxUploadResponse upload(Long raceId, MultipartFile file) {
        Race race = raceRepository.findById(raceId)
                .orElseThrow(() -> new ApiException(404, "race_not_found"));

        if (file == null) {
            throw new ApiException(400, "gpx_file_required");
        }
        if (file.isEmpty()) {
            throw new ApiException(400, "gpx_file_empty");
        }
        if (file.getSize() > MAX_GPX_FILE_SIZE_BYTES) {
            throw new ApiException(413, "gpx_file_too_large");
        }

        String filename = file.getOriginalFilename();
        if (filename == null || !filename.toLowerCase().endsWith(".gpx")) {
            throw new ApiException(400, "invalid_gpx_file_extension");
        }

        ElevationProfile profile = parseProfile(file);
        Race savedRace = persistRaceSummary(race, filename, profile);
        int pointsCount = replaceProfilePoints(savedRace, profile);

        return new RaceGpxUploadResponse(
                savedRace.getId(),
                savedRace.getGpxFileName(),
                pointsCount,
                savedRace.getDistanceKm(),
                savedRace.getElevationGainM(),
                savedRace.getElevationLossM(),
                savedRace.getMinElevationM(),
                savedRace.getMaxElevationM());
    }

    private ElevationProfile parseProfile(MultipartFile file) {
        try {
            GpxTrack track = gpxParser.parse(file.getInputStream());
            validateParsedTrack(track);
            return elevationProfileCalculator.calculate(track, MAX_PROFILE_POINTS);
        } catch (IOException ex) {
            throw new ApiException(400, "invalid_gpx");
        }
    }

    private void validateParsedTrack(GpxTrack track) {
        if (track.points().isEmpty()) {
            throw new ApiException(400, "gpx_no_usable_point");
        }
        boolean hasElevation = track.points().stream().anyMatch(point -> point.elevationM() != null);
        if (!hasElevation) {
            throw new ApiException(400, "gpx_no_elevation_data");
        }
    }

    private Race persistRaceSummary(Race race, String filename, ElevationProfile profile) {
        race.setDistanceKm(profile.distanceKm());
        race.setElevationGainM(roundNullable(profile.elevationGainM()));
        race.setElevationLossM(roundNullable(profile.elevationLossM()));
        race.setMinElevationM(roundNullable(profile.minElevationM()));
        race.setMaxElevationM(roundNullable(profile.maxElevationM()));
        race.setGpxFileName(filename);
        race.setGpxImportedAt(Instant.now());
        return raceRepository.save(race);
    }

    private int replaceProfilePoints(Race race, ElevationProfile profile) {
        pointRepository.deleteByRaceId(race.getId());
        pointRepository.flush();

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
        Iterable<RaceElevationProfilePoint> savedPoints = pointRepository.saveAll(points);
        int savedCount = 0;
        for (RaceElevationProfilePoint ignored : savedPoints) {
            savedCount++;
        }
        return savedCount;
    }

    private Integer roundNullable(Double value) {
        return value == null ? null : (int) Math.round(value);
    }

    private Integer roundNullable(double value) {
        return (int) Math.round(value);
    }
}
