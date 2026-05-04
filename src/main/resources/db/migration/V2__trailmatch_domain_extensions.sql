ALTER TABLE races ADD COLUMN IF NOT EXISTS source_url VARCHAR(1000);
ALTER TABLE races ADD COLUMN IF NOT EXISTS created_at TIMESTAMP NOT NULL DEFAULT NOW();
ALTER TABLE races ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP NOT NULL DEFAULT NOW();

CREATE TABLE IF NOT EXISTS runner_profiles (
    id BIGSERIAL PRIMARY KEY,
    max_distance_km DOUBLE PRECISION NOT NULL CHECK (max_distance_km >= 0),
    max_elevation_gain_m INTEGER NOT NULL CHECK (max_elevation_gain_m >= 0),
    weekly_volume_km DOUBLE PRECISION NOT NULL CHECK (weekly_volume_km >= 0),
    weekly_training_hours DOUBLE PRECISION NOT NULL CHECK (weekly_training_hours >= 0),
    longest_recent_run_km DOUBLE PRECISION NOT NULL CHECK (longest_recent_run_km >= 0),
    current_fitness_level VARCHAR(30) NOT NULL,
    usual_terrain VARCHAR(30) NOT NULL,
    objective VARCHAR(200) NOT NULL,
    target_date DATE,
    average_easy_pace_min_km DOUBLE PRECISION,
    average_trail_pace_min_km DOUBLE PRECISION,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);
