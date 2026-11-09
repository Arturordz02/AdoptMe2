-- =============================================================================
-- Neon_Fase5_ImagePublicId_Mascotas.sql
-- MIGRACIÓN DE ESQUEMA IDEMPOTENTE: Agregar ImagePublicId a Mascotas
-- Proyecto: AdoptMe
-- Entrega / Fase: Fase A - Imágenes Persistentes con Cloudinary
--
-- ESTADO DE EJECUCIÓN:
-- 1. Migración ejecutada exitosamente en Neon PostgreSQL durante la Fase A2.
-- 2. Idempotencia validada en segunda ejecución (NOTICE: La columna ya existe).
-- 3. Tipo real verificado en information_schema.columns: text, is_nullable = YES.
-- 4. Conserva intactos los 18 registros históricos preexistentes con ImagePublicId NULL.
-- =============================================================================

DO $$
BEGIN
    -- Comprobar si la columna ImagePublicId ya existe en la tabla Mascotas
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND lower(table_name) = 'mascotas'
          AND lower(column_name) = 'imagepublicid'
    ) THEN
        ALTER TABLE public."Mascotas"
        ADD COLUMN "ImagePublicId" text NULL;

        RAISE NOTICE 'Columna ImagePublicId agregada exitosamente a la tabla Mascotas.';
    ELSE
        RAISE NOTICE 'La columna ImagePublicId ya existe en la tabla Mascotas. Omitiendo.';
    END IF;
END $$;
