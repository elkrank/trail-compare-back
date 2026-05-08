package com.trailmatch.service;

import com.trailmatch.exception.ApiException;
import com.trailmatch.repository.RaceRepository;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RaceGpxServiceTest {
    @Test
    void uploadReturnsConfirmationForExistingRace() {
        RaceRepository repository = mock(RaceRepository.class);
        when(repository.existsById(42L)).thenReturn(true);
        RaceGpxService service = new RaceGpxService(repository);
        MockMultipartFile file = new MockMultipartFile("file", "trace.gpx", "application/gpx+xml", "<gpx></gpx>".getBytes(StandardCharsets.UTF_8));

        var response = service.upload(42L, file);

        assertEquals(42L, response.raceId());
        assertEquals("trace.gpx", response.filename());
        assertEquals("application/gpx+xml", response.contentType());
        assertEquals(11L, response.sizeBytes());
        assertEquals("uploaded", response.status());
    }

    @Test
    void uploadFailsWhenRaceDoesNotExist() {
        RaceRepository repository = mock(RaceRepository.class);
        when(repository.existsById(42L)).thenReturn(false);
        RaceGpxService service = new RaceGpxService(repository);
        MockMultipartFile file = new MockMultipartFile("file", "trace.gpx", "application/gpx+xml", "<gpx></gpx>".getBytes(StandardCharsets.UTF_8));

        ApiException exception = assertThrows(ApiException.class, () -> service.upload(42L, file));

        assertEquals(404, exception.status);
        assertEquals("race_not_found", exception.getMessage());
    }

    @Test
    void uploadFailsWhenFileIsEmpty() {
        RaceRepository repository = mock(RaceRepository.class);
        when(repository.existsById(42L)).thenReturn(true);
        RaceGpxService service = new RaceGpxService(repository);
        MockMultipartFile file = new MockMultipartFile("file", "trace.gpx", "application/gpx+xml", new byte[0]);

        ApiException exception = assertThrows(ApiException.class, () -> service.upload(42L, file));

        assertEquals(400, exception.status);
        assertEquals("gpx_file_empty", exception.getMessage());
    }
}
