-- ============================================================================
-- SCRIPT DE MIGRACIÓN CONTROLADA E IDEMPOTENTE
-- PROYECTO: AdoptMe — Módulo de Centros de Adopción Dinámicos
-- BASE DE DATOS: Neon PostgreSQL 16
-- ARCHIVO: Backend/AdoptMe.Api/Data/Neon_Fase6_Centros_Dinamicos.sql
-- ============================================================================

BEGIN;

-- ----------------------------------------------------------------------------
-- 1. CREACIÓN DE LA TABLA 'Centros'
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS "Centros" (
    "Id" SERIAL PRIMARY KEY,
    "UsuarioId" INTEGER NULL,
    "Nombre" VARCHAR(150) NOT NULL,
    "Descripcion" VARCHAR(1000) NOT NULL,
    "Direccion" VARCHAR(250) NOT NULL,
    "Distrito" VARCHAR(100) NOT NULL,
    "Telefono" VARCHAR(20) NOT NULL,
    "CorreoContacto" VARCHAR(150) NOT NULL,
    "Horario" VARCHAR(150) NOT NULL,
    "Latitud" DOUBLE PRECISION NULL,
    "Longitud" DOUBLE PRECISION NULL,
    "LogoUrl" TEXT NULL,
    "LogoPublicId" TEXT NULL,
    "Estado" VARCHAR(30) NOT NULL DEFAULT 'Pendiente',
    "MotivoRechazo" VARCHAR(500) NULL,
    "FechaSolicitud" TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "FechaRevision" TIMESTAMP WITH TIME ZONE NULL,
    "RevisadoPorUsuarioId" INTEGER NULL,
    "EsDemostrativo" BOOLEAN NOT NULL DEFAULT FALSE,
    "Activo" BOOLEAN NOT NULL DEFAULT TRUE,
    "FechaCreacion" TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "FechaActualizacion" TIMESTAMP WITH TIME ZONE NULL,

    -- Clave foránea hacia el usuario propietario (ON DELETE RESTRICT para proteger integridad)
    CONSTRAINT "FK_Centros_Usuarios_UsuarioId"
        FOREIGN KEY ("UsuarioId")
        REFERENCES "Usuarios"("Id")
        ON DELETE RESTRICT,

    -- Clave foránea hacia el administrador revisor (ON DELETE SET NULL)
    CONSTRAINT "FK_Centros_Usuarios_RevisadoPorUsuarioId"
        FOREIGN KEY ("RevisadoPorUsuarioId")
        REFERENCES "Usuarios"("Id")
        ON DELETE SET NULL,

    -- Validación de estados permitidos del ciclo de vida
    CONSTRAINT "CK_Centros_Estado"
        CHECK ("Estado" IN ('Pendiente', 'Aprobado', 'Rechazado')),

    -- Validación de consistencia por pares en coordenadas geográficas
    CONSTRAINT "CK_Centros_Coordenadas_Pares"
        CHECK (
            ("Latitud" IS NULL AND "Longitud" IS NULL) OR
            ("Latitud" IS NOT NULL AND "Longitud" IS NOT NULL)
        ),

    -- Rangos válidos para coordenadas GPS
    CONSTRAINT "CK_Centros_Latitud_Rango"
        CHECK ("Latitud" IS NULL OR ("Latitud" >= -90.0 AND "Latitud" <= 90.0)),

    CONSTRAINT "CK_Centros_Longitud_Rango"
        CHECK ("Longitud" IS NULL OR ("Longitud" >= -180.0 AND "Longitud" <= 180.0))
);

-- ----------------------------------------------------------------------------
-- 2. ÍNDICES DE RENDIMIENTO Y UNICIDAD
-- ----------------------------------------------------------------------------

-- Índice único parcial: Un usuario real solo puede administrar un único centro.
-- Permite múltiples centros con UsuarioId = NULL (centros demostrativos sin cuenta vinculada).
CREATE UNIQUE INDEX IF NOT EXISTS "IX_Centros_UsuarioId_Unico"
    ON "Centros" ("UsuarioId")
    WHERE "UsuarioId" IS NOT NULL;

-- Índices compuestos para catálogo público y administración
CREATE INDEX IF NOT EXISTS "IX_Centros_Estado_Activo"
    ON "Centros" ("Estado", "Activo");

