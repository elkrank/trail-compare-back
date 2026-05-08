package com.trailmatch.service.gpx;

/**
 * Point géographique élémentaire issu du parsing d'un fichier GPX.
 */
public record GpxPoint(double latitude, double longitude, double elevationM) {
}
