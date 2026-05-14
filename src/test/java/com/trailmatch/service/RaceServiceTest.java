package com.trailmatch.service;

import com.trailmatch.dto.RaceRequest;
import com.trailmatch.dto.RaceResponse;
import com.trailmatch.entity.Race;
import com.trailmatch.entity.TechnicalityLevel;
import com.trailmatch.entity.TerrainType;
import com.trailmatch.exception.ApiException;
import com.trailmatch.mapper.RaceMapper;
import com.trailmatch.repository.RaceRepository;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

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

    @Test
    void createWithOptionalGpxIsTransactional() throws NoSuchMethodException {
        Method method = RaceService.class.getMethod("createWithOptionalGpx", RaceRequest.class, org.springframework.web.multipart.MultipartFile.class);

        assertTrue(method.isAnnotationPresent(Transactional.class));
    }

    @Test
    void createWithOptionalGpxCreatesRaceWithoutImportWhenNoGpxIsProvided() {
        RaceRepository repository = mock(RaceRepository.class);
        RaceMapper mapper = mock(RaceMapper.class);
        RaceGpxService raceGpxService = mock(RaceGpxService.class);
        RaceService service = new RaceService(repository, mapper, raceGpxService);
        RaceRequest request = request();
        Race raceToSave = race(20, 700, 300, 280, TechnicalityLevel.MODERATE);
        Race savedRace = race(20, 700, 300, 280, TechnicalityLevel.MODERATE);
        RaceResponse expectedResponse = response(savedRace);
        when(mapper.toEntity(request)).thenReturn(raceToSave);
        when(repository.save(raceToSave)).thenReturn(savedRace);
        when(mapper.toResponse(savedRace)).thenReturn(expectedResponse);

        RaceResponse response = service.createWithOptionalGpx(request, null);

        assertSame(expectedResponse, response);
        verify(repository).save(raceToSave);
        verify(mapper).toResponse(savedRace);
        verifyNoInteractions(raceGpxService);
    }

    @Test
    void createWithOptionalGpxImportsProvidedGpxForSavedRace() {
        RaceRepository repository = mock(RaceRepository.class);
        RaceMapper mapper = mock(RaceMapper.class);
        RaceGpxService raceGpxService = mock(RaceGpxService.class);
        RaceService service = new RaceService(repository, mapper, raceGpxService);
        RaceRequest request = request();
        Race raceToSave = race(20, 700, 300, 280, TechnicalityLevel.MODERATE);
        Race savedRace = race(20, 700, 300, 280, TechnicalityLevel.MODERATE);
        RaceResponse expectedResponse = response(savedRace);
        MockMultipartFile file = new MockMultipartFile("gpx", "trace.gpx", "application/gpx+xml", "<gpx></gpx>".getBytes(StandardCharsets.UTF_8));
        when(mapper.toEntity(request)).thenReturn(raceToSave);
        when(repository.save(raceToSave)).thenReturn(savedRace);
        when(mapper.toResponse(savedRace)).thenReturn(expectedResponse);

        RaceResponse response = service.createWithOptionalGpx(request, file);

        assertSame(expectedResponse, response);
        verify(repository).save(raceToSave);
        verify(raceGpxService).importForRace(savedRace, file);
        verify(mapper).toResponse(savedRace);
    }

    @Test
    void createWithOptionalGpxPropagatesGpxValidationException() {
        RaceRepository repository = mock(RaceRepository.class);
        RaceMapper mapper = mock(RaceMapper.class);
        RaceGpxService raceGpxService = mock(RaceGpxService.class);
        RaceService service = new RaceService(repository, mapper, raceGpxService);
        RaceRequest request = request();
        Race raceToSave = race(20, 700, 300, 280, TechnicalityLevel.MODERATE);
        Race savedRace = race(20, 700, 300, 280, TechnicalityLevel.MODERATE);
        MockMultipartFile file = new MockMultipartFile("gpx", "trace.txt", "text/plain", "not gpx".getBytes(StandardCharsets.UTF_8));
        ApiException expectedException = new ApiException(400, "invalid_gpx_file_extension");
        when(mapper.toEntity(request)).thenReturn(raceToSave);
        when(repository.save(raceToSave)).thenReturn(savedRace);
        doThrow(expectedException).when(raceGpxService).importForRace(savedRace, file);

        ApiException exception = assertThrows(ApiException.class, () -> service.createWithOptionalGpx(request, file));

        assertSame(expectedException, exception);
        verify(repository).save(raceToSave);
        verify(raceGpxService).importForRace(savedRace, file);
    }

    private RaceRequest request() {
        return new RaceRequest(
                "Trail",
                "Chamonix",
                "Auvergne-Rhône-Alpes",
                LocalDate.now(),
                20.0,
                700,
                TerrainType.MOUNTAIN,
                TechnicalityLevel.MODERATE,
                300,
                280,
                240,
                2,
                BigDecimal.TEN,
                "Profiled race",
                List.of("trail"),
                "https://example.test/race");
    }

    private RaceResponse response(Race race) {
        return new RaceResponse(
                race.getId(),
                race.getName(),
                race.getLocation(),
                race.getRegion(),
                race.getDate(),
                race.getDistanceKm(),
                race.getElevationGainM(),
                race.getTerrainType(),
                race.getTechnicalityLevel(),
                race.getCutoffTimeMinutes(),
                race.getLastFinisherTimeMinutes(),
                race.getMedianFinisherTimeMinutes(),
                race.getAidStationsCount(),
                race.getPriceEur(),
                race.getDescription(),
                race.getTags(),
                race.getSourceUrl(),
                race.getGpxImportedAt() != null,
                race.getGpxFileName(),
                race.getGpxImportedAt(),
                race.getCreatedAt(),
                race.getUpdatedAt(),
                35.0,
                15.0,
                14.0,
                12.0,
                55);
    }
}
