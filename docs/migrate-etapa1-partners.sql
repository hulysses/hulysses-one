-- Execute manually on a backed-up Etapa 1 database, with both applications stopped.
-- Default source schema: public. Default partner schema: business_partner_service.
-- This changes ownership boundaries, keeps all data/IDs and removes only cross-boundary FKs.
BEGIN;

CREATE SCHEMA IF NOT EXISTS business_partner_service;

DO $$
DECLARE
    fk record;
BEGIN
    IF to_regclass('public.business_partner') IS NULL
       OR to_regclass('public.address') IS NULL
       OR to_regclass('public.business_partner_role') IS NULL THEN
        RAISE EXCEPTION 'Expected Etapa 1 partner tables in public; aborting migration';
    END IF;
    IF to_regclass('business_partner_service.business_partner') IS NOT NULL
       OR to_regclass('business_partner_service.address') IS NOT NULL
       OR to_regclass('business_partner_service.business_partner_role') IS NOT NULL THEN
        RAISE EXCEPTION 'Partner destination tables already exist; aborting migration';
    END IF;
    FOR fk IN
        SELECT conrelid::regclass AS table_name, conname
        FROM pg_constraint
        WHERE contype = 'f'
          AND confrelid = 'public.business_partner'::regclass
          AND conrelid IN (to_regclass('public.product'), to_regclass('public.sales_order'))
    LOOP
        EXECUTE format('ALTER TABLE %s DROP CONSTRAINT %I', fk.table_name, fk.conname);
    END LOOP;
END $$;

-- PostgreSQL also moves associated indexes and sequences owned by these tables.
ALTER TABLE public.business_partner SET SCHEMA business_partner_service;
ALTER TABLE public.address SET SCHEMA business_partner_service;
ALTER TABLE public.business_partner_role SET SCHEMA business_partner_service;

COMMIT;
