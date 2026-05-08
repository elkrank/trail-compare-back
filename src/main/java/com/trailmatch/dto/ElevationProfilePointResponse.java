package com.trailmatch.dto;

public record ElevationProfilePointResponse(
        Integer pointIndex,
        Double distanceKm,
        Integer elevationM
) {}
