package com.trailmatch.controller.admin;

import com.trailmatch.config.SecurityConfig;
import com.trailmatch.exception.ApiException;
import com.trailmatch.security.JwtAuthFilter;
import com.trailmatch.security.JwtService;
import com.trailmatch.security.LoginRateLimitFilter;
import com.trailmatch.service.RaceService;
import com.trailmatch.service.gpx.ElevationProfile;
import com.trailmatch.service.gpx.ElevationProfileCalculator;
import com.trailmatch.service.gpx.GpxParser;
import com.trailmatch.service.gpx.GpxTrack;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
    @MockBean GpxParser gpxParser;
    @MockBean ElevationProfileCalculator elevationProfileCalculator;
    @MockBean JwtService jwtService;

    @Test
    @WithMockUser(roles = "ADMIN")
    void uploadGpxOnUnknownRaceReturnsRaceNotFound() throws Exception {
        doThrow(new ApiException(404, "race_not_found")).when(raceService).ensureExists(404L);

        mockMvc.perform(multipart("/api/admin/races/{id}/gpx", 404L).file(gpxFile()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("race_not_found"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void uploadWithoutFileReturnsBadRequest() throws Exception {
        mockMvc.perform(multipart("/api/admin/races/{id}/gpx", 1L))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("gpx_file_required"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void uploadNonGpxFileReturnsBadRequest() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "track.txt", "text/plain", "not a gpx".getBytes());

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
        GpxTrack track = new GpxTrack(List.of());
        ElevationProfile profile = new ElevationProfile(0.0, 0.0, 0.0, null, null, List.of());
        when(gpxParser.parse(any())).thenReturn(track);
        when(elevationProfileCalculator.calculate(eq(track))).thenReturn(profile);

        mockMvc.perform(multipart("/api/admin/races/{id}/gpx", 1L).file(gpxFile()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.distanceKm").value(0.0));

        verify(raceService).ensureExists(1L);
    }

    private MockMultipartFile gpxFile() {
        return new MockMultipartFile("file", "track.gpx", "application/gpx+xml", "<gpx/>".getBytes());
    }
}
