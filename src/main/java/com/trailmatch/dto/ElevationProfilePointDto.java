package com.trailmatch.dto;

import java.io.Serializable;

public record ElevationProfilePointDto(
        Double distanceKm,
        Integer elevationM
) implements Serializable {}
