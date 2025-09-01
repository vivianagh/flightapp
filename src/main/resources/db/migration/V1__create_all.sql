-- =========================
-- V1: CREAR TODA LA BASE
-- =========================

-- 1) flight_over_home (coincide con FlightOverHomeEntity)
CREATE TABLE IF NOT EXISTS flight_over_home (
                                                id                 BIGSERIAL PRIMARY KEY,
                                                icao24             VARCHAR(16) NOT NULL,
                                                callsign           VARCHAR(16),
                                                airline            VARCHAR(64),
                                                latitude           DOUBLE PRECISION,
                                                longitude          DOUBLE PRECISION,
                                                altitude           DOUBLE PRECISION,
                                                speed              DOUBLE PRECISION,
                                                logged_date        VARCHAR(16),
                                                logged_time        VARCHAR(16),
                                                origin             VARCHAR(8),
                                                destination        VARCHAR(8)
);

-- Evita duplicados por avión + timestamp
CREATE UNIQUE INDEX IF NOT EXISTS uq_foh_icao24_logged
    ON flight_over_home (icao24, logged_date, logged_time);

-- 2) flights_per_day (coincide con FlightsPerDayEntity)
CREATE TABLE IF NOT EXISTS flights_per_day (
                                               date  DATE PRIMARY KEY,
                                               count INT  NOT NULL DEFAULT 0
);

-- 3) daily_flight_count (coincide con DailyFlightCountEntity)
CREATE TABLE IF NOT EXISTS daily_flight_count (
                                                  date  DATE PRIMARY KEY,
                                                  count INT  NOT NULL DEFAULT 0
);

-- 4) flights_per_hour y flights_per_hour_seen (tu V2)
CREATE TABLE IF NOT EXISTS flights_per_hour (
                                                date  DATE NOT NULL,
                                                hour  INT  NOT NULL CHECK (hour BETWEEN 0 AND 23),
                                                count INT  NOT NULL DEFAULT 0,
                                                PRIMARY KEY (date, hour)
);

CREATE TABLE IF NOT EXISTS flights_per_hour_seen (
                                                     date   DATE        NOT NULL,
                                                     hour   INT         NOT NULL CHECK (hour BETWEEN 0 AND 23),
                                                     icao24 VARCHAR(10) NOT NULL,
                                                     PRIMARY KEY (date, hour, icao24)
);

-- 5) flight_route_cache (coincide con FlightRouteCacheEntity)
CREATE TABLE IF NOT EXISTS flight_route_cache (
                                                  flight_number VARCHAR(10) PRIMARY KEY,  -- PK
                                                  icao24        VARCHAR(6),
                                                  origin        VARCHAR(4),
                                                  destination   VARCHAR(4),
                                                  updated_at    TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_frc_icao24     ON flight_route_cache (icao24);
CREATE INDEX IF NOT EXISTS idx_frc_updated_at ON flight_route_cache (updated_at);
