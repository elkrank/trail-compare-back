package com.trailmatch.dto;

public record RaceGpxUploadResponse(
        Long raceId,
        String fileName,
        Integer pointsCount,
        Double distanceKm,
        Integer elevationGainM,
        Integer elevationLossM,
        Integer minElevationM,
        Integer maxElevationM
) {}
