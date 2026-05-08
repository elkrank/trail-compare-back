package com.trailmatch.controller.admin;

import com.trailmatch.dto.RaceRequest;
import com.trailmatch.dto.RaceGpxUploadResponse;
import com.trailmatch.dto.RaceResponse;
import com.trailmatch.service.RaceGpxService;
import com.trailmatch.service.RaceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController @RequestMapping("/api/admin/races") @RequiredArgsConstructor
public class AdminRaceController {
    private final RaceService service;
    private final RaceGpxService raceGpxService;
    @PostMapping public RaceResponse create(@RequestBody @Valid RaceRequest req){ return service.create(req); }
    @PutMapping("/{id}") public RaceResponse put(@PathVariable Long id, @RequestBody @Valid RaceRequest req){ return service.update(id, req); }
    @PatchMapping("/{id}") public RaceResponse patch(@PathVariable Long id, @RequestBody Map<String,Object> patch){ return service.patch(id, patch); }
    @DeleteMapping("/{id}") public void delete(@PathVariable Long id){ service.delete(id); }
    @PostMapping("/{id}/gpx") public RaceGpxUploadResponse importGpx(@PathVariable Long id, @RequestParam("file") MultipartFile file){ return raceGpxService.importGpx(id, file); }
}
