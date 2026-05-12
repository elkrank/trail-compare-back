package com.trailmatch.controller.admin;

import com.trailmatch.config.SecurityConfig;
import com.trailmatch.dto.RaceGpxUploadResponse;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminRaceController.class)
@Import({SecurityConfig.class, JwtAuthFilter.class, LoginRateLimitFilter.class})
class AdminRaceControllerIT {
    @Autowired MockMvc mockMvc;

    @MockBean RaceService raceService;
    @MockBean RaceGpxService raceGpxService;
    @MockBean JwtService jwtService;

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

    private MockMultipartFile gpxFile() {
        return new MockMultipartFile("file", "track.gpx", "application/gpx+xml", "<gpx/>".getBytes());
    }
}
