package com.trailmatch.service;

import com.trailmatch.dto.RaceGpxUploadResponse;
import com.trailmatch.exception.ApiException;
import com.trailmatch.repository.RaceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class RaceGpxService {
    private final RaceRepository raceRepository;

    public RaceGpxUploadResponse upload(Long raceId, MultipartFile file) {
        if (!raceRepository.existsById(raceId)) {
            throw new ApiException(404, "race_not_found");
        }

        if (file == null || file.isEmpty()) {
            throw new ApiException(400, "gpx_file_empty");
        }

        return new RaceGpxUploadResponse(
                raceId,
                file.getOriginalFilename(),
                file.getContentType(),
                file.getSize(),
                "uploaded"
        );
    }
}
