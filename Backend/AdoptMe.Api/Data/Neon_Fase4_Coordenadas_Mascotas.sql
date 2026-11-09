-- =============================================================================
-- SCRIPT DDL: Coordenadas geográficas para Mascotas en PostgreSQL (Neon)
-- Entrega #4: Integración con Google Maps y Geolocalización
-- Proyecto AdoptMe
-- =============================================================================

-- 1. Agregar columna Latitud si no existe
ALTER TABLE "Mascotas"
ADD COLUMN IF NOT EXISTS "Latitud" double precision NULL;

-- 2. Agregar columna Longitud si no existe
ALTER TABLE "Mascotas"
ADD COLUMN IF NOT EXISTS "Longitud" double precision NULL;

-- 3. Documentación del rango válido:
-- Latitud: -90.0 a 90.0
-- Longitud: -180.0 a 180.0
-- Registros anteriores permanecen con valores NULL manteniendo compatibilidad total.
