package com.trailmatch.service;

import com.trailmatch.dto.ElevationProfilePointDto;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ElevationProfileCalculatorTest {
    private final ElevationProfileCalculator calculator = new ElevationProfileCalculator();

    @Test
    void downsampleKeepsAtMostDefaultMaxPointsAndCriticalEndpoints() {
        List<ElevationProfilePointDto> points = new ArrayList<>();
        for (int i = 0; i < 750; i++) {
            points.add(new ElevationProfilePointDto(i * 0.1, 100 + i));
        }

        List<ElevationProfilePointDto> result = calculator.downsample(points);

        assertTrue(result.size() <= ElevationProfileCalculator.DEFAULT_MAX_POINTS);
        assertEquals(points.getFirst(), result.getFirst());
        assertEquals(points.getLast(), result.getLast());
        for (int i = 1; i < result.size(); i++) {
            assertTrue(result.get(i).distanceKm() > result.get(i - 1).distanceKm());
        }
    }

    @Test
    void downsampleReturnsOriginalListWhenAlreadyUnderLimit() {
        List<ElevationProfilePointDto> points = List.of(
                new ElevationProfilePointDto(0.0, 100.0),
                new ElevationProfilePointDto(1.0, 120.0)
        );

        assertSame(points, calculator.downsample(points));
    }
}