CREATE INDEX IF NOT EXISTS "IX_Centros_Distrito"
    ON "Centros" ("Distrito");

CREATE INDEX IF NOT EXISTS "IX_Centros_FechaSolicitud"
    ON "Centros" ("FechaSolicitud");


-- ----------------------------------------------------------------------------
-- 3. VINCULACIÓN OPCIONAL EN TABLA 'Mascotas' (CentroId NULLABLE)
-- ----------------------------------------------------------------------------
-- Preserva intactas las 18 mascotas históricas y publicaciones de adoptantes independientes.
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'Mascotas'
          AND column_name = 'CentroId'
    ) THEN
        ALTER TABLE "Mascotas"
            ADD COLUMN "CentroId" INTEGER NULL;

        ALTER TABLE "Mascotas"
            ADD CONSTRAINT "FK_Mascotas_Centros_CentroId"
            FOREIGN KEY ("CentroId")
            REFERENCES "Centros"("Id")
            ON DELETE SET NULL;

        CREATE INDEX "IX_Mascotas_CentroId"
            ON "Mascotas" ("CentroId");
    END IF;
END $$;


-- ----------------------------------------------------------------------------
-- 4. VALIDACIÓN PREVIA ESTRICTA Y SIEMBRA IDEMPOTENTE DE LOS CINCO CENTROS
-- ----------------------------------------------------------------------------
DO $$
DECLARE
    v_usuario_id INTEGER;
    v_conteo_usuarios INTEGER;
    v_nombre_c1 CONSTANT VARCHAR := 'Albergue Huellitas del Norte - Lima (Demo)';
    v_nombre_c2 CONSTANT VARCHAR := 'Refugio Patitas Felices Surco (Demo)';
    v_nombre_c3 CONSTANT VARCHAR := 'Centro de Adopción Miraflores (Demo)';
    v_nombre_c4 CONSTANT VARCHAR := 'Hogar Temporal Ate Vitarte (Demo)';
    v_nombre_c5 CONSTANT VARCHAR := 'Módulo Animal Comas (Demo)';
