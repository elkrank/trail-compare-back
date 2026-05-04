package com.trailmatch.service;

import com.trailmatch.dto.RunnerProfileDtos;
import com.trailmatch.entity.RunnerProfile;
import com.trailmatch.repository.RunnerProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service @RequiredArgsConstructor
public class RunnerProfileService {
    private final RunnerProfileRepository repository;

    public RunnerProfileDtos.RunnerProfileResponse upsert(Long id, RunnerProfileDtos.RunnerProfileRequest req){
        RunnerProfile profile = id == null ? new RunnerProfile() : repository.findById(id).orElse(new RunnerProfile());
        profile.setMaxDistanceKm(req.maxDistanceKm()); profile.setMaxElevationGainM(req.maxElevationGainM());
        profile.setWeeklyVolumeKm(req.weeklyVolumeKm()); profile.setWeeklyTrainingHours(req.weeklyTrainingHours());
        profile.setLongestRecentRunKm(req.longestRecentRunKm()); profile.setCurrentFitnessLevel(req.currentFitnessLevel());
        profile.setUsualTerrain(req.usualTerrain()); profile.setObjective(req.objective()); profile.setTargetDate(req.targetDate());
        profile.setAverageEasyPaceMinKm(req.averageEasyPaceMinKm()); profile.setAverageTrailPaceMinKm(req.averageTrailPaceMinKm());
        RunnerProfile saved = repository.save(profile);
        return new RunnerProfileDtos.RunnerProfileResponse(saved.getId(), saved.getMaxDistanceKm(), saved.getMaxElevationGainM(), saved.getWeeklyVolumeKm(), saved.getWeeklyTrainingHours(), saved.getLongestRecentRunKm(), saved.getCurrentFitnessLevel(), saved.getUsualTerrain(), saved.getObjective(), saved.getTargetDate(), saved.getAverageEasyPaceMinKm(), saved.getAverageTrailPaceMinKm(), saved.getCreatedAt(), saved.getUpdatedAt());
    }
    public RunnerProfileDtos.RunnerProfileResponse getDefault(){ return repository.findAll().stream().findFirst().map(p -> new RunnerProfileDtos.RunnerProfileResponse(p.getId(), p.getMaxDistanceKm(), p.getMaxElevationGainM(), p.getWeeklyVolumeKm(), p.getWeeklyTrainingHours(), p.getLongestRecentRunKm(), p.getCurrentFitnessLevel(), p.getUsualTerrain(), p.getObjective(), p.getTargetDate(), p.getAverageEasyPaceMinKm(), p.getAverageTrailPaceMinKm(), p.getCreatedAt(), p.getUpdatedAt())).orElse(null); }
}
