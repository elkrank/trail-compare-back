package com.trailmatch.service.gpx;

import com.trailmatch.dto.ElevationProfilePointDto;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ElevationProfileCalculator {
    private static final double EARTH_RADIUS_KM = 6_371.0088;
    private static final double MIN_ELEVATION_DELTA_M = 3.0;

    public ComputedElevationProfile compute(List<GpxPoint> gpxPoints) {
        if (gpxPoints == null || gpxPoints.isEmpty()) {
            return new ComputedElevationProfile(0.0, 0, 0, 0, 0, List.of(), 0);
        }

        List<ElevationProfilePointDto> profilePoints = new ArrayList<>(gpxPoints.size());
        GpxPoint firstPoint = gpxPoints.getFirst();
        double cumulativeDistanceKm = 0.0;
        double elevationGainM = 0.0;
        double elevationLossM = 0.0;
        double minElevationM = firstPoint.elevationM();
        double maxElevationM = firstPoint.elevationM();

        profilePoints.add(new ElevationProfilePointDto(0.0, roundToInt(firstPoint.elevationM())));

        for (int i = 1; i < gpxPoints.size(); i++) {
            GpxPoint previous = gpxPoints.get(i - 1);
            GpxPoint current = gpxPoints.get(i);

            cumulativeDistanceKm += distanceKmBetween(previous, current);

            double elevationDeltaM = current.elevationM() - previous.elevationM();
            if (Math.abs(elevationDeltaM) >= MIN_ELEVATION_DELTA_M) {
                if (elevationDeltaM > 0) {
                    elevationGainM += elevationDeltaM;
                } else {
                    elevationLossM += Math.abs(elevationDeltaM);
                }
            }

            minElevationM = Math.min(minElevationM, current.elevationM());
            maxElevationM = Math.max(maxElevationM, current.elevationM());
            profilePoints.add(new ElevationProfilePointDto(
                    roundDistanceKm(cumulativeDistanceKm, 3),
                    roundToInt(current.elevationM())
            ));
        }

        return new ComputedElevationProfile(
                roundDistanceKm(cumulativeDistanceKm, 2),
                roundToInt(elevationGainM),
                roundToInt(elevationLossM),
                roundToInt(minElevationM),
                roundToInt(maxElevationM),
                profilePoints,
                gpxPoints.size()
        );
    }

    public double distanceKmBetween(GpxPoint first, GpxPoint second) {
        double latitudeDelta = Math.toRadians(second.latitude() - first.latitude());
        double longitudeDelta = Math.toRadians(second.longitude() - first.longitude());
        double firstLatitude = Math.toRadians(first.latitude());
        double secondLatitude = Math.toRadians(second.latitude());

        double a = Math.pow(Math.sin(latitudeDelta / 2), 2)
                + Math.cos(firstLatitude) * Math.cos(secondLatitude) * Math.pow(Math.sin(longitudeDelta / 2), 2);
        double clampedA = Math.min(1.0, a);
        double c = 2 * Math.atan2(Math.sqrt(clampedA), Math.sqrt(1 - clampedA));

        return EARTH_RADIUS_KM * c;
    }

    private static double roundDistanceKm(double value, int decimals) {
        double scale = Math.pow(10, decimals);
        return Math.round(value * scale) / scale;
    }

    private static int roundToInt(double value) {
        return (int) Math.round(value);
    }
}
