package com.trailmatch.controller;

import com.trailmatch.dto.ComparisonDtos;
import com.trailmatch.service.ComparisonService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/comparisons") @RequiredArgsConstructor
public class ComparisonController {
    private final ComparisonService service;
    @PostMapping public ComparisonDtos.ComparisonResponse compare(@RequestBody @Valid ComparisonDtos.ComparisonRequest req){ return service.compare(req); }
}
