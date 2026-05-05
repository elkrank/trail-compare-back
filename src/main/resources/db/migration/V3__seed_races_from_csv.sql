-- Seed races imported from provided CSV dataset.
-- Notes:
-- - The source CSV does not include all required columns in table `races`.
-- - Missing fields are filled with deterministic defaults that can be adjusted later.

INSERT INTO races (
    name,
    location,
    region,
    date,
    distance_km,
    elevation_gain_m,
    terrain_type,
    technicality_level,
    cutoff_time_minutes,
    last_finisher_time_minutes,
    median_finisher_time_minutes,
    aid_stations_count,
    price_eur,
    description,
    gradient,
    source_url
)
SELECT
    v.name,
    v.location,
    v.region,
    DATE '2026-01-01' AS date,
    v.distance_km,
    v.elevation_gain_m,
    'MOUNTAIN'::VARCHAR(30) AS terrain_type,
    CASE
        WHEN v.distance_km >= 120 OR v.elevation_gain_m >= 9000 THEN 'EXTREME'
        WHEN v.distance_km >= 70 OR v.elevation_gain_m >= 3500 THEN 'HARD'
        WHEN v.distance_km >= 30 OR v.elevation_gain_m >= 1200 THEN 'MODERATE'
        ELSE 'EASY'
    END AS technicality_level,
    GREATEST(120, ROUND(v.distance_km * 15 + v.elevation_gain_m * 0.05))::INTEGER AS cutoff_time_minutes,
    GREATEST(110, ROUND(v.distance_km * 14 + v.elevation_gain_m * 0.045))::INTEGER AS last_finisher_time_minutes,
    GREATEST(90, ROUND(v.distance_km * 10 + v.elevation_gain_m * 0.03))::INTEGER AS median_finisher_time_minutes,
    GREATEST(1, CEIL(v.distance_km / 20.0))::INTEGER AS aid_stations_count,
    0.00::NUMERIC(10,2) AS price_eur,
    'Import CSV: ' || v.name || ' (' || v.region || ')' AS description,
    ROUND((v.elevation_gain_m::NUMERIC / NULLIF(v.distance_km, 0)), 2)::DOUBLE PRECISION AS gradient,
    NULL::VARCHAR(1000) AS source_url
FROM (
    VALUES
    ('UTMB',174.0,9900,'Chamonix','Auvergne-Rhône-Alpes'),
    ('CCC (UTMB)',101.0,6050,'Courmayeur','Vallée d''Aoste, Italie'),
    ('OCC (UTMB)',57.0,3500,'Orsières','Valais, Suisse'),
    ('TDS (UTMB)',145.0,9500,'Courmayeur','Vallée d''Aoste, Italie'),
    ('MCC (UTMB)',40.0,2350,'Martigny-Combe','Valais, Suisse'),
    ('ETC (UTMB)',15.0,1200,'Chamonix','Auvergne-Rhône-Alpes'),
    ('PTL (UTMB)',300.0,25000,'Chamonix','Auvergne-Rhône-Alpes'),
    ('UT4M 180 Xtrem',180.0,11500,'Seyssins','Auvergne-Rhône-Alpes'),
    ('UT4M 160 Challenge',180.0,12190,'Seyssins','Auvergne-Rhône-Alpes'),
    ('UT4M 100 Master',97.0,5860,'Parc d''Uriage','Auvergne-Rhône-Alpes'),
    ('UT4M 80 Challenge',78.0,6395,'Seyssins','Auvergne-Rhône-Alpes'),
    ('UT4M 40 Taillefer',49.8,3500,'Vif','Auvergne-Rhône-Alpes'),
    ('UT4M 40 Belledonne',48.0,2900,'Rioupéroux','Auvergne-Rhône-Alpes'),
    ('UT4M 40 Chartreuse',41.0,2800,'Saint-Nazaire-les-Eymes','Auvergne-Rhône-Alpes'),
    ('UT4M 40 Vercors',40.0,2700,'Seyssins','Auvergne-Rhône-Alpes'),
    ('UT4M 20 Taillefer',22.0,1800,'La Morte','Auvergne-Rhône-Alpes'),
    ('UT4M 20 Belledonne',20.7,1920,'Uriage','Auvergne-Rhône-Alpes'),
    ('UT4M 20 Vercors',20.0,1800,'Lans-en-Vercors','Auvergne-Rhône-Alpes'),
    ('UT4M 20 Chartreuse',16.0,730,'Le Sappey-en-Chartreuse','Auvergne-Rhône-Alpes'),
    ('Endurance Trail (Templiers)',104.0,4309,'Millau','Occitanie'),
    ('Rock''Voizine',48.5,2220,'Millau','Occitanie'),
    ('Intégrale des Causses',66.0,2695,'Millau','Occitanie'),
    ('Marathon du Larzac',35.5,1420,'Millau','Occitanie'),
    ('La Templière',7.3,290,'Millau','Occitanie'),
    ('Les Troubadours',11.7,512,'Millau','Occitanie'),
    ('Boffi Fifty',47.0,2220,'Millau','Occitanie'),
    ('Dourbie Fourmi',25.0,1230,'Millau','Occitanie'),
    ('La Mona Lisa',30.0,1400,'Millau','Occitanie'),
    ('Marathon des Causses',35.0,1585,'Millau','Occitanie'),
    ('KD',8.0,290,'Millau','Occitanie'),
    ('VO2 Trail',17.2,700,'Millau','Occitanie'),
    ('Grand Trail des Templiers',80.6,3440,'Millau','Occitanie'),
    ('Trail des Citadelles Ultra',72.0,3400,'Lavelanet','Occitanie'),
    ('Trail des Citadelles Maxi',57.0,2800,'Lavelanet','Occitanie'),
    ('Trail des Citadelles Trail',40.0,2000,'Lavelanet','Occitanie'),
    ('Trail des Citadelles Rando',25.0,1200,'Lavelanet','Occitanie'),
    ('Trail des Citadelles Découverte',10.0,500,'Lavelanet','Occitanie'),
    ('SaintéLyon',81.0,2100,'Saint-Étienne','Auvergne-Rhône-Alpes'),
    ('6000D',65.0,3500,'Aime','Auvergne-Rhône-Alpes'),
    ('Marathon du Mont-Blanc',42.0,2540,'Chamonix','Auvergne-Rhône-Alpes'),
    ('Diagonale des Fous',170.0,10500,'Saint-Pierre','La Réunion'),
    ('Grand Raid des Pyrénées',160.0,10500,'Vielle-Aure','Occitanie'),
    ('EcoTrail Paris 120 km',120.0,2000,'Rambouillet','Île-de-France'),
    ('EcoTrail Paris 80 km',80.0,1200,'Élancourt','Île-de-France'),
    ('EcoTrail Paris 45 km',45.0,800,'Versailles','Île-de-France')
) AS v(name, distance_km, elevation_gain_m, location, region)
WHERE NOT EXISTS (
    SELECT 1
    FROM races r
    WHERE r.name = v.name
      AND r.location = v.location
      AND r.distance_km = v.distance_km
);
