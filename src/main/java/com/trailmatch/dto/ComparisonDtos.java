package com.trailmatch.dto;

import com.trailmatch.entity.RiskLevel;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.util.List;

public class ComparisonDtos {
    public record ComparisonRequest(@NotNull @Size(min = 2, max = 4) List<Long> raceIds,
                                    @NotNull @Valid RunnerProfileDtos.RunnerProfileRequest runnerProfile) {}

    public record ComparisonResultDto(Long raceId, Integer difficultyScore, Integer compatibilityScore,
                                      Double cutoffPaceMinKm, Double lastFinisherPaceMinKm, Double medianPaceMinKm,
                                      RiskLevel riskLevel, String recommendationLabel, List<String> strengths,
                                      List<String> warnings) {}

    public record ComparisonResponse(List<ComparisonResultDto> results, Long bestMatchRaceId, Long easiestRaceId,
                                     Long riskiestRaceId, String summary) {}
}
