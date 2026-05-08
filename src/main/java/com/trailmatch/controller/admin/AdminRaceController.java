package com.trailmatch.controller.admin;

import com.trailmatch.dto.RaceRequest;
import com.trailmatch.dto.RaceResponse;
import com.trailmatch.exception.ApiException;
import com.trailmatch.service.RaceService;
import com.trailmatch.service.gpx.ElevationProfile;
import com.trailmatch.service.gpx.ElevationProfileCalculator;
import com.trailmatch.service.gpx.GpxParser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@RestController @RequestMapping("/api/admin/races") @RequiredArgsConstructor
public class AdminRaceController {
    private final RaceService service;
    private final GpxParser gpxParser;
    private final ElevationProfileCalculator elevationProfileCalculator;
    @PostMapping public RaceResponse create(@RequestBody @Valid RaceRequest req){ return service.create(req); }
    @PutMapping("/{id}") public RaceResponse put(@PathVariable Long id, @RequestBody @Valid RaceRequest req){ return service.update(id, req); }
    @PatchMapping("/{id}") public RaceResponse patch(@PathVariable Long id, @RequestBody Map<String,Object> patch){ return service.patch(id, patch); }
    @DeleteMapping("/{id}") public void delete(@PathVariable Long id){ service.delete(id); }

    @PostMapping(path = "/{id}/gpx", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ElevationProfile uploadGpx(@PathVariable Long id, @RequestParam(value = "file", required = false) MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new ApiException(400, "gpx_file_required");
        }
        String filename = file.getOriginalFilename();
        if (filename == null || !filename.toLowerCase().endsWith(".gpx")) {
            throw new ApiException(400, "invalid_gpx_file_extension");
        }
        service.ensureExists(id);
        return elevationProfileCalculator.calculate(gpxParser.parse(file.getInputStream()));
    }
}
