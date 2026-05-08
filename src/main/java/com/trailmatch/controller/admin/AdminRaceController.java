package com.trailmatch.controller.admin;

import com.trailmatch.dto.RaceRequest;
import com.trailmatch.dto.RaceResponse;
import com.trailmatch.service.RaceGpxService;
import com.trailmatch.service.RaceService;
import com.trailmatch.service.gpx.ElevationProfile;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
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

    @PostMapping(path = "/{id}/gpx", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ElevationProfile uploadGpx(@PathVariable Long id, @RequestParam(value = "file", required = false) MultipartFile file) {
        return raceGpxService.upload(id, file);
    }
}
