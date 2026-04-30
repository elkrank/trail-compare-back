CREATE TABLE admin_users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(100) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL
);
CREATE TABLE races (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    location VARCHAR(255) NOT NULL,
    region VARCHAR(255) NOT NULL,
    date DATE NOT NULL,
    distance_km DOUBLE PRECISION NOT NULL CHECK (distance_km > 0),
    elevation_gain_m INTEGER NOT NULL CHECK (elevation_gain_m >= 0),
    terrain_type VARCHAR(30) NOT NULL,
    technicality_level VARCHAR(30) NOT NULL,
    cutoff_time_minutes INTEGER NOT NULL CHECK (cutoff_time_minutes > 0),
    last_finisher_time_minutes INTEGER NOT NULL CHECK (last_finisher_time_minutes > 0),
    median_finisher_time_minutes INTEGER NOT NULL CHECK (median_finisher_time_minutes > 0),
    aid_stations_count INTEGER NOT NULL CHECK (aid_stations_count >= 0),
    price_eur NUMERIC(10,2) NOT NULL CHECK (price_eur >= 0),
    description VARCHAR(3000) NOT NULL,
    gradient DOUBLE PRECISION NOT NULL
);
CREATE TABLE race_tags (race_id BIGINT NOT NULL REFERENCES races(id) ON DELETE CASCADE, tag VARCHAR(30) NOT NULL);
INSERT INTO races(name,location,region,date,distance_km,elevation_gain_m,terrain_type,technicality_level,cutoff_time_minutes,last_finisher_time_minutes,median_finisher_time_minutes,aid_stations_count,price_eur,description,gradient)
VALUES ('Trail des Cimes','Chamonix','Auvergne-Rhône-Alpes','2026-08-21',45,2500,'MOUNTAIN','HARD',600,560,430,4,85.00,'Course alpine exigeante',5.5);
INSERT INTO race_tags(race_id,tag) VALUES (1,'alpin'),(1,'technique');
