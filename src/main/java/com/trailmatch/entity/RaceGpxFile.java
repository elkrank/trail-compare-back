package com.trailmatch.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "race_gpx_files")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RaceGpxFile {
    @Id
    @Column(name = "race_id")
    private Long raceId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId
    @JoinColumn(name = "race_id", nullable = false)
    private Race race;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "content_type", length = 100)
    private String contentType;

    @Column(name = "size_bytes", nullable = false)
    private Long sizeBytes;

    @Column(length = 64)
    private String sha256;

    @Column(nullable = false, columnDefinition = "BYTEA")
    private byte[] content;

    @Column(name = "imported_at", nullable = false)
    private Instant importedAt;
}
