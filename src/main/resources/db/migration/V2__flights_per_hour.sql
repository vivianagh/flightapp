-- Conteo por hora
CREATE TABLE IF NOT EXISTS flights_per_hour (
                                                date  date      NOT NULL,
                                                hour  int       NOT NULL CHECK (hour BETWEEN 0 AND 23),
                                                count int       NOT NULL DEFAULT 0,
                                                PRIMARY KEY (date, hour)
);

-- Dedupe: un icao24 por hora y día se cuenta una sola vez
CREATE TABLE IF NOT EXISTS flights_per_hour_seen (
                                                     date   date        NOT NULL,
                                                     hour   int         NOT NULL CHECK (hour BETWEEN 0 AND 23),
                                                     icao24 varchar(10) NOT NULL,
                                                     PRIMARY KEY (date, hour, icao24)
);
