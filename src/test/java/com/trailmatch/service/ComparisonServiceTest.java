package com.trailmatch.service;

import com.trailmatch.dto.ComparisonDtos;
import com.trailmatch.dto.RunnerProfileDtos;
import com.trailmatch.entity.Race;
import com.trailmatch.entity.TechnicalityLevel;
import com.trailmatch.entity.TerrainType;
import com.trailmatch.repository.RaceRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class ComparisonServiceTest {
    @Test void compareReturnsBestAndRiskiest() {
        RaceRepository repo = mock(RaceRepository.class);
        Race r1 = Race.builder().id(1L).name("a").location("x").region("r").date(LocalDate.now()).distanceKm(20.0).elevationGainM(500).terrainType(TerrainType.MIXED).technicalityLevel(TechnicalityLevel.EASY).cutoffTimeMinutes(200).lastFinisherTimeMinutes(180).medianFinisherTimeMinutes(150).aidStationsCount(1).priceEur(BigDecimal.ONE).description("d").build();
        Race r2 = Race.builder().id(2L).name("b").location("x").region("r").date(LocalDate.now()).distanceKm(80.0).elevationGainM(4500).terrainType(TerrainType.MOUNTAIN).technicalityLevel(TechnicalityLevel.HARD).cutoffTimeMinutes(700).lastFinisherTimeMinutes(690).medianFinisherTimeMinutes(600).aidStationsCount(5).priceEur(BigDecimal.ONE).description("d").build();
        when(repo.findAllById(any())).thenReturn(List.of(r1, r2));
        ComparisonService s = new ComparisonService(repo, new DifficultyScoringService(), new CompatibilityScoringService(), new RaceMetricsService());
        var req = new ComparisonDtos.ComparisonRequest(List.of(1L,2L), new RunnerProfileDtos.RunnerProfileRequest(30.0, 1000, 40.0, 5.0, 25.0, "MEDIUM", TerrainType.MIXED, "finish", null, null, null));
        var result = s.compare(req);
        assertEquals(1L, result.bestMatchRaceId());
        assertEquals(2L, result.riskiestRaceId());
    }
}
