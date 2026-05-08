package com.trailmatch.dto;

public record RaceGpxUploadResponse(
        Long raceId,
        String filename,
        String contentType,
        long sizeBytes,
        String status
) {}
