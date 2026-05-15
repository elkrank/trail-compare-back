package com.trailmatch.controller.admin;

import com.trailmatch.dto.RaceGpxUploadResponse;
import com.trailmatch.dto.RaceRequest;
import com.trailmatch.dto.RaceResponse;
import com.trailmatch.exception.ApiException;
import com.trailmatch.service.RaceGpxService;
import com.trailmatch.service.RaceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import java.util.Map;

@RestController @RequestMapping("/api/admin/races") @RequiredArgsConstructor
public class AdminRaceController {
    private final RaceService service;
    private final RaceGpxService raceGpxService;
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE) public RaceResponse create(@RequestBody @Valid RaceRequest req){ return service.create(req); }

    /**
     * Creates a race from multipart/form-data.
     *
     * Contract: the JSON race payload must be sent as part {@code race}; the optional GPX file must be sent as
     * part {@code gpx}. The legacy {@code file} alias is intentionally rejected on this endpoint to keep the
     * create contract unambiguous. The standalone GPX upload endpoint still uses {@code file}.
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public RaceResponse createWithGpx(@RequestPart("race") @Valid RaceRequest req,
                                      @RequestPart(value = "gpx", required = false) MultipartFile gpx,
                                      MultipartHttpServletRequest request) {
        if (request.getMultiFileMap().containsKey("file")) {
            throw new ApiException(400, "invalid_multipart_file_part");
        }

        return service.createWithOptionalGpx(req, gpx);
    }
    @PutMapping("/{id}") public RaceResponse put(@PathVariable Long id, @RequestBody @Valid RaceRequest req){ return service.update(id, req); }
    @PatchMapping("/{id}") public RaceResponse patch(@PathVariable Long id, @RequestBody Map<String,Object> patch){ return service.patch(id, patch); }
    @DeleteMapping("/{id}") public void delete(@PathVariable Long id){ service.delete(id); }

    @PostMapping(path = "/{id}/gpx", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public RaceGpxUploadResponse uploadGpx(@PathVariable("id") Long raceId, @RequestParam(value = "file", required = false) MultipartFile file) {
        return raceGpxService.upload(raceId, file);
    }
}
