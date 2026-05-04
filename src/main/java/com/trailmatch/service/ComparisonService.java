package com.trailmatch.service;

import com.trailmatch.dto.ComparisonDtos;
import com.trailmatch.dto.RunnerProfileDtos;
import com.trailmatch.entity.Race;
import com.trailmatch.entity.RiskLevel;
import com.trailmatch.repository.RaceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

@Service @RequiredArgsConstructor
public class ComparisonService {
    private final RaceRepository raceRepository;
    private final DifficultyScoringService difficulty;
    private final CompatibilityScoringService compatibility;
    private final RaceMetricsService metrics;

    public ComparisonDtos.ComparisonResponse compare(ComparisonDtos.ComparisonRequest request){
        List<Race> races = raceRepository.findAllById(request.raceIds());
        List<ComparisonDtos.ComparisonResultDto> results = new ArrayList<>();
        for (Race r : races) {
            int comp = compatibility.score(r, request.runnerProfile());
            results.add(new ComparisonDtos.ComparisonResultDto(r.getId(), difficulty.score(r), comp, metrics.cutoffPace(r), metrics.lastFinisherPace(r), metrics.medianPace(r), risk(comp), label(comp), List.of(), List.of()));
        }
        Long best = results.stream().max(Comparator.comparingInt(ComparisonDtos.ComparisonResultDto::compatibilityScore)).map(ComparisonDtos.ComparisonResultDto::raceId).orElse(null);
        Long easy = results.stream().min(Comparator.comparingInt(ComparisonDtos.ComparisonResultDto::difficultyScore)).map(ComparisonDtos.ComparisonResultDto::raceId).orElse(null);
        Long risk = results.stream().min(Comparator.comparingInt(ComparisonDtos.ComparisonResultDto::compatibilityScore)).map(ComparisonDtos.ComparisonResultDto::raceId).orElse(null);
        return new ComparisonDtos.ComparisonResponse(results, best, easy, risk, "Comparaison calculée sur base déterministe.");
    }
    private RiskLevel risk(int c){ if(c<=40) return RiskLevel.HIGH; if(c<=60) return RiskLevel.MODERATE; if(c<=75) return RiskLevel.VIGILANT; if(c<=90) return RiskLevel.GOOD; return RiskLevel.EXCELLENT; }
    private String label(int c){ if(c<=40) return "Peu compatible / risque élevé"; if(c<=60) return "Possible mais risqué"; if(c<=75) return "Compatible avec vigilance"; if(c<=90) return "Bon match"; return "Très bon match"; }
}
