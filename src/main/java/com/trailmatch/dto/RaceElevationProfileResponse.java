package com.trailmatch.dto;

import java.util.List;

public record RaceElevationProfileResponse(
        Long raceId,
        Double distanceKm,
        Integer elevationGainM,
        Integer elevationLossM,
        Integer minElevationM,
        Integer maxElevationM,
        List<ElevationProfilePointResponse> points
) {}
