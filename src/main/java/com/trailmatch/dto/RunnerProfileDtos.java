package com.trailmatch.dto;

import com.trailmatch.entity.TerrainType;
import jakarta.validation.constraints.*;

import java.time.Instant;
import java.time.LocalDate;

public class RunnerProfileDtos {
    public record RunnerProfileRequest(
            @NotNull @Min(0) Double maxDistanceKm,
            @NotNull @Min(0) Integer maxElevationGainM,
            @NotNull @Min(0) Double weeklyVolumeKm,
            @NotNull @Min(0) Double weeklyTrainingHours,
            @NotNull @Min(0) Double longestRecentRunKm,
            @NotBlank String currentFitnessLevel,
            @NotNull TerrainType usualTerrain,
            @NotBlank String objective,
            LocalDate targetDate,
            @Positive Double averageEasyPaceMinKm,
            @Positive Double averageTrailPaceMinKm
    ) {}

    public record RunnerProfileResponse(Long id, Double maxDistanceKm, Integer maxElevationGainM, Double weeklyVolumeKm,
                                        Double weeklyTrainingHours, Double longestRecentRunKm, String currentFitnessLevel,
                                        TerrainType usualTerrain, String objective, LocalDate targetDate,
                                        Double averageEasyPaceMinKm, Double averageTrailPaceMinKm,
                                        Instant createdAt, Instant updatedAt) {}
}
