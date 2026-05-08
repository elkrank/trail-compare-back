package com.trailmatch.service.gpx;

import com.trailmatch.dto.ElevationProfilePointDto;

import java.util.List;

/**
 * Résultat technique calculé à partir des points d'un tracé GPX.
 */
public record ComputedElevationProfile(
        double distanceKm,
        int elevationGainM,
        int elevationLossM,
        int minElevationM,
        int maxElevationM,
        List<ElevationProfilePointDto> points,
        int originalPointsCount
) {
    public ComputedElevationProfile {
        points = List.copyOf(points);
    }
}