BEGIN
    -- 4.0. Verificación estricta del usuario demo institucional
    SELECT COUNT(*), MAX("Id")
    INTO v_conteo_usuarios, v_usuario_id
    FROM "Usuarios"
    WHERE lower(trim("Correo")) = 'centro@adoptme.com'
      AND "Rol" = 'Centro';

    IF v_conteo_usuarios = 0 THEN
        RAISE EXCEPTION 'MIGRACIÓN ABORTADA: No se encontró ningún usuario con correo centro@adoptme.com y rol Centro.';
    ELSIF v_conteo_usuarios > 1 THEN
        RAISE EXCEPTION 'MIGRACIÓN ABORTADA: Se encontraron % usuarios coincidentes con correo centro@adoptme.com y rol Centro. Se esperaba exactamente uno.', v_conteo_usuarios;
    END IF;

    -- 4.1. Centro 1: Albergue Huellitas del Norte - Lima (Demo)
    -- Vinculado forzosamente al usuario demo verificado
    IF EXISTS (
        SELECT 1 FROM "Centros"
        WHERE "Nombre" = v_nombre_c1
          AND ("UsuarioId" IS DISTINCT FROM v_usuario_id OR "EsDemostrativo" IS NOT TRUE)
    ) THEN
        RAISE EXCEPTION 'INCONSISTENCIA EN MIGRACIÓN: El Centro 1 ya existe pero posee datos incompatibles con la cuenta institucional.';
    END IF;

    IF NOT EXISTS (SELECT 1 FROM "Centros" WHERE "Nombre" = v_nombre_c1) THEN
        INSERT INTO "Centros" (
            "UsuarioId", "Nombre", "Descripcion", "Direccion", "Distrito",
            "Telefono", "CorreoContacto", "Horario", "Latitud", "Longitud",
            "Estado", "EsDemostrativo", "Activo", "FechaCreacion"
        ) VALUES (
            v_usuario_id,
            v_nombre_c1,
            'Espacio comunitario en SJL dedicado al cuidado y bienestar de perros y gatos rescatados en espera de una familia responsable.',
            'Av. Próceres de la Independencia 1402, SJL',
            'San Juan de Lurigancho',
            '999777666',
            'centro@adoptme.com',
            'Lunes a Viernes de 9:00 a 17:00',
            -12.0089,
            -77.0042,
            'Aprobado',
            TRUE,
            TRUE,
            CURRENT_TIMESTAMP
        );
    END IF;

    -- 4.2. Centro 2: Refugio Patitas Felices Surco (Demo)
    IF EXISTS (
        SELECT 1 FROM "Centros"
        WHERE "Nombre" = v_nombre_c2
          AND ("UsuarioId" IS NOT NULL OR "EsDemostrativo" IS NOT TRUE)
    ) THEN
        RAISE EXCEPTION 'INCONSISTENCIA EN MIGRACIÓN: El Centro 2 ya existe pero no corresponde a un centro demostrativo sin titular.';
    END IF;

    IF NOT EXISTS (SELECT 1 FROM "Centros" WHERE "Nombre" = v_nombre_c2) THEN
        INSERT INTO "Centros" (
            "UsuarioId", "Nombre", "Descripcion", "Direccion", "Distrito",
            "Telefono", "CorreoContacto", "Horario", "Latitud", "Longitud",
            "Estado", "EsDemostrativo", "Activo", "FechaCreacion"
        ) VALUES (
            NULL,
            v_nombre_c2,
            'Albergue demostrativo en Surco con programa de socialización y seguimiento post-adopción para mascotas rescatadas.',
            'Calle Monte Rosa 180, Santiago de Surco',
            'Santiago de Surco',
            '999111222',
            'patitas.surco.demo@ejemplo.pe',
            'Martes a Sábado de 10:00 a 18:00',
            -12.1384,
            -76.9856,
            'Aprobado',
            TRUE,
            TRUE,
            CURRENT_TIMESTAMP
        );
    END IF;

    -- 4.3. Centro 3: Centro de Adopción Miraflores (Demo)
    IF EXISTS (
        SELECT 1 FROM "Centros"
        WHERE "Nombre" = v_nombre_c3
          AND ("UsuarioId" IS NOT NULL OR "EsDemostrativo" IS NOT TRUE)
    ) THEN
        RAISE EXCEPTION 'INCONSISTENCIA EN MIGRACIÓN: El Centro 3 ya existe pero no corresponde a un centro demostrativo sin titular.';
    END IF;

    IF NOT EXISTS (SELECT 1 FROM "Centros" WHERE "Nombre" = v_nombre_c3) THEN
        INSERT INTO "Centros" (
            "UsuarioId", "Nombre", "Descripcion", "Direccion", "Distrito",
            "Telefono", "CorreoContacto", "Horario", "Latitud", "Longitud",
            "Estado", "EsDemostrativo", "Activo", "FechaCreacion"
        ) VALUES (
            NULL,
            v_nombre_c3,
            'Iniciativa en Miraflores enfocada en la sensibilización vecinal y adopción responsable de mascotas en buen estado de salud.',
            'Av. Larco 345, Miraflores',
            'Miraflores',
            '999333444',
            'adopcion.miraflores.demo@ejemplo.pe',
            'Lunes a Domingo de 10:00 a 16:00',
            -12.1221,
            -77.0305,
            'Aprobado',
            TRUE,
            TRUE,
            CURRENT_TIMESTAMP
        );
    END IF;

    -- 4.4. Centro 4: Hogar Temporal Ate Vitarte (Demo)
    IF EXISTS (
        SELECT 1 FROM "Centros"
        WHERE "Nombre" = v_nombre_c4
          AND ("UsuarioId" IS NOT NULL OR "EsDemostrativo" IS NOT TRUE)
    ) THEN
        RAISE EXCEPTION 'INCONSISTENCIA EN MIGRACIÓN: El Centro 4 ya existe pero no corresponde a un centro demostrativo sin titular.';
    END IF;

    IF NOT EXISTS (SELECT 1 FROM "Centros" WHERE "Nombre" = v_nombre_c4) THEN
        INSERT INTO "Centros" (
            "UsuarioId", "Nombre", "Descripcion", "Direccion", "Distrito",
            "Telefono", "CorreoContacto", "Horario", "Latitud", "Longitud",
            "Estado", "EsDemostrativo", "Activo", "FechaCreacion"
        ) VALUES (
            NULL,
            v_nombre_c4,
            'Red de hogares de tránsito en Ate que acoge animales rescatados de la calle mientras encuentran familia definitiva.',
            'Av. Nicolás Ayllón 2890, Ate',
            'Ate Vitarte',
            '999555666',
            'hogar.ate.demo@ejemplo.pe',
            'Miércoles a Domingo de 11:00 a 17:00',
            -12.0461,
            -76.9214,
            'Aprobado',
            TRUE,
            TRUE,
            CURRENT_TIMESTAMP
        );
    END IF;

    -- 4.5. Centro 5: Módulo Animal Comas (Demo)
    IF EXISTS (
        SELECT 1 FROM "Centros"
        WHERE "Nombre" = v_nombre_c5
          AND ("UsuarioId" IS NOT NULL OR "EsDemostrativo" IS NOT TRUE)
    ) THEN
        RAISE EXCEPTION 'INCONSISTENCIA EN MIGRACIÓN: El Centro 5 ya existe pero no corresponde a un centro demostrativo sin titular.';
    END IF;

    IF NOT EXISTS (SELECT 1 FROM "Centros" WHERE "Nombre" = v_nombre_c5) THEN
        INSERT INTO "Centros" (
            "UsuarioId", "Nombre", "Descripcion", "Direccion", "Distrito",
            "Telefono", "CorreoContacto", "Horario", "Latitud", "Longitud",
            "Estado", "EsDemostrativo", "Activo", "FechaCreacion"
        ) VALUES (
            NULL,
            v_nombre_c5,
            'Módulo demostrativo en Comas para atención preventiva, resguardo temporal y coordinación de adopciones en la zona norte de Lima.',
            'Av. Universitaria Norte 4500, Comas',
            'Comas',
            '999777888',
            'modulo.comas.demo@ejemplo.pe',
            'Lunes a Viernes de 8:30 a 16:30',
            -11.9325,
            -77.0542,
            'Aprobado',
            TRUE,
            TRUE,
            CURRENT_TIMESTAMP
        );
    END IF;
