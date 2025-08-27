-- Crea restricción única en (icao24, logged_date, logged_time) si no existe.
-- Idempotente y seguro aunque la tabla aún no exista.

DO $$
    BEGIN
        IF EXISTS (
            SELECT 1
            FROM information_schema.tables
            WHERE table_schema = 'public'
              AND table_name   = 'flight_over_home'
        ) AND NOT EXISTS (
            SELECT 1
            FROM pg_constraint c
                     JOIN pg_class      t ON t.oid = c.conrelid
                     JOIN pg_namespace  n ON n.oid = t.relnamespace
            WHERE c.conname = 'uq_foh'
              AND t.relname = 'flight_over_home'
              AND n.nspname = 'public'
        ) THEN
            ALTER TABLE public.flight_over_home
                ADD CONSTRAINT uq_foh
                    UNIQUE (icao24, logged_date, logged_time);
        END IF;
    END
$$;
