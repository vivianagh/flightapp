-- Acelera búsquedas por icao24 y updated_at
CREATE INDEX IF NOT EXISTS idx_frc_icao24_updated ON flight_route_cache (icao24, updated_at DESC);
-- Por flight_number (aunque sea PK, a veces el planner agradece)
CREATE INDEX IF NOT EXISTS idx_frc_flightnumber ON flight_route_cache (flight_number);