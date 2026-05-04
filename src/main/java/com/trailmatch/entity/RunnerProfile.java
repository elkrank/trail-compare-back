package com.trailmatch.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "runner_profiles")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RunnerProfile {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "max_distance_km", nullable = false) private Double maxDistanceKm;
    @Column(name = "max_elevation_gain_m", nullable = false) private Integer maxElevationGainM;
    @Column(name = "weekly_volume_km", nullable = false) private Double weeklyVolumeKm;
    @Column(name = "weekly_training_hours", nullable = false) private Double weeklyTrainingHours;
    @Column(name = "longest_recent_run_km", nullable = false) private Double longestRecentRunKm;
    @Column(name = "current_fitness_level", nullable = false, length = 30) private String currentFitnessLevel;
    @Enumerated(EnumType.STRING) @Column(name = "usual_terrain", nullable = false) private TerrainType usualTerrain;
    @Column(nullable = false, length = 200) private String objective;
    @Column(name = "target_date") private LocalDate targetDate;
    @Column(name = "average_easy_pace_min_km") private Double averageEasyPaceMinKm;
    @Column(name = "average_trail_pace_min_km") private Double averageTrailPaceMinKm;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;

    @PrePersist void onCreate(){ createdAt = Instant.now(); updatedAt = createdAt; }
    @PreUpdate void onUpdate(){ updatedAt = Instant.now(); }
}
