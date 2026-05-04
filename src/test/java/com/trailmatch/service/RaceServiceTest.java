package com.trailmatch.service;

import com.trailmatch.entity.Race;
import com.trailmatch.entity.TechnicalityLevel;
import com.trailmatch.entity.TerrainType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class RaceServiceTest {
    private Race race(double distance, int dplus, int cutoff, int last, TechnicalityLevel tech) {
        return Race.builder().id(1L).name("r").location("l").region("x").date(LocalDate.now()).distanceKm(distance).elevationGainM(dplus)
                .terrainType(TerrainType.MOUNTAIN).technicalityLevel(tech).cutoffTimeMinutes(cutoff).lastFinisherTimeMinutes(last)
                .medianFinisherTimeMinutes((cutoff+last)/2).aidStationsCount(2).priceEur(BigDecimal.TEN).description("d").build();
    }

    @Test void metricsAndDifficultyAreDeterministic() {
        RaceMetricsService m = new RaceMetricsService();
        DifficultyScoringService d = new DifficultyScoringService();
        Race r = race(50, 3000, 600, 570, TechnicalityLevel.HARD);
        assertEquals(60.0, m.cutoffPace(r), 0.0001);
        assertTrue(d.score(r) > 70);
    }
}
