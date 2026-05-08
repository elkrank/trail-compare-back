package com.trailmatch.service;

import com.trailmatch.dto.RaceGpxUploadResponse;
import com.trailmatch.entity.Race;
import com.trailmatch.entity.RaceElevationProfilePoint;
import com.trailmatch.entity.TechnicalityLevel;
import com.trailmatch.entity.TerrainType;
import com.trailmatch.exception.ApiException;
import com.trailmatch.repository.RaceElevationProfilePointRepository;
import com.trailmatch.repository.RaceRepository;
import com.trailmatch.service.gpx.ElevationProfile;
import com.trailmatch.service.gpx.ElevationProfileCalculator;
import com.trailmatch.service.gpx.ElevationProfilePoint;
import com.trailmatch.service.gpx.GpxParser;
import com.trailmatch.service.gpx.GpxTrack;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class RaceGpxServiceTest {
    private final RaceRepository raceRepository = mock(RaceRepository.class);
    private final RaceElevationProfilePointRepository pointRepository = mock(RaceElevationProfilePointRepository.class);
    private final GpxParser gpxParser = mock(GpxParser.class);
    private final ElevationProfileCalculator elevationProfileCalculator = mock(ElevationProfileCalculator.class);
    private final RaceGpxService service = new RaceGpxService(raceRepository, pointRepository, gpxParser, elevationProfileCalculator);

    @Test
    void uploadPersistsRaceSummaryAndReplacesDownsampledProfilePoints() {
        Race race = race();
        GpxTrack track = new GpxTrack(List.of());
        ElevationProfile profile = new ElevationProfile(
                12.34,
                456.7,
                123.4,
                987.6,
                1543.2,
                List.of(
                        new ElevationProfilePoint(0.0, 987.6),
                        new ElevationProfilePoint(6.0, 1200.2),
                        new ElevationProfilePoint(12.34, 1543.2)
                ));
        when(raceRepository.findById(42L)).thenReturn(Optional.of(race));
        when(gpxParser.parse(any())).thenReturn(track);
        when(elevationProfileCalculator.calculate(eq(track), eq(500))).thenReturn(profile);
        MockMultipartFile file = new MockMultipartFile("file", "trace.gpx", "application/gpx+xml", "<gpx></gpx>".getBytes(StandardCharsets.UTF_8));

        RaceGpxUploadResponse response = service.upload(42L, file);

        assertEquals(42L, response.raceId());
        assertEquals("trace.gpx", response.filename());
        assertEquals("application/gpx+xml", response.contentType());
        assertEquals(file.getSize(), response.sizeBytes());
        assertEquals("uploaded", response.status());
        assertEquals(12.34, race.getDistanceKm());
        assertEquals(457, race.getElevationGainM());
        assertEquals(123, race.getElevationLossM());
        assertEquals(988, race.getMinElevationM());
        assertEquals(1543, race.getMaxElevationM());
        assertEquals("trace.gpx", race.getGpxFileName());
        assertNotNull(race.getGpxImportedAt());
        verify(raceRepository).save(race);
        verify(pointRepository).deleteByRaceId(42L);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<RaceElevationProfilePoint>> pointsCaptor = ArgumentCaptor.forClass(List.class);
        verify(pointRepository).saveAll(pointsCaptor.capture());
        List<RaceElevationProfilePoint> points = pointsCaptor.getValue();
        assertEquals(3, points.size());
        assertEquals(race, points.get(0).getRace());
        assertEquals(0, points.get(0).getPointIndex());
        assertEquals(0.0, points.get(0).getDistanceKm());
        assertEquals(988, points.get(0).getElevationM());
        assertEquals(2, points.get(2).getPointIndex());
        assertEquals(12.34, points.get(2).getDistanceKm());
        assertEquals(1543, points.get(2).getElevationM());
    }

    @Test
    void uploadFailsWhenRaceDoesNotExist() {
        when(raceRepository.findById(42L)).thenReturn(Optional.empty());
        MockMultipartFile file = new MockMultipartFile("file", "trace.gpx", "application/gpx+xml", "<gpx></gpx>".getBytes(StandardCharsets.UTF_8));

        ApiException exception = assertThrows(ApiException.class, () -> service.upload(42L, file));

        assertEquals(404, exception.status);
        assertEquals("race_not_found", exception.getMessage());
        verifyNoInteractions(pointRepository, gpxParser, elevationProfileCalculator);
    }

    @Test
    void uploadFailsWhenFileIsEmpty() {
        when(raceRepository.findById(42L)).thenReturn(Optional.of(race()));
        MockMultipartFile file = new MockMultipartFile("file", "trace.gpx", "application/gpx+xml", new byte[0]);

        ApiException exception = assertThrows(ApiException.class, () -> service.upload(42L, file));

        assertEquals(400, exception.status);
        assertEquals("gpx_file_required", exception.getMessage());
        verifyNoInteractions(pointRepository, gpxParser, elevationProfileCalculator);
    }

    @Test
    void uploadFailsWhenFileExtensionIsInvalid() {
        when(raceRepository.findById(42L)).thenReturn(Optional.of(race()));
        MockMultipartFile file = new MockMultipartFile("file", "trace.txt", "text/plain", "not gpx".getBytes(StandardCharsets.UTF_8));

        ApiException exception = assertThrows(ApiException.class, () -> service.upload(42L, file));

        assertEquals(400, exception.status);
        assertEquals("invalid_gpx_file_extension", exception.getMessage());
        verifyNoInteractions(pointRepository, gpxParser, elevationProfileCalculator);
    }

    private Race race() {
        return Race.builder()
                .id(42L)
                .name("Trail")
                .location("Chamonix")
                .region("Auvergne-Rhône-Alpes")
                .date(LocalDate.now())
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
                .build();
    }
}
