package com.trailmatch.dto;

import com.trailmatch.entity.TechnicalityLevel;
import com.trailmatch.entity.TerrainType;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record RaceResponse(Long id, String name, String location, String region, LocalDate date, Double distanceKm,
                           Integer elevationGainM, TerrainType terrainType, TechnicalityLevel technicalityLevel,
                           Integer cutoffTimeMinutes, Integer lastFinisherTimeMinutes, Integer medianFinisherTimeMinutes,
                           Integer aidStationsCount, BigDecimal priceEur, String description, List<String> tags,
                           String sourceUrl, Instant createdAt, Instant updatedAt,
                           Double elevationPerKm, Double cutoffPaceMinKm, Double lastFinisherPaceMinKm,
                           Double medianPaceMinKm, Integer difficultyScore) {}
