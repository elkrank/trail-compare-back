package com.trailmatch.service;

import com.trailmatch.entity.Race;
import com.trailmatch.entity.TechnicalityLevel;
import com.trailmatch.entity.TerrainType;
import org.springframework.stereotype.Service;

@Service
public class DifficultyScoringService {
    public int score(Race r) {
        double distanceScore = Math.min(1, r.getDistanceKm() / 80d) * 30;
        double elevationScore = Math.min(1, r.getElevationGainM() / 5000d) * 25;
        double ratioScore = Math.min(1, (r.getElevationGainM() / r.getDistanceKm()) / 120d) * 15;
        double tech = switch (r.getTechnicalityLevel()) { case EASY -> 0.3; case MODERATE -> 0.6; case HARD -> 0.85; case EXTREME -> 1.0; };
        double technicalityScore = tech * 15;
        double terrainFactor = r.getTerrainType() == TerrainType.MOUNTAIN ? 1.0 : r.getTerrainType() == TerrainType.MIXED ? 0.7 : 0.4;
        double cutoffPressure = Math.max(0, 1 - ((r.getCutoffTimeMinutes() - r.getLastFinisherTimeMinutes()) / 120d));
        double cutoffScore = ((cutoffPressure * 0.8) + (terrainFactor * 0.2)) * 15;
        return (int)Math.round(Math.min(100, distanceScore + elevationScore + ratioScore + technicalityScore + cutoffScore));
    }
}
