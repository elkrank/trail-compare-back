package com.trailmatch.controller.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trailmatch.dto.RaceRequest;
import com.trailmatch.entity.Race;
import com.trailmatch.entity.RaceGpxFile;
import com.trailmatch.entity.TechnicalityLevel;
import com.trailmatch.entity.TerrainType;
import com.trailmatch.repository.RaceElevationProfilePointRepository;
import com.trailmatch.repository.RaceGpxFileRepository;
import com.trailmatch.repository.RaceRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class AdminRaceCreationIT {
    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void registerPostgresProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired RaceRepository raceRepository;
    @Autowired RaceElevationProfilePointRepository pointRepository;
    @Autowired RaceGpxFileRepository gpxFileRepository;

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminCanCreateRaceWithoutGradientColumnSqlError() throws Exception {
        RaceRequest request = new RaceRequest(
                "Trail API création",
                "Annecy",
                "Auvergne-Rhône-Alpes",
                LocalDate.of(2026, 9, 12),
                24.5,
                1350,
                TerrainType.MOUNTAIN,
                TechnicalityLevel.MODERATE,
                360,
                330,
                270,
                3,
                new BigDecimal("42.00"),
                "Course créée par test API admin sans colonne gradient stockée.",
                List.of("api", "admin"),
                "https://example.test/races/trail-api-creation"
        );

        mockMvc.perform(post("/api/admin/races")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.name").value("Trail API création"))
                .andExpect(jsonPath("$.hasGpx").value(false))
                .andExpect(jsonPath("$.gpxFileName").doesNotExist())
                .andExpect(jsonPath("$.gpxImportedAt").doesNotExist())
                .andExpect(jsonPath("$.elevationPerKm").value(closeTo(1350.0 / 24.5, 0.0001)));

        assertThat(raceRepository.findAll())
                .anySatisfy(race -> {
                    assertThat(race.getName()).isEqualTo("Trail API création");
                    assertThat(race.getDistanceKm()).isEqualTo(24.5);
                    assertThat(race.getElevationGainM()).isEqualTo(1350);
                });
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminCanCreateRaceWithGpxMultipartAndPersistImportedGpxData() throws Exception {
        String raceName = "Trail API création GPX multipart";
        byte[] gpxContent = validGpx().getBytes(StandardCharsets.UTF_8);

        mockMvc.perform(multipart("/api/admin/races")
                        .file(racePart(raceRequest(raceName)))
                        .file(new MockMultipartFile("gpx", "creation-track.gpx", "application/gpx+xml", gpxContent)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.name").value(raceName))
                .andExpect(jsonPath("$.hasGpx").value(true))
                .andExpect(jsonPath("$.gpxFileName").value("creation-track.gpx"))
                .andExpect(jsonPath("$.gpxImportedAt").exists())
                .andExpect(jsonPath("$.distanceKm").value(greaterThan(0.0)))
                .andExpect(jsonPath("$.elevationGainM").value(20));

        Race savedRace = raceByName(raceName).orElseThrow();
        assertThat(savedRace.getGpxFileName()).isEqualTo("creation-track.gpx");
        assertThat(savedRace.getGpxImportedAt()).isNotNull();
        assertThat(savedRace.getDistanceKm()).isPositive();
        assertThat(savedRace.getElevationGainM()).isEqualTo(20);
        assertThat(savedRace.getElevationLossM()).isZero();
        assertThat(savedRace.getMinElevationM()).isEqualTo(1000);
        assertThat(savedRace.getMaxElevationM()).isEqualTo(1020);
        assertThat(pointRepository.findByRaceIdOrderByPointIndexAsc(savedRace.getId()))
                .hasSize(2)
                .satisfies(points -> {
                    assertThat(points.get(0).getPointIndex()).isZero();
                    assertThat(points.get(0).getDistanceKm()).isZero();
                    assertThat(points.get(0).getElevationM()).isEqualTo(1000);
                    assertThat(points.get(1).getPointIndex()).isEqualTo(1);
                    assertThat(points.get(1).getDistanceKm()).isPositive();
                    assertThat(points.get(1).getElevationM()).isEqualTo(1020);
                });
        RaceGpxFile rawFile = gpxFileRepository.findById(savedRace.getId()).orElseThrow();
        assertThat(rawFile.getFileName()).isEqualTo("creation-track.gpx");
        assertThat(rawFile.getContentType()).isEqualTo("application/gpx+xml");
        assertThat(rawFile.getSizeBytes()).isEqualTo((long) gpxContent.length);
        assertThat(rawFile.getContent()).isEqualTo(gpxContent);
        assertThat(rawFile.getImportedAt()).isEqualTo(savedRace.getGpxImportedAt());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void invalidGpxDuringMultipartCreationReturnsErrorAndDoesNotPersistRace() throws Exception {
        String raceName = "Trail API création GPX invalide";

        mockMvc.perform(multipart("/api/admin/races")
                        .file(racePart(raceRequest(raceName)))
                        .file(new MockMultipartFile("gpx", "broken.gpx", "application/gpx+xml", "<gpx><trk>".getBytes(StandardCharsets.UTF_8))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("invalid_gpx"));

        assertThat(raceByName(raceName)).isEmpty();
    }


    private MockMultipartFile racePart(RaceRequest request) throws Exception {
        return new MockMultipartFile(
                "race",
                "race.json",
                MediaType.APPLICATION_JSON_VALUE,
                objectMapper.writeValueAsBytes(request));
    }

    private RaceRequest raceRequest(String name) {
        return new RaceRequest(
                name,
                "Annecy",
                "Auvergne-Rhône-Alpes",
                LocalDate.of(2026, 9, 12),
                24.5,
                1350,
                TerrainType.MOUNTAIN,
                TechnicalityLevel.MODERATE,
                360,
                330,
                270,
                3,
                new BigDecimal("42.00"),
                "Course créée par test API admin.",
                List.of("api", "admin"),
                "https://example.test/races/" + name.toLowerCase().replace(" ", "-")
        );
    }

    private Optional<Race> raceByName(String name) {
        return raceRepository.findAll().stream()
                .filter(race -> race.getName().equals(name))
                .findFirst();
    }

    private String validGpx() {
        return """
                <gpx version=\"1.1\" creator=\"TrailMatch test\">
                  <trk><name>Creation track</name><trkseg>
                    <trkpt lat=\"45.0000\" lon=\"6.0000\"><ele>1000</ele></trkpt>
                    <trkpt lat=\"45.0010\" lon=\"6.0010\"><ele>1020</ele></trkpt>
                  </trkseg></trk>
                </gpx>
                """;
    }
}
