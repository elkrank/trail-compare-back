package com.trailmatch.dto;

import com.trailmatch.entity.TechnicalityLevel;
import com.trailmatch.entity.TerrainType;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record RaceRequest(
        @NotBlank String name,
        @NotBlank String location,
        @NotBlank String region,
        @NotNull LocalDate date,
        @NotNull @Positive Double distanceKm,
        @NotNull @Min(0) Integer elevationGainM,
        @NotNull TerrainType terrainType,
        @NotNull TechnicalityLevel technicalityLevel,
        @NotNull @Positive Integer cutoffTimeMinutes,
        @NotNull @Positive Integer lastFinisherTimeMinutes,
        @NotNull @Positive Integer medianFinisherTimeMinutes,
        @NotNull @Min(0) Integer aidStationsCount,
        @NotNull @DecimalMin("0.00") BigDecimal priceEur,
        @NotBlank @Size(max=3000) String description,
        @NotNull @Size(max=10) List<@Size(max=30) String> tags,
        @NotNull Double gradient
) {}