END $$;

COMMIT;

-- ============================================================================
-- CONSULTAS DE VERIFICACIÓN Y AUDITORÍA (SOLO LECTURA)
-- ============================================================================

-- 1. Validar la estructura y columnas de la tabla Centros
-- SELECT column_name, data_type, character_maximum_length, is_nullable, column_default
-- FROM information_schema.columns
-- WHERE table_name = 'Centros'
-- ORDER BY ordinal_position;

-- 2. Validar que la columna CentroId se agregó a Mascotas
-- SELECT column_name, data_type, is_nullable
-- FROM information_schema.columns
-- WHERE table_name = 'Mascotas' AND column_name = 'CentroId';

-- 3. Validar las 18 mascotas históricas (todas deben tener CentroId = NULL inicialmente)
-- SELECT COUNT(*) AS total_mascotas,
--        COUNT("CentroId") AS con_centro,
--        COUNT(*) - COUNT("CentroId") AS sin_centro
-- FROM "Mascotas";

-- 4. Listar los centros registrados con su estado y propietario vinculado
-- SELECT "Id", "Nombre", "Distrito", "Estado", "Activo", "EsDemostrativo", "UsuarioId"
-- FROM "Centros"
-- ORDER BY "Id";

-- 5. Validar que el usuario demo 'centro@adoptme.com' quedó correctamente vinculado al Centro 1
-- SELECT c."Id" AS centro_id, c."Nombre" AS centro_nombre, u."Id" AS usuario_id, u."Correo", u."Rol"
-- FROM "Centros" c
-- LEFT JOIN "Usuarios" u ON c."UsuarioId" = u."Id"
-- WHERE c."Nombre" = 'Albergue Huellitas del Norte - Lima (Demo)';

-- 6. Verificar que los índices existen en el catálogo de PostgreSQL
-- SELECT indexname, indexdef
-- FROM pg_indexes
-- WHERE tablename IN ('Centros', 'Mascotas')
-- ORDER BY tablename, indexname;
