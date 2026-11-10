-- =============================================================================
-- AdoptMe - Migración SQL Idempotente: Gestión de Apelaciones de Centros Suspendidos
-- Archivo: Neon_UX_Centros_Apelaciones.sql
-- Fase: C9 - Migración Remota y Despliegue de Apelaciones
-- =============================================================================

CREATE TABLE IF NOT EXISTS "CentrosApelaciones" (
    "Id" SERIAL PRIMARY KEY,
    "CentroId" INTEGER NOT NULL,
    "MotivoApelacion" VARCHAR(2000) NOT NULL,
    "Estado" VARCHAR(30) NOT NULL DEFAULT 'Pendiente',
    "FechaCreacion" TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    "FechaRevision" TIMESTAMP WITH TIME ZONE NULL,
    "RevisadoPorAdminId" INTEGER NULL,
    "RespuestaAdmin" VARCHAR(2000) NULL,
    CONSTRAINT "FK_CentrosApelaciones_Centros_CentroId" 
        FOREIGN KEY ("CentroId") REFERENCES "Centros" ("Id") ON DELETE CASCADE,
    CONSTRAINT "FK_CentrosApelaciones_Usuarios_RevisadoPorAdminId" 
        FOREIGN KEY ("RevisadoPorAdminId") REFERENCES "Usuarios" ("Id") ON DELETE SET NULL,
    CONSTRAINT "CK_CentrosApelaciones_Estado" 
        CHECK ("Estado" IN ('Pendiente', 'Aprobada', 'Rechazada'))
);

-- Índices de consulta de alto rendimiento
CREATE INDEX IF NOT EXISTS "IX_CentrosApelaciones_CentroId" 
    ON "CentrosApelaciones" ("CentroId");

CREATE INDEX IF NOT EXISTS "IX_CentrosApelaciones_Estado" 
    ON "CentrosApelaciones" ("Estado");

CREATE INDEX IF NOT EXISTS "IX_CentrosApelaciones_FechaCreacion" 
    ON "CentrosApelaciones" ("FechaCreacion" DESC);

-- Índice único condicional: impide más de una apelación en estado 'Pendiente' por centro
CREATE UNIQUE INDEX IF NOT EXISTS "UX_CentrosApelaciones_Centro_Pendiente" 
    ON "CentrosApelaciones" ("CentroId") 
    WHERE "Estado" = 'Pendiente';
