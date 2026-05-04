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
    @Column(nullable = false) private Double maxDistanceKm;
    @Column(nullable = false) private Integer maxElevationGainM;
    @Column(nullable = false) private Double weeklyVolumeKm;
    @Column(nullable = false) private Double weeklyTrainingHours;
    @Column(nullable = false) private Double longestRecentRunKm;
    @Column(nullable = false, length = 30) private String currentFitnessLevel;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private TerrainType usualTerrain;
    @Column(nullable = false, length = 200) private String objective;
    private LocalDate targetDate;
    private Double averageEasyPaceMinKm;
    private Double averageTrailPaceMinKm;
    @Column(nullable = false) private Instant createdAt;
    @Column(nullable = false) private Instant updatedAt;

    @PrePersist void onCreate(){ createdAt = Instant.now(); updatedAt = createdAt; }
    @PreUpdate void onUpdate(){ updatedAt = Instant.now(); }
}
