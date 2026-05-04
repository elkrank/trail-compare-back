package com.trailmatch.controller;

import com.trailmatch.dto.RaceResponse;
import com.trailmatch.entity.TechnicalityLevel;
import com.trailmatch.entity.TerrainType;
import com.trailmatch.service.RaceService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/races") @RequiredArgsConstructor
public class RaceController {
    private final RaceService service;
    @GetMapping
    public Page<RaceResponse> all(@RequestParam(required = false) String search, @RequestParam(required = false) String region,
                                  @RequestParam(required = false) Integer month, @RequestParam(required = false) TerrainType terrainType,
                                  @RequestParam(required = false) TechnicalityLevel technicalityLevel,
                                  @RequestParam(required = false) Double minDistanceKm, @RequestParam(required = false) Double maxDistanceKm,
                                  @RequestParam(required = false) Integer minElevationGainM, @RequestParam(required = false) Integer maxElevationGainM,
                                  @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size,
                                  @RequestParam(defaultValue = "date") String sort){
        return service.findAll(search, region, month, terrainType, technicalityLevel, minDistanceKm, maxDistanceKm, minElevationGainM, maxElevationGainM, page, size, sort);
    }
    @GetMapping("/{id}") public RaceResponse one(@PathVariable Long id){ return service.findById(id); }
}
