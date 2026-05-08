CREATE TABLE race_elevation_profile_points (
    id BIGSERIAL PRIMARY KEY,
    race_id BIGINT NOT NULL REFERENCES races(id) ON DELETE CASCADE,
    point_index INTEGER NOT NULL CHECK (point_index >= 0),
    distance_km DOUBLE PRECISION NOT NULL CHECK (distance_km >= 0),
    elevation_m INTEGER NOT NULL,
    CONSTRAINT uk_race_elevation_profile_point_index UNIQUE (race_id, point_index)
);

CREATE INDEX idx_race_elevation_profile_points_race_id_point_index
    ON race_elevation_profile_points (race_id, point_index);
