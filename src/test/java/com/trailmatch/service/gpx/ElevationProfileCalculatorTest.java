package com.trailmatch.service.gpx;

import com.trailmatch.dto.ElevationProfilePointDto;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class ElevationProfileCalculatorTest {
    private final ElevationProfileCalculator calculator = new ElevationProfileCalculator();

    @Test
    void distanceKmBetweenUsesHaversineFormulaForKnownPoints() {
        GpxPoint paris = new GpxPoint(48.8566, 2.3522, 35.0);
        GpxPoint london = new GpxPoint(51.5074, -0.1278, 11.0);

        double distanceKm = calculator.distanceKmBetween(paris, london);

        assertThat(distanceKm).isCloseTo(343.56, within(2.0));
    }

    @Test
    void computeBuildsProfileAndIgnoresSmallElevationNoise() {
        List<GpxPoint> points = List.of(
                new GpxPoint(45.0, 6.0, 100.0),
                new GpxPoint(45.0, 6.01, 102.9),
                new GpxPoint(45.01, 6.01, 106.0),
                new GpxPoint(45.01, 6.02, 101.0)
        );

        ComputedElevationProfile profile = calculator.compute(points);

        assertThat(profile.originalPointsCount()).isEqualTo(4);
        assertThat(profile.distanceKm()).isEqualTo(2.68);
        assertThat(profile.elevationGainM()).isEqualTo(3);
        assertThat(profile.elevationLossM()).isEqualTo(5);
        assertThat(profile.minElevationM()).isEqualTo(100);
        assertThat(profile.maxElevationM()).isEqualTo(106);
        assertThat(profile.points()).extracting(ElevationProfilePointDto::distanceKm).containsExactly(0.0, 0.786, 1.898, 2.684);
        assertThat(profile.points()).extracting(ElevationProfilePointDto::elevationM).containsExactly(100, 103, 106, 101);
    }

    @Test
    void computeReturnsEmptyProfileForEmptyInput() {
        ComputedElevationProfile profile = calculator.compute(List.of());

        assertThat(profile.distanceKm()).isZero();
        assertThat(profile.elevationGainM()).isZero();
        assertThat(profile.elevationLossM()).isZero();
        assertThat(profile.minElevationM()).isZero();
        assertThat(profile.maxElevationM()).isZero();
        assertThat(profile.points()).isEmpty();
        assertThat(profile.originalPointsCount()).isZero();
    }
}
