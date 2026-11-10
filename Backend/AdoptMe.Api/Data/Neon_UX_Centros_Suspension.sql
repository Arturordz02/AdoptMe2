-- ============================================================================
-- MIGRACIÓN DEFINITIVA: SUSPENSIÓN OPERATIVA Y REACTIVACIÓN DE CENTROS
-- PROYECTO: AdoptMe — Plataforma de Adopción Responsable
-- ARCHIVO: Backend/AdoptMe.Api/Data/Neon_UX_Centros_Suspension.sql
-- FASE: UX-B3
-- COMPATIBILIDAD: Neon PostgreSQL 16 (Idempotente y Transaccional)
-- ============================================================================
-- OBJETIVO:
-- 1. Diferenciar 'Rechazo' (evaluación inicial) de 'Suspensión Operativa' (medida
--    administrativa sobre un centro previamente aprobado y activo).
-- 2. Preservar Estado = 'Aprobado' y cuenta histórica cuando Activo = FALSE.
-- 3. Registrar auditoría completa de suspensión (motivo, fecha, admin) y reactivación.
-- ============================================================================

BEGIN;

-- ----------------------------------------------------------------------------
-- 1. ADICIÓN IDEMPOTENTE DE COLUMNAS DE SUSPENSIÓN Y AUDITORÍA EN "Centros"
-- ----------------------------------------------------------------------------

DO $$
BEGIN
    -- Motivo de la suspensión administrativa (obligatorio al suspender)
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'Centros'
          AND column_name = 'MotivoSuspension'
    ) THEN
        ALTER TABLE "Centros"
            ADD COLUMN "MotivoSuspension" TEXT NULL;
        RAISE NOTICE 'Columna MotivoSuspension agregada a tabla Centros.';
    ELSE
        RAISE NOTICE 'Columna MotivoSuspension ya existe en tabla Centros.';
    END IF;

    -- Fecha y hora UTC en que el centro fue suspendido por administración
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'Centros'
          AND column_name = 'FechaSuspension'
    ) THEN
        ALTER TABLE "Centros"
            ADD COLUMN "FechaSuspension" TIMESTAMP WITH TIME ZONE NULL;
        RAISE NOTICE 'Columna FechaSuspension agregada a tabla Centros.';
    ELSE
        RAISE NOTICE 'Columna FechaSuspension ya existe en tabla Centros.';
    END IF;

    -- Identificador del administrador que ordenó la suspensión
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'Centros'
          AND column_name = 'SuspendidoPorAdminId'
    ) THEN
        ALTER TABLE "Centros"
            ADD COLUMN "SuspendidoPorAdminId" INTEGER NULL;
        RAISE NOTICE 'Columna SuspendidoPorAdminId agregada a tabla Centros.';
    ELSE
        RAISE NOTICE 'Columna SuspendidoPorAdminId ya existe en tabla Centros.';
    END IF;

    -- Fecha y hora UTC de la última rehabilitación/reactivación administrativa
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'Centros'
          AND column_name = 'FechaReactivacion'
    ) THEN
        ALTER TABLE "Centros"
            ADD COLUMN "FechaReactivacion" TIMESTAMP WITH TIME ZONE NULL;
        RAISE NOTICE 'Columna FechaReactivacion agregada a tabla Centros.';
    ELSE
        RAISE NOTICE 'Columna FechaReactivacion ya existe en tabla Centros.';
    END IF;
END $$;

-- ----------------------------------------------------------------------------
-- 2. RESTRICCIÓN DE CLAVE FORÁNEA PARA EL ADMINISTRADOR QUE SUSPENDE
-- ----------------------------------------------------------------------------
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE constraint_name = 'FK_Centros_Usuarios_SuspendidoPorAdminId'
          AND table_name = 'Centros'
    ) THEN
        ALTER TABLE "Centros"
            ADD CONSTRAINT "FK_Centros_Usuarios_SuspendidoPorAdminId"
            FOREIGN KEY ("SuspendidoPorAdminId")
            REFERENCES "Usuarios"("Id")
            ON DELETE SET NULL;
        RAISE NOTICE 'Restricción FK_Centros_Usuarios_SuspendidoPorAdminId agregada.';
    ELSE
        RAISE NOTICE 'Restricción FK_Centros_Usuarios_SuspendidoPorAdminId ya existe.';
    END IF;
END $$;

-- ----------------------------------------------------------------------------
-- 3. RESTRICCIÓN DE INTEGRIDAD DE ESTADOS Y SUSPENSIÓN (CHECK)
-- ----------------------------------------------------------------------------
-- Regla de integridad:
-- Si un centro registra metadatos de suspensión, su estado formal debe ser 'Aprobado'.
-- Un centro en 'Pendiente' o 'Rechazado' no puede estar suspendido.
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE constraint_name = 'CK_Centros_Suspension_Consistencia'
          AND table_name = 'Centros'
    ) THEN
        ALTER TABLE "Centros"
            ADD CONSTRAINT "CK_Centros_Suspension_Consistencia"
            CHECK (
                ("MotivoSuspension" IS NULL AND "FechaSuspension" IS NULL AND "SuspendidoPorAdminId" IS NULL) OR
                ("Estado" = 'Aprobado')
            );
        RAISE NOTICE 'Restricción CK_Centros_Suspension_Consistencia agregada.';
    ELSE
        RAISE NOTICE 'Restricción CK_Centros_Suspension_Consistencia ya existe.';
    END IF;
END $$;

-- ----------------------------------------------------------------------------
-- 4. ÍNDICE DE RENDIMIENTO PARA FILTRO ADMINISTRATIVO DE SUSPENDIDOS
-- ----------------------------------------------------------------------------
CREATE INDEX IF NOT EXISTS "IX_Centros_Suspension"
    ON "Centros" ("Activo", "FechaSuspension")
    WHERE "Activo" = FALSE AND "MotivoSuspension" IS NOT NULL;

-- ----------------------------------------------------------------------------
-- 5. VALIDACIÓN DE NO REGRESIÓN DE DATOS HISTÓRICOS
-- ----------------------------------------------------------------------------
DO $$
DECLARE
    conteo_centros INT;
    conteo_mascotas INT;
BEGIN
    SELECT COUNT(*) INTO conteo_centros FROM "Centros";
    SELECT COUNT(*) INTO conteo_mascotas FROM "Mascotas";
    RAISE NOTICE 'Migración completada: % centros registrados, % mascotas históricas en Neon.', conteo_centros, conteo_mascotas;
END $$;

COMMIT;
