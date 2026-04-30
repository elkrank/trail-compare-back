package com.trailmatch.mapper;

import com.trailmatch.dto.RaceRequest;
import com.trailmatch.dto.RaceResponse;
import com.trailmatch.entity.Race;
import org.springframework.stereotype.Component;

@Component
public class RaceMapper {
    public Race toEntity(RaceRequest r) { return Race.builder().name(r.name()).location(r.location()).region(r.region()).date(r.date())
            .distanceKm(r.distanceKm()).elevationGainM(r.elevationGainM()).terrainType(r.terrainType()).technicalityLevel(r.technicalityLevel())
            .cutoffTimeMinutes(r.cutoffTimeMinutes()).lastFinisherTimeMinutes(r.lastFinisherTimeMinutes()).medianFinisherTimeMinutes(r.medianFinisherTimeMinutes())
            .aidStationsCount(r.aidStationsCount()).priceEur(r.priceEur()).description(r.description()).tags(r.tags()).gradient(r.gradient()).build(); }
    public void update(Race e, RaceRequest r){ e.setName(r.name());e.setLocation(r.location());e.setRegion(r.region());e.setDate(r.date());e.setDistanceKm(r.distanceKm());e.setElevationGainM(r.elevationGainM());e.setTerrainType(r.terrainType());e.setTechnicalityLevel(r.technicalityLevel());e.setCutoffTimeMinutes(r.cutoffTimeMinutes());e.setLastFinisherTimeMinutes(r.lastFinisherTimeMinutes());e.setMedianFinisherTimeMinutes(r.medianFinisherTimeMinutes());e.setAidStationsCount(r.aidStationsCount());e.setPriceEur(r.priceEur());e.setDescription(r.description());e.setTags(r.tags());e.setGradient(r.gradient()); }
    public RaceResponse toResponse(Race r){ return new RaceResponse(r.getId(), r.getName(), r.getLocation(), r.getRegion(), r.getDate(), r.getDistanceKm(), r.getElevationGainM(), r.getTerrainType(), r.getTechnicalityLevel(), r.getCutoffTimeMinutes(), r.getLastFinisherTimeMinutes(), r.getMedianFinisherTimeMinutes(), r.getAidStationsCount(), r.getPriceEur(), r.getDescription(), r.getTags(), r.getGradient()); }
}
