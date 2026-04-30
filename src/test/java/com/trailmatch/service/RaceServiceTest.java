package com.trailmatch.service;

import com.trailmatch.dto.RaceRequest;
import com.trailmatch.entity.TechnicalityLevel;
import com.trailmatch.entity.TerrainType;
import com.trailmatch.mapper.RaceMapper;
import com.trailmatch.repository.RaceRepository;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.Mockito.*;

class RaceServiceTest {
    @Test
    void createCallsRepository() {
        RaceRepository repo = mock(RaceRepository.class);
        RaceService service = new RaceService(repo, new RaceMapper());
        var req = new RaceRequest("a","b","c", LocalDate.now(), 10.0, 1, TerrainType.MIXED, TechnicalityLevel.EASY, 120, 110, 90, 2, BigDecimal.ONE, "d", List.of("x"), 3.0);
        service.create(req);
        verify(repo, times(1)).save(Mockito.any());
    }
}
