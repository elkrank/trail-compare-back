package com.trailmatch.service.gpx;

import java.util.List;

public record GpxTrack(List<GpxPoint> points) {
    public GpxTrack {
        points = List.copyOf(points);
    }
}
