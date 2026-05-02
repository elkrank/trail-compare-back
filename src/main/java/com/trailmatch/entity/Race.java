package com.trailmatch.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Entity @Table(name = "races")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Race {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false) private String name;
    @Column(nullable = false) private String location;
    @Column(nullable = false) private String region;
    @Column(nullable = false) private LocalDate date;
    @Column(nullable = false) private Double distanceKm;
    @Column(name = "elevation_gain_m", nullable = false) private Integer elevationGainM;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private TerrainType terrainType;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private TechnicalityLevel technicalityLevel;
    @Column(nullable = false) private Integer cutoffTimeMinutes;
    @Column(nullable = false) private Integer lastFinisherTimeMinutes;
    @Column(nullable = false) private Integer medianFinisherTimeMinutes;
    @Column(nullable = false) private Integer aidStationsCount;
    @Column(nullable = false, precision = 10, scale = 2) private BigDecimal priceEur;
    @Column(nullable = false, length = 3000) private String description;
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "race_tags", joinColumns = @JoinColumn(name = "race_id"))
    @Column(name = "tag", nullable = false, length = 30)
    private List<String> tags;
    @Column(nullable = false) private Double gradient;
}
