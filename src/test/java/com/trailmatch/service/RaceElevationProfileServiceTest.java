package com.trailmatch.service;

import com.trailmatch.dto.RaceElevationProfileResponse;
import com.trailmatch.entity.Race;
import com.trailmatch.entity.RaceElevationProfilePoint;
import com.trailmatch.entity.TechnicalityLevel;
import com.trailmatch.entity.TerrainType;
import com.trailmatch.exception.ApiException;
import com.trailmatch.repository.RaceElevationProfilePointRepository;
import com.trailmatch.repository.RaceRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RaceElevationProfileServiceTest {
    private final RaceRepository raceRepository = mock(RaceRepository.class);
    private final RaceElevationProfilePointRepository pointRepository = mock(RaceElevationProfilePointRepository.class);
    private final RaceElevationProfileService service = new RaceElevationProfileService(raceRepository, pointRepository);

    @Test
    void returnsProfileWithSummaryAndOrderedPoints() {
        Race race = race();
        when(raceRepository.findById(1L)).thenReturn(Optional.of(race));
        when(pointRepository.findByRaceIdOrderByPointIndexAsc(1L)).thenReturn(List.of(
                point(race, 0, 0.0, 1000),
                point(race, 1, 5.0, 1300),
                point(race, 2, 10.0, 1150),
                point(race, 3, 20.0, 1500)
        ));

        RaceElevationProfileResponse response = service.findByRaceId(1L);

        assertEquals(1L, response.raceId());
        assertEquals(21.5, response.distanceKm());
        assertEquals(777, response.elevationGainM());
        assertEquals(155, response.elevationLossM());
        assertEquals(990, response.minElevationM());
        assertEquals(1510, response.maxElevationM());
        assertEquals(4, response.points().size());
        assertEquals(2, response.points().get(2).pointIndex());
        assertEquals(10.0, response.points().get(2).distanceKm());
        assertEquals(1150, response.points().get(2).elevationM());
    }

    @Test
    void throwsRaceNotFoundWhenRaceDoesNotExist() {
        when(raceRepository.findById(404L)).thenReturn(Optional.empty());

        ApiException exception = assertThrows(ApiException.class, () -> service.findByRaceId(404L));

        assertEquals(404, exception.status);
        assertEquals("race_not_found", exception.getMessage());
        verifyNoInteractions(pointRepository);
    }

    @Test
    void throwsProfileNotFoundWhenRaceHasNoImportedProfile() {
        when(raceRepository.findById(1L)).thenReturn(Optional.of(race()));
        when(pointRepository.findByRaceIdOrderByPointIndexAsc(1L)).thenReturn(List.of());

        ApiException exception = assertThrows(ApiException.class, () -> service.findByRaceId(1L));

        assertEquals(404, exception.status);
        assertEquals("elevation_profile_not_found", exception.getMessage());
    }

    private Race race() {
        return Race.builder()
                .id(1L)
                .name("Trail")
                .location("Chamonix")
                .region("Auvergne-Rhône-Alpes")
                .date(LocalDate.now())
                .distanceKm(21.5)
                .elevationGainM(777)
                .elevationLossM(155)
                .minElevationM(990)
                .maxElevationM(1510)
                .terrainType(TerrainType.MOUNTAIN)
                .technicalityLevel(TechnicalityLevel.MODERATE)
                .cutoffTimeMinutes(300)
                .lastFinisherTimeMinutes(280)
                .medianFinisherTimeMinutes(240)
                .aidStationsCount(2)
                .priceEur(BigDecimal.TEN)
                .description("Profiled race")
                .build();
    }

    private RaceElevationProfilePoint point(Race race, int index, double distanceKm, int elevationM) {
        return RaceElevationProfilePoint.builder()
                .race(race)
                .pointIndex(index)
                .distanceKm(distanceKm)
                .elevationM(elevationM)
                .build();
    }
}
