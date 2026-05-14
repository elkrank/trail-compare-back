package com.trailmatch.service;

import com.trailmatch.dto.RaceGpxUploadResponse;
import com.trailmatch.entity.Race;
import com.trailmatch.entity.RaceElevationProfilePoint;
import com.trailmatch.entity.RaceGpxFile;
import com.trailmatch.exception.ApiException;
import com.trailmatch.repository.RaceElevationProfilePointRepository;
import com.trailmatch.repository.RaceGpxFileRepository;
import com.trailmatch.repository.RaceRepository;
import com.trailmatch.service.gpx.ElevationProfile;
import com.trailmatch.service.gpx.ElevationProfileCalculator;
import com.trailmatch.service.gpx.GpxParser;
import com.trailmatch.service.gpx.GpxTrack;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
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
    private final RaceGpxFileRepository gpxFileRepository;
    private final GpxParser gpxParser;
    private final ElevationProfileCalculator elevationProfileCalculator;

    @Transactional
    public RaceGpxUploadResponse upload(Long raceId, MultipartFile file) {
        Race race = raceRepository.findById(raceId)
                .orElseThrow(() -> new ApiException(404, "race_not_found"));
        return importForRace(race, file);
    }

    @Transactional
    public RaceGpxUploadResponse importForRace(Race race, MultipartFile file) {
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

        byte[] content = readFileBytes(file);
        ElevationProfile profile = parseProfile(content);
        Race savedRace = persistRaceSummary(race, filename, profile);
        persistRawGpxFile(savedRace, file, filename, content);
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

    private byte[] readFileBytes(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException ex) {
            throw new ApiException(400, "invalid_gpx");
        }
    }

    private ElevationProfile parseProfile(byte[] content) {
        GpxTrack track = gpxParser.parse(new ByteArrayInputStream(content));
        validateParsedTrack(track);
        return elevationProfileCalculator.calculate(track, MAX_PROFILE_POINTS);
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

    private void persistRawGpxFile(Race race, MultipartFile file, String filename, byte[] content) {
        gpxFileRepository.save(RaceGpxFile.builder()
                .raceId(race.getId())
                .race(race)
                .fileName(filename)
                .contentType(file.getContentType())
                .sizeBytes((long) content.length)
                .sha256(sha256Hex(content))
                .content(content)
                .importedAt(race.getGpxImportedAt())
                .build());
    }

    private String sha256Hex(byte[] content) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(content);
            StringBuilder hex = new StringBuilder(digest.length * 2);
            for (byte value : digest) {
                hex.append(String.format("%02x", value));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 algorithm is not available", ex);
        }
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
