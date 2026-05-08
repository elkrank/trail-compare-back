package com.trailmatch.service.gpx;

import java.util.List;

public record ElevationProfile(
        double distanceKm,
        double elevationGainM,
        double elevationLossM,
        Double minElevationM,
        Double maxElevationM,
        List<ElevationProfilePoint> points
) {
    public ElevationProfile {
        points = List.copyOf(points);
    }
}
