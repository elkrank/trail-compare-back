package com.trailmatch.service.gpx;

import java.time.Instant;

public record GpxPoint(double latitude, double longitude, Double elevationM, Instant time) {
}
