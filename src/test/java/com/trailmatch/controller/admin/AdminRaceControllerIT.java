package com.trailmatch.controller.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trailmatch.config.SecurityConfig;
import com.trailmatch.dto.RaceGpxUploadResponse;
import com.trailmatch.dto.RaceRequest;
import com.trailmatch.dto.RaceResponse;
import com.trailmatch.entity.TechnicalityLevel;
import com.trailmatch.entity.TerrainType;
import com.trailmatch.exception.ApiException;
import com.trailmatch.security.JwtAuthFilter;
import com.trailmatch.security.JwtService;
import com.trailmatch.security.LoginRateLimitFilter;
import com.trailmatch.service.RaceGpxService;
import com.trailmatch.service.RaceService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminRaceController.class)
@Import({SecurityConfig.class, JwtAuthFilter.class, LoginRateLimitFilter.class})
class AdminRaceControllerIT {
    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockBean RaceService raceService;
    @MockBean RaceGpxService raceGpxService;
    @MockBean JwtService jwtService;

    @Test
    @WithMockUser(roles = "ADMIN")
    void createJsonRaceWithoutGpxKeepsExistingCompatibility() throws Exception {
        RaceResponse response = raceResponse(10L, false);
        when(raceService.create(any(RaceRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/admin/races")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(raceRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.name").value("Trail Test"))
                .andExpect(jsonPath("$.hasGpx").value(false))
                .andExpect(jsonPath("$.gpxFileName").doesNotExist())
                .andExpect(jsonPath("$.gpxImportedAt").doesNotExist());

        verify(raceService).create(any(RaceRequest.class));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createMultipartRaceWithGpxDelegatesToRaceService() throws Exception {
        RaceResponse response = raceResponse(1L, true);
        when(raceService.createWithOptionalGpx(any(RaceRequest.class), any())).thenReturn(response);

        mockMvc.perform(multipart("/api/admin/races")
                        .file(racePart())
                        .file(new MockMultipartFile("gpx", "track.gpx", "application/gpx+xml", "<gpx/>".getBytes())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Trail Test"))
                .andExpect(jsonPath("$.hasGpx").value(true))
                .andExpect(jsonPath("$.gpxFileName").value("track.gpx"));

        verify(raceService).createWithOptionalGpx(any(RaceRequest.class), any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createMultipartRaceAcceptsFileAlias() throws Exception {
        RaceResponse response = raceResponse(2L, true);
        when(raceService.createWithOptionalGpx(any(RaceRequest.class), any())).thenReturn(response);

        mockMvc.perform(multipart("/api/admin/races")
                        .file(racePart())
                        .file(new MockMultipartFile("file", "track.gpx", "application/gpx+xml", "<gpx/>".getBytes())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.hasGpx").value(true));

        verify(raceService).createWithOptionalGpx(any(RaceRequest.class), any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createMultipartRaceWithTooLargeGpxReturnsPayloadTooLarge() throws Exception {
        doThrow(new ApiException(413, "gpx_file_too_large"))
                .when(raceService).createWithOptionalGpx(any(RaceRequest.class), any());

        mockMvc.perform(multipart("/api/admin/races")
                        .file(racePart())
                        .file(new MockMultipartFile("gpx", "track.gpx", "application/gpx+xml", "too large".getBytes())))
                .andExpect(status().is(413))
                .andExpect(jsonPath("$.error").value("gpx_file_too_large"));

        verify(raceService).createWithOptionalGpx(any(RaceRequest.class), any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createMultipartRaceWithInvalidGpxExtensionReturnsBadRequest() throws Exception {
        doThrow(new ApiException(400, "invalid_gpx_file_extension"))
                .when(raceService).createWithOptionalGpx(any(RaceRequest.class), any());

        mockMvc.perform(multipart("/api/admin/races")
                        .file(racePart())
                        .file(new MockMultipartFile("gpx", "track.txt", "text/plain", "not gpx".getBytes())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("invalid_gpx_file_extension"));

        verify(raceService).createWithOptionalGpx(any(RaceRequest.class), any());
    }

    @Test
    @WithMockUser(roles = "USER")
    void createMultipartRaceWithoutAdminRoleIsRejected() throws Exception {
        mockMvc.perform(multipart("/api/admin/races")
                        .file(racePart())
                        .file(new MockMultipartFile("gpx", "track.gpx", "application/gpx+xml", "<gpx/>".getBytes())))
                .andExpect(status().isForbidden());

        verifyNoInteractions(raceService, raceGpxService);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void uploadGpxOnUnknownRaceReturnsRaceNotFound() throws Exception {
        doThrow(new ApiException(404, "race_not_found")).when(raceGpxService).upload(eq(404L), any());

        mockMvc.perform(multipart("/api/admin/races/{id}/gpx", 404L).file(gpxFile()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("race_not_found"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void uploadWithoutFileReturnsBadRequest() throws Exception {
        doThrow(new ApiException(400, "gpx_file_required")).when(raceGpxService).upload(eq(1L), isNull());

        mockMvc.perform(multipart("/api/admin/races/{id}/gpx", 1L))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("gpx_file_required"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void uploadNonGpxFileReturnsBadRequest() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "track.txt", "text/plain", "not a gpx".getBytes());
        doThrow(new ApiException(400, "invalid_gpx_file_extension")).when(raceGpxService).upload(eq(1L), any());

        mockMvc.perform(multipart("/api/admin/races/{id}/gpx", 1L).file(file))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("invalid_gpx_file_extension"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void uploadGpxWithoutAdminRoleIsRejected() throws Exception {
        mockMvc.perform(multipart("/api/admin/races/{id}/gpx", 1L).file(gpxFile()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void uploadGpxWithAdminRoleIsAccepted() throws Exception {
        RaceGpxUploadResponse response = new RaceGpxUploadResponse(1L, "track.gpx", 2, 12.5, 450, 120, 800, 1250);
        when(raceGpxService.upload(eq(1L), any())).thenReturn(response);

        mockMvc.perform(multipart("/api/admin/races/{id}/gpx", 1L).file(gpxFile()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.raceId").value(1))
                .andExpect(jsonPath("$.fileName").value("track.gpx"))
                .andExpect(jsonPath("$.pointsCount").value(2))
                .andExpect(jsonPath("$.distanceKm").value(12.5))
                .andExpect(jsonPath("$.elevationGainM").value(450))
                .andExpect(jsonPath("$.elevationLossM").value(120))
                .andExpect(jsonPath("$.minElevationM").value(800))
                .andExpect(jsonPath("$.maxElevationM").value(1250));

        verify(raceGpxService).upload(eq(1L), any());
    }


    @Test
    @WithMockUser(roles = "ADMIN")
    void uploadGpxAcceptsSameGpxFieldNameAsAdminCreation() throws Exception {
        RaceGpxUploadResponse response = new RaceGpxUploadResponse(1L, "track.gpx", 2, 12.5, 450, 120, 800, 1250);
        when(raceGpxService.upload(eq(1L), argThat(file -> file != null && "gpx".equals(file.getName()))))
                .thenReturn(response);

        mockMvc.perform(multipart("/api/admin/races/{id}/gpx", 1L)
                        .file(new MockMultipartFile("gpx", "track.gpx", "application/gpx+xml", "<gpx/>".getBytes())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.raceId").value(1))
                .andExpect(jsonPath("$.fileName").value("track.gpx"));

        verify(raceGpxService).upload(eq(1L), argThat(file -> file != null && "gpx".equals(file.getName())));
    }


    private MockMultipartFile racePart() throws Exception {
        return new MockMultipartFile(
                "race",
                "race.json",
                MediaType.APPLICATION_JSON_VALUE,
                objectMapper.writeValueAsBytes(raceRequest()));
    }

    private RaceRequest raceRequest() {
        return new RaceRequest(
                "Trail Test",
                "Annecy",
                "Auvergne-Rhône-Alpes",
                LocalDate.of(2026, 6, 1),
                12.5,
                450,
                TerrainType.MOUNTAIN,
                TechnicalityLevel.MODERATE,
                180,
                160,
                150,
                2,
                BigDecimal.TEN,
                "A scenic trail race",
                List.of("trail"),
                "https://example.com");
    }

    private RaceResponse raceResponse(Long id) {
        return raceResponse(id, false);
    }

    private RaceResponse raceResponse(Long id, boolean hasGpx) {
        RaceRequest req = raceRequest();
        return new RaceResponse(
                id,
                req.name(),
                req.location(),
                req.region(),
                req.date(),
                req.distanceKm(),
                req.elevationGainM(),
                req.terrainType(),
                req.technicalityLevel(),
                req.cutoffTimeMinutes(),
                req.lastFinisherTimeMinutes(),
                req.medianFinisherTimeMinutes(),
                req.aidStationsCount(),
                req.priceEur(),
                req.description(),
                req.tags(),
                req.sourceUrl(),
                hasGpx,
                hasGpx ? "track.gpx" : null,
                hasGpx ? Instant.parse("2026-05-14T12:00:00Z") : null,
                null,
                null,
                null,
                null,
                null,
                null,
                null);
    }

    private MockMultipartFile gpxFile() {
        return new MockMultipartFile("file", "track.gpx", "application/gpx+xml", "<gpx/>".getBytes());
    }
}
