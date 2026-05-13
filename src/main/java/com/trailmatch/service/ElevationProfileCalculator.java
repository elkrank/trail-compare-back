package com.trailmatch.service;

import com.trailmatch.dto.ElevationProfilePointDto;
import java.util.ArrayList;
import java.util.List;

public class ElevationProfileCalculator {
    public static final int DEFAULT_MAX_POINTS = 500;

    public List<ElevationProfilePointDto> downsample(List<ElevationProfilePointDto> points) {
        return downsample(points, DEFAULT_MAX_POINTS);
    }

    public List<ElevationProfilePointDto> downsample(List<ElevationProfilePointDto> points, int maxPoints) {
        if (points.size() <= maxPoints) {
            return points;
        }

        if (maxPoints < 2) {
            throw new IllegalArgumentException("maxPoints must be at least 2 to keep the first and last points");
        }

        int lastIndex = points.size() - 1;
        List<ElevationProfilePointDto> downsampled = new ArrayList<>(maxPoints);
        downsampled.add(points.getFirst());

        for (int targetIndex = 1; targetIndex < maxPoints - 1; targetIndex++) {
            int sourceIndex = (int) Math.floor((double) targetIndex * lastIndex / (maxPoints - 1));
            downsampled.add(points.get(sourceIndex));
        }

        downsampled.add(points.getLast());
        return downsampled;
    }
}
