package com.trailmatch.controller.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trailmatch.dto.RaceRequest;
import com.trailmatch.entity.TechnicalityLevel;
import com.trailmatch.entity.TerrainType;
import com.trailmatch.repository.RaceRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.closeTo;
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
                .andExpect(jsonPath("$.elevationPerKm").value(closeTo(1350.0 / 24.5, 0.0001)));

        assertThat(raceRepository.findAll())
                .anySatisfy(race -> {
                    assertThat(race.getName()).isEqualTo("Trail API création");
                    assertThat(race.getDistanceKm()).isEqualTo(24.5);
                    assertThat(race.getElevationGainM()).isEqualTo(1350);
                });
    }
}
