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
import com.trailmatch.service.gpx.GpxPoint;
import com.trailmatch.service.gpx.GpxTrack;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
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
import static org.mockito.Mockito.verifyNoMoreInteractions;
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
        GpxTrack track = new GpxTrack(List.of(
                new GpxPoint(45.0, 6.0, 987.6, null),
                new GpxPoint(45.1, 6.1, 1543.2, null)
        ));
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
        when(raceRepository.save(race)).thenReturn(race);
        when(pointRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(gpxParser.parse(any())).thenReturn(track);
        when(elevationProfileCalculator.calculate(eq(track), eq(500))).thenReturn(profile);
        MockMultipartFile file = new MockMultipartFile("file", "trace.gpx", "application/gpx+xml", "<gpx></gpx>".getBytes(StandardCharsets.UTF_8));

        RaceGpxUploadResponse response = service.upload(42L, file);

        assertEquals(42L, response.raceId());
        assertEquals("trace.gpx", response.fileName());
        assertEquals(3, response.pointsCount());
        assertEquals(12.34, response.distanceKm());
        assertEquals(457, response.elevationGainM());
        assertEquals(123, response.elevationLossM());
        assertEquals(988, response.minElevationM());
        assertEquals(1543, response.maxElevationM());
        assertEquals(12.34, race.getDistanceKm());
        assertEquals(457, race.getElevationGainM());
        assertEquals(123, race.getElevationLossM());
        assertEquals(988, race.getMinElevationM());
        assertEquals(1543, race.getMaxElevationM());
        assertEquals("trace.gpx", race.getGpxFileName());
        assertNotNull(race.getGpxImportedAt());
        verify(elevationProfileCalculator).calculate(track, 500);
        verifyNoMoreInteractions(elevationProfileCalculator);
        verify(raceRepository).save(race);
        verify(pointRepository).deleteByRaceId(42L);
        verify(pointRepository).flush();

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
        assertEquals("gpx_file_empty", exception.getMessage());
        verifyNoInteractions(pointRepository, gpxParser, elevationProfileCalculator);
    }

    @Test
    void uploadFailsWhenFileIsNull() {
        when(raceRepository.findById(42L)).thenReturn(Optional.of(race()));

        ApiException exception = assertThrows(ApiException.class, () -> service.upload(42L, null));

        assertEquals(400, exception.status);
        assertEquals("gpx_file_required", exception.getMessage());
        verifyNoInteractions(pointRepository, gpxParser, elevationProfileCalculator);
    }

    @Test
    void uploadFailsWhenFileIsTooLarge() {
        when(raceRepository.findById(42L)).thenReturn(Optional.of(race()));
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getSize()).thenReturn(RaceGpxService.MAX_GPX_FILE_SIZE_BYTES + 1);

        ApiException exception = assertThrows(ApiException.class, () -> service.upload(42L, file));

        assertEquals(413, exception.status);
        assertEquals("gpx_file_too_large", exception.getMessage());
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

    @Test
    void uploadFailsWhenParsedTrackHasNoUsablePoint() {
        when(raceRepository.findById(42L)).thenReturn(Optional.of(race()));
        when(gpxParser.parse(any())).thenReturn(new GpxTrack(List.of()));
        MockMultipartFile file = new MockMultipartFile("file", "trace.gpx", "application/gpx+xml", "<gpx></gpx>".getBytes(StandardCharsets.UTF_8));

        ApiException exception = assertThrows(ApiException.class, () -> service.upload(42L, file));

        assertEquals(400, exception.status);
        assertEquals("gpx_no_usable_point", exception.getMessage());
        verify(gpxParser).parse(any());
        verifyNoInteractions(pointRepository, elevationProfileCalculator);
    }

    @Test
    void uploadFailsWhenParsedTrackHasNoElevationData() {
        when(raceRepository.findById(42L)).thenReturn(Optional.of(race()));
        when(gpxParser.parse(any())).thenReturn(new GpxTrack(List.of(
                new GpxPoint(45.0, 6.0, null, null),
                new GpxPoint(45.1, 6.1, null, null)
        )));
        MockMultipartFile file = new MockMultipartFile("file", "trace.gpx", "application/gpx+xml", "<gpx></gpx>".getBytes(StandardCharsets.UTF_8));

        ApiException exception = assertThrows(ApiException.class, () -> service.upload(42L, file));

        assertEquals(400, exception.status);
        assertEquals("gpx_no_elevation_data", exception.getMessage());
        verify(gpxParser).parse(any());
        verifyNoInteractions(pointRepository, elevationProfileCalculator);
    }

    @Test
    void uploadWrapsFileStreamAccessIOExceptionAsInvalidGpx() throws IOException {
        when(raceRepository.findById(42L)).thenReturn(Optional.of(race()));
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getSize()).thenReturn(123L);
        when(file.getOriginalFilename()).thenReturn("trace.gpx");
        when(file.getInputStream()).thenThrow(new IOException("stream failure"));

        ApiException exception = assertThrows(ApiException.class, () -> service.upload(42L, file));

        assertEquals(400, exception.status);
        assertEquals("invalid_gpx", exception.getMessage());
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
