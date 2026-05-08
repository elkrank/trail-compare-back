package com.trailmatch.service;

import com.trailmatch.dto.RaceRequest;
import com.trailmatch.dto.RaceResponse;
import com.trailmatch.entity.Race;
import com.trailmatch.entity.TechnicalityLevel;
import com.trailmatch.entity.TerrainType;
import com.trailmatch.exception.ApiException;
import com.trailmatch.mapper.RaceMapper;
import com.trailmatch.repository.RaceRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

import java.util.*;

@Service @RequiredArgsConstructor
public class RaceService {
    private final RaceRepository repo; private final RaceMapper mapper;
    public Page<RaceResponse> findAll(String search, String region, Integer month, TerrainType terrainType, TechnicalityLevel technicalityLevel,
                                      Double minDistanceKm, Double maxDistanceKm, Integer minElevationGainM, Integer maxElevationGainM,
                                      int page, int size, String sort){
        String sortValue = (sort == null || sort.isBlank()) ? "date" : sort.trim();
        Sort.Direction direction = Sort.Direction.ASC;
        String property = sortValue;

        if (sortValue.startsWith("-")) {
            direction = Sort.Direction.DESC;
            property = sortValue.substring(1);
        } else if (sortValue.contains(",")) {
            String[] parts = sortValue.split(",", 2);
            property = parts[0].trim();
            if (parts.length > 1 && "desc".equalsIgnoreCase(parts[1].trim())) {
                direction = Sort.Direction.DESC;
            }
        }

        Sort s = Sort.by(direction, property);
        Pageable p = PageRequest.of(page, size, s);
        return repo.findAll((root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if(search != null) predicates.add(cb.like(cb.lower(root.get("name")), "%" + search.toLowerCase() + "%"));
            if(region != null) predicates.add(cb.equal(root.get("region"), region));
            if(month != null) predicates.add(cb.equal(cb.function("month", Integer.class, root.get("date")), month));
            if(terrainType != null) predicates.add(cb.equal(root.get("terrainType"), terrainType));
            if(technicalityLevel != null) predicates.add(cb.equal(root.get("technicalityLevel"), technicalityLevel));
            if(minDistanceKm != null) predicates.add(cb.ge(root.get("distanceKm"), minDistanceKm));
            if(maxDistanceKm != null) predicates.add(cb.le(root.get("distanceKm"), maxDistanceKm));
            if(minElevationGainM != null) predicates.add(cb.ge(root.get("elevationGainM"), minElevationGainM));
            if(maxElevationGainM != null) predicates.add(cb.le(root.get("elevationGainM"), maxElevationGainM));
            return cb.and(predicates.toArray(Predicate[]::new));
        }, p).map(mapper::toResponse);
    }
    public RaceResponse findById(Long id){ return mapper.toResponse(repo.findById(id).orElseThrow(() -> new ApiException(404, "race_not_found"))); }
    public void ensureExists(Long id){ repo.findById(id).orElseThrow(() -> new ApiException(404, "race_not_found")); }
    public RaceResponse create(RaceRequest req){ return mapper.toResponse(repo.save(mapper.toEntity(req))); }
    public RaceResponse update(Long id, RaceRequest req){ Race r = repo.findById(id).orElseThrow(() -> new ApiException(404, "race_not_found")); mapper.update(r, req); return mapper.toResponse(repo.save(r)); }
    public RaceResponse patch(Long id, Map<String, Object> patch){ Race r = repo.findById(id).orElseThrow(() -> new ApiException(404, "race_not_found")); if(patch.containsKey("priceEur")) r.setPriceEur(new java.math.BigDecimal(patch.get("priceEur").toString())); if(patch.containsKey("description")) r.setDescription(patch.get("description").toString()); return mapper.toResponse(repo.save(r)); }
    public void delete(Long id){ repo.deleteById(id); }
}
