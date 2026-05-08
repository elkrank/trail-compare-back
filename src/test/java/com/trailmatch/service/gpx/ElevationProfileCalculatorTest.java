package com.trailmatch.service.gpx;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ElevationProfileCalculatorTest {
    private final ElevationProfileCalculator calculator = new ElevationProfileCalculator();

    @Test
    void calculatesCumulativeHaversineDistance() {
        ElevationProfile profile = calculator.calculate(new GpxTrack(List.of(
                point(0.0, 0.0, 100.0),
                point(0.0, 1.0, 100.0),
                point(0.0, 2.0, 100.0)
        )));

        assertEquals(222.39, profile.distanceKm(), 0.2);
        assertEquals(111.19, profile.points().get(1).cumulativeDistanceKm(), 0.2);
        assertEquals(profile.distanceKm(), profile.points().getLast().cumulativeDistanceKm());
    }

    @Test
    void calculatesPositiveElevationGainWithThreeMeterNoiseThreshold() {
        ElevationProfile profile = calculator.calculate(new GpxTrack(List.of(
                point(45.0, 6.0, 100.0),
                point(45.0, 6.001, 102.0),
                point(45.0, 6.002, 106.0),
                point(45.0, 6.003, 109.0),
                point(45.0, 6.004, 113.0)
        )));

        assertEquals(8.0, profile.elevationGainM());
    }

    @Test
    void calculatesNegativeElevationLossWithThreeMeterNoiseThreshold() {
        ElevationProfile profile = calculator.calculate(new GpxTrack(List.of(
                point(45.0, 6.0, 120.0),
                point(45.0, 6.001, 118.0),
                point(45.0, 6.002, 114.0),
                point(45.0, 6.003, 111.0),
                point(45.0, 6.004, 106.0)
        )));

        assertEquals(9.0, profile.elevationLossM());
    }

    @Test
    void calculatesMinimumAndMaximumElevation() {
        ElevationProfile profile = calculator.calculate(new GpxTrack(List.of(
                point(45.0, 6.0, 120.0),
                point(45.0, 6.001, 95.0),
                point(45.0, 6.002, 130.0)
        )));

        assertEquals(95.0, profile.minElevationM());
        assertEquals(130.0, profile.maxElevationM());
    }

    @Test
    void downsamplesGeneratedPointsAndKeepsFirstAndLastPoint() {
        List<GpxPoint> points = new ArrayList<>();
        for (int i = 0; i < 101; i++) {
            points.add(point(45.0, 6.0 + i * 0.001, 100.0 + i));
        }

        ElevationProfile profile = calculator.calculate(new GpxTrack(points), 10);

        assertEquals(10, profile.points().size());
        assertEquals(0.0, profile.points().getFirst().cumulativeDistanceKm());
        assertEquals(100.0, profile.points().getFirst().elevationM());
        assertEquals(calculator.calculate(new GpxTrack(points)).distanceKm(), profile.points().getLast().cumulativeDistanceKm());
        assertEquals(200.0, profile.points().getLast().elevationM());
    }

    @Test
    void calculatesMetricsFromOriginalPointsBeforeDownsampling() {
        ElevationProfile profile = calculator.calculate(new GpxTrack(List.of(
                point(45.0, 6.000, 100.0),
                point(45.0, 6.001, 160.0),
                point(45.0, 6.002, 90.0),
                point(45.0, 6.003, 150.0),
                point(45.0, 6.004, 110.0)
        )), 3);

        assertEquals(3, profile.points().size());
        assertEquals(120.0, profile.elevationGainM());
        assertEquals(110.0, profile.elevationLossM());
        assertEquals(90.0, profile.minElevationM());
        assertEquals(160.0, profile.maxElevationM());
    }

    private GpxPoint point(double latitude, double longitude, Double elevationM) {
        return new GpxPoint(latitude, longitude, elevationM, Instant.parse("2026-05-08T08:00:00Z"));
    }
}
