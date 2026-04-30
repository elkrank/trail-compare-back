package com.trailmatch.service;

import com.trailmatch.dto.RaceRequest;
import com.trailmatch.dto.RaceResponse;
import com.trailmatch.entity.Race;
import com.trailmatch.entity.TerrainType;
import com.trailmatch.exception.ApiException;
import com.trailmatch.mapper.RaceMapper;
import com.trailmatch.repository.RaceRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;

@Service @RequiredArgsConstructor
public class RaceService {
    private final RaceRepository repo; private final RaceMapper mapper;
    public Page<RaceResponse> findAll(String region, TerrainType terrainType, Double minDistance, Double maxDistance, LocalDate minDate, LocalDate maxDate, int page, int size, String sort){
        Sort s = Sort.by(sort.startsWith("-") ? Sort.Direction.DESC : Sort.Direction.ASC, sort.replace("-", ""));
        Pageable p = PageRequest.of(page, size, s);
        return repo.findAll((root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if(region != null) predicates.add(cb.equal(root.get("region"), region));
            if(terrainType != null) predicates.add(cb.equal(root.get("terrainType"), terrainType));
            if(minDistance != null) predicates.add(cb.ge(root.get("distanceKm"), minDistance));
            if(maxDistance != null) predicates.add(cb.le(root.get("distanceKm"), maxDistance));
            if(minDate != null) predicates.add(cb.greaterThanOrEqualTo(root.get("date"), minDate));
            if(maxDate != null) predicates.add(cb.lessThanOrEqualTo(root.get("date"), maxDate));
            return cb.and(predicates.toArray(Predicate[]::new));
        }, p).map(mapper::toResponse);
    }
    public RaceResponse findById(Long id){ return mapper.toResponse(repo.findById(id).orElseThrow(() -> new ApiException(404, "race_not_found"))); }
    public RaceResponse create(RaceRequest req){ return mapper.toResponse(repo.save(mapper.toEntity(req))); }
    public RaceResponse update(Long id, RaceRequest req){ Race r = repo.findById(id).orElseThrow(() -> new ApiException(404, "race_not_found")); mapper.update(r, req); return mapper.toResponse(repo.save(r)); }
    public RaceResponse patch(Long id, Map<String, Object> patch){ Race r = repo.findById(id).orElseThrow(() -> new ApiException(404, "race_not_found")); if(patch.containsKey("priceEur")) r.setPriceEur(new java.math.BigDecimal(patch.get("priceEur").toString())); if(patch.containsKey("description")) r.setDescription(patch.get("description").toString()); return mapper.toResponse(repo.save(r)); }
    public void delete(Long id){ repo.deleteById(id); }
}
