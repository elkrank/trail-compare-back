package com.trailmatch.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "race_elevation_profile_points",
        uniqueConstraints = @UniqueConstraint(name = "uk_race_elevation_profile_point_index", columnNames = {"race_id", "point_index"}),
        indexes = @Index(name = "idx_race_elevation_profile_points_race_id_point_index", columnList = "race_id, point_index")
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RaceElevationProfilePoint {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "race_id", nullable = false)
    private Race race;

    @Column(name = "point_index", nullable = false)
    private Integer pointIndex;

    @Column(name = "distance_km", nullable = false)
    private Double distanceKm;

    @Column(name = "elevation_m", nullable = false)
    private Integer elevationM;
}
