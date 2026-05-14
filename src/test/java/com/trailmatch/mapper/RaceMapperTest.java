package com.trailmatch.mapper;

import com.trailmatch.dto.RaceResponse;
import com.trailmatch.entity.Race;
import com.trailmatch.entity.TechnicalityLevel;
import com.trailmatch.entity.TerrainType;
import com.trailmatch.service.DifficultyScoringService;
import com.trailmatch.service.RaceMetricsService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RaceMapperTest {
    private final RaceMapper mapper = new RaceMapper(new RaceMetricsService(), new DifficultyScoringService());

    @Test
    void toResponseIncludesGpxMetadataWhenImported() {
        Instant importedAt = Instant.parse("2026-05-14T12:00:00Z");
        Race race = raceBuilder()
                .gpxFileName("trace.gpx")
                .gpxImportedAt(importedAt)
                .build();

        RaceResponse response = mapper.toResponse(race);

        assertTrue(response.hasGpx());
        assertEquals("trace.gpx", response.gpxFileName());
        assertEquals(importedAt, response.gpxImportedAt());
    }

    @Test
    void toResponseMarksMissingGpxWhenNoImportDateExists() {
        Race race = raceBuilder()
                .gpxFileName("trace.gpx")
                .gpxImportedAt(null)
                .build();

        RaceResponse response = mapper.toResponse(race);

        assertFalse(response.hasGpx());
        assertEquals("trace.gpx", response.gpxFileName());
        assertNull(response.gpxImportedAt());
    }

    private Race.RaceBuilder raceBuilder() {
        return Race.builder()
                .id(1L)
                .name("Trail")
                .location("Chamonix")
                .region("Auvergne-Rhône-Alpes")
                .date(LocalDate.of(2026, 6, 1))
                .distanceKm(20.0)
                .elevationGainM(700)
                .terrainType(TerrainType.MOUNTAIN)
                .technicalityLevel(TechnicalityLevel.MODERATE)
                .cutoffTimeMinutes(300)
                .lastFinisherTimeMinutes(280)
                .medianFinisherTimeMinutes(240)
                .aidStationsCount(2)
                .priceEur(BigDecimal.TEN)
                .description("Profiled race")
                .tags(List.of("trail"))
                .sourceUrl("https://example.test/race")
                .createdAt(Instant.parse("2026-05-01T10:00:00Z"))
                .updatedAt(Instant.parse("2026-05-02T10:00:00Z"));
    }
}
