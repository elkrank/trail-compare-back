package com.trailmatch.service;

import com.trailmatch.entity.Race;
import org.springframework.stereotype.Service;

@Service
public class RaceMetricsService {
    public double elevationPerKm(Race r){ return r.getElevationGainM() / r.getDistanceKm(); }
    public double cutoffPace(Race r){ return r.getCutoffTimeMinutes() / r.getDistanceKm(); }
    public double lastFinisherPace(Race r){ return r.getLastFinisherTimeMinutes() / r.getDistanceKm(); }
    public double medianPace(Race r){ return r.getMedianFinisherTimeMinutes() / r.getDistanceKm(); }
}
