package com.trailmatch.dto;

/**
 * Point du profil altimétrique exposant l'altitude à une distance cumulée donnée.
 */
public record ElevationProfilePointDto(double distanceKm, int elevationM) {
}
