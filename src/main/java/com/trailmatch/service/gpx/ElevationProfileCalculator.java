package com.trailmatch.service.gpx;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Component
public class ElevationProfileCalculator {
    private static final double EARTH_RADIUS_KM = 6_371.0088;
    private static final double DEFAULT_NOISE_THRESHOLD_M = 3.0;

    public ElevationProfile calculate(GpxTrack track) {
        return calculate(track, Integer.MAX_VALUE);
    }

    public ElevationProfile calculate(GpxTrack track, int maxPoints) {
        List<GpxPoint> points = track.points();
        List<ElevationProfilePoint> profilePoints = new ArrayList<>();
        double cumulativeDistanceKm = 0.0;
        double elevationGainM = 0.0;
        double elevationLossM = 0.0;

        for (int i = 0; i < points.size(); i++) {
            GpxPoint current = points.get(i);
            if (i > 0) {
                GpxPoint previous = points.get(i - 1);
                cumulativeDistanceKm += haversineKm(previous.latitude(), previous.longitude(), current.latitude(), current.longitude());

                if (previous.elevationM() != null && current.elevationM() != null) {
                    double delta = current.elevationM() - previous.elevationM();
                    if (delta > DEFAULT_NOISE_THRESHOLD_M) {
                        elevationGainM += delta;
                    } else if (delta < -DEFAULT_NOISE_THRESHOLD_M) {
                        elevationLossM += Math.abs(delta);
                    }
                }
            }
            profilePoints.add(new ElevationProfilePoint(cumulativeDistanceKm, current.elevationM()));
        }

        Double minElevationM = points.stream().map(GpxPoint::elevationM).filter(java.util.Objects::nonNull).min(Comparator.naturalOrder()).orElse(null);
        Double maxElevationM = points.stream().map(GpxPoint::elevationM).filter(java.util.Objects::nonNull).max(Comparator.naturalOrder()).orElse(null);

        return new ElevationProfile(cumulativeDistanceKm, elevationGainM, elevationLossM, minElevationM, maxElevationM, downsample(profilePoints, maxPoints));
    }

    private double haversineKm(double lat1, double lon1, double lat2, double lon2) {
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.pow(Math.sin(latDistance / 2), 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * Math.pow(Math.sin(lonDistance / 2), 2);
        return EARTH_RADIUS_KM * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    private List<ElevationProfilePoint> downsample(List<ElevationProfilePoint> points, int maxPoints) {
        if (maxPoints < 2) {
            throw new IllegalArgumentException("maxPoints must be greater than or equal to 2");
        }
        if (points.size() <= maxPoints) {
            return points;
        }

        List<ElevationProfilePoint> sampled = new ArrayList<>(maxPoints);
        int lastIndex = points.size() - 1;
        for (int i = 0; i < maxPoints; i++) {
            int sourceIndex = (int) Math.round(i * lastIndex / (double) (maxPoints - 1));
            sampled.add(points.get(sourceIndex));
        }
        return sampled;
    }
}
