package com.trailmatch.service;

import com.trailmatch.dto.RunnerProfileDtos;
import com.trailmatch.entity.Race;
import org.springframework.stereotype.Service;

@Service
public class CompatibilityScoringService {
    public int score(Race race, RunnerProfileDtos.RunnerProfileRequest p){
        double distance = Math.min(1, p.maxDistanceKm() / race.getDistanceKm()) * 30;
        double elevation = Math.min(1, p.maxElevationGainM() / (double) race.getElevationGainM()) * 25;
        double volume = Math.min(1, p.weeklyVolumeKm() / Math.max(20, race.getDistanceKm())) * 15;
        double longest = Math.min(1, p.longestRecentRunKm() / race.getDistanceKm()) * 15;
        double terrain = p.usualTerrain() == race.getTerrainType() ? 10 : 5;
        double time = p.weeklyTrainingHours() >= 4 ? 5 : 2;
        return (int)Math.round(Math.max(0, Math.min(100, distance+elevation+volume+longest+terrain+time)));
    }
}
