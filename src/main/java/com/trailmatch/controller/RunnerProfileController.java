package com.trailmatch.controller;

import com.trailmatch.dto.RunnerProfileDtos;
import com.trailmatch.service.RunnerProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/runner-profile") @RequiredArgsConstructor
public class RunnerProfileController {
    private final RunnerProfileService service;
    @GetMapping public RunnerProfileDtos.RunnerProfileResponse get(){ return service.getDefault(); }
    @PostMapping public RunnerProfileDtos.RunnerProfileResponse createOrUpdate(@RequestBody @Valid RunnerProfileDtos.RunnerProfileRequest req){ return service.upsert(null, req); }
    @PutMapping("/{id}") public RunnerProfileDtos.RunnerProfileResponse update(@PathVariable Long id, @RequestBody @Valid RunnerProfileDtos.RunnerProfileRequest req){ return service.upsert(id, req); }
}
