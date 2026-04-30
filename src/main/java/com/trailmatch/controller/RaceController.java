package com.trailmatch.controller;

import com.trailmatch.dto.RaceResponse;
import com.trailmatch.entity.TerrainType;
import com.trailmatch.service.RaceService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController @RequestMapping("/api/races") @RequiredArgsConstructor
public class RaceController {
    private final RaceService service;
    @GetMapping
    public Page<RaceResponse> all(@RequestParam(required = false) String region, @RequestParam(required = false) TerrainType terrain,
                                  @RequestParam(required = false) Double minDistance, @RequestParam(required = false) Double maxDistance,
                                  @RequestParam(required = false) LocalDate minDate, @RequestParam(required = false) LocalDate maxDate,
                                  @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size,
                                  @RequestParam(defaultValue = "date") String sort){
        return service.findAll(region, terrain, minDistance, maxDistance, minDate, maxDate, page, size, sort);
    }
    @GetMapping("/{id}") public RaceResponse one(@PathVariable Long id){ return service.findById(id); }
}
