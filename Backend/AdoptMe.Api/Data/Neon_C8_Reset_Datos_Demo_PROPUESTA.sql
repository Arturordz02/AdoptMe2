-- ==============================================================================
-- ADOPTME - FASE C8: REINICIO SEGURO DE DATOS DEMOSTRATIVOS
-- Archivo: Backend/AdoptMe.Api/Data/Neon_C8_Reset_Datos_Demo_PROPUESTA.sql
-- Estado: PROPUESTA PARA ENSAYO CONTROLADO CON ROLLBACK
-- Base de Datos: Neon PostgreSQL 16
-- Fecha: 2026-11-10
-- ==============================================================================
-- ADVERTENCIA DE SEGURIDAD:
-- Este script es de CARÁCTER TRANSACCIONAL.
-- Finaliza con ROLLBACK; para el ensayo obligatorio.
-- ==============================================================================

BEGIN;

-- ------------------------------------------------------------------------------
-- PASO 1: CREAR TABLA TEMPORAL DE USUARIOS PROTEGIDOS
-- ------------------------------------------------------------------------------
CREATE TEMP TABLE temp_usuarios_protegidos (
    "Id" INT PRIMARY KEY,
    "Correo" VARCHAR(150),
    "Motivo" VARCHAR(200)
);

INSERT INTO temp_usuarios_protegidos ("Id", "Correo", "Motivo")
VALUES
    (3, 'arthur01961@outlook.com', 'Cuenta personal del desarrollador / Administrador técnico'),
    (5, 'admin@adoptme.com', 'Administrador principal del sistema demostrativo'),
    (6, 'centro@adoptme.com', 'Responsable vinculado al Centro #1 (Albergue Huellitas del Norte)'),
    (7, 'usuario@adoptme.com', 'Adoptante demostrativo oficial para pruebas de evaluación');

-- ------------------------------------------------------------------------------
-- PASO 1.1: VALIDACIÓN PREVIA DE SEGURIDAD
-- ------------------------------------------------------------------------------
-- Impedir la ejecución si existe algún usuario vinculado a un Centro que NO esté en la lista protegida.
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM "Usuarios" u
        JOIN "Centros" c ON c."UsuarioId" = u."Id"
        WHERE u."Id" NOT IN (SELECT "Id" FROM temp_usuarios_protegidos)
    ) THEN
        RAISE EXCEPTION 'Abortando transacción: Se detectó un usuario vinculado a un Centro fuera de la lista protegida.';
    END IF;

    -- Verificar que los 4 usuarios protegidos existen con sus correos exactos
    IF (
        SELECT COUNT(*)
        FROM "Usuarios" u
        JOIN temp_usuarios_protegidos p ON u."Id" = p."Id" AND u."Correo" = p."Correo"
    ) != 4 THEN
        RAISE EXCEPTION 'Abortando transacción: No coinciden exactamente los usuarios protegidos esperados.';
    END IF;
END $$;

-- ------------------------------------------------------------------------------
-- PASO 2: ELIMINACIÓN ORDENADA RESPETANDO FOREIGN KEYS (RESTRICT / CASCADE)
-- ------------------------------------------------------------------------------
-- 2.1 Eliminar todas las solicitudes residuales de prueba
--     Depende de Mascotas y Adoptantes (FK ON DELETE RESTRICT)
DELETE FROM "Solicitudes";

-- 2.2 Eliminar todas las mascotas residuales anteriores
--     Mascotas previas carecían de coordenadas válidas para Google Maps
DELETE FROM "Mascotas";

-- 2.3 Eliminar usuarios residuales que no están en la lista protegida
--     Incluye el usuario ID 32 (teddy@gmail.com) y cuentas de prueba sintéticas.
DELETE FROM "Usuarios"
WHERE "Id" NOT IN (SELECT "Id" FROM temp_usuarios_protegidos);

-- ------------------------------------------------------------------------------
-- PASO 3: REASIGNACIÓN Y ESTADO DE CENTROS DEMOSTRATIVOS
-- ------------------------------------------------------------------------------
-- Asegurar que los 5 centros demostrativos permanezcan Aprobados, Activos y sin suspensiones.
UPDATE "Centros"
SET "Estado" = 'Aprobado',
    "Activo" = TRUE,
    "MotivoSuspension" = NULL,
    "FechaSuspension" = NULL,
    "SuspendidoPorAdminId" = NULL
WHERE "Id" IN (1, 2, 3, 4, 5);

-- ------------------------------------------------------------------------------
-- PASO 4: INSERCIÓN DE ADOPTANTES DEMOSTRATIVOS COHERENTES
-- ------------------------------------------------------------------------------
-- Se reutiliza el hash de contraseña almacenado del usuario demo (usuario@adoptme.com)
-- para mantener coherencia de autenticación sin hardcodear credenciales en texto claro.

DO $$
DECLARE
    v_hash VARCHAR(500);
BEGIN
    SELECT "PasswordHash" INTO v_hash
    FROM "Usuarios"
    WHERE "Correo" = 'usuario@adoptme.com' AND "Id" = 7
    LIMIT 1;

    IF v_hash IS NULL OR LENGTH(TRIM(v_hash)) = 0 THEN
        RAISE EXCEPTION 'Abortando transacción: No se encontró el hash de contraseña del usuario demo oficial.';
    END IF;

    -- Adoptante Demo 2: Carlos Mendoza (Surco)
    INSERT INTO "Usuarios" ("Nombre", "Correo", "PasswordHash", "Telefono", "Rol", "FechaRegistro")
    VALUES ('Carlos Mendoza', 'carlos.mendoza@adoptme.pe', v_hash, '987112233', 'Adoptante', NOW());

    -- Adoptante Demo 3: Ana Valdivia (Miraflores)
    INSERT INTO "Usuarios" ("Nombre", "Correo", "PasswordHash", "Telefono", "Rol", "FechaRegistro")
    VALUES ('Ana Valdivia', 'ana.valdivia@adoptme.pe', v_hash, '987223344', 'Adoptante', NOW());

    -- Adoptante Demo 4: Lucía Paredes (San Juan de Lurigancho)
    INSERT INTO "Usuarios" ("Nombre", "Correo", "PasswordHash", "Telefono", "Rol", "FechaRegistro")
    VALUES ('Lucía Paredes', 'lucia.paredes@adoptme.pe', v_hash, '987334455', 'Adoptante', NOW());
END $$;

-- ------------------------------------------------------------------------------
-- PASO 5: INSERCIÓN DE MASCOTAS DEMOSTRATIVAS COHERENTES CON COORDENADAS GPS
-- ------------------------------------------------------------------------------
-- Requisitos cumplidos:
-- - Exactamente 2 perros, 2 gatos, 2 conejos (total 6).
-- - 5 mascotas con estado 'Disponible' y 1 con estado 'En proceso' (Luna).
-- - Coordenadas Lat/Long reales de Lima para visualización en Google Maps.
-- - CentroId válido existente.
-- - URLs de imágenes HTTPS reales y accesibles de Unsplash.
-- - ImagePublicId = NULL (fotos externas, no Cloudinary).

-- 5.1 Perro Disponible: Rocky (Labrador Retriever) en SJL (Centro 1)
INSERT INTO "Mascotas" (
    "Name", "Species", "Breed", "Age", "Gender", "Location", "Status",
    "Description", "ImageUrl", "CreadoPorCorreo", "FechaCreacion",
    "Latitud", "Longitud", "ImagePublicId", "CentroId"
)
VALUES (
    'Rocky',
    'Perro',
    'Labrador Retriever',
    '2 años',
    'Macho',
    'San Juan de Lurigancho, Lima',
    'Disponible',
    'Rocky es un labrador noble, cariñoso y muy sociable con niños y otras mascotas. Cuenta con vacunas completas y desparasitación al día.',
    'https://images.unsplash.com/photo-1552053831-71594a27632d?auto=format&fit=crop&w=800&q=80',
    'centro@adoptme.com',
    NOW(),
    -12.012500000000,
    -77.001800000000,
    NULL,
    1
);

-- 5.2 Gato Disponible: Mía (Siamés) en Miraflores (Centro 3)
INSERT INTO "Mascotas" (
    "Name", "Species", "Breed", "Age", "Gender", "Location", "Status",
    "Description", "ImageUrl", "CreadoPorCorreo", "FechaCreacion",
    "Latitud", "Longitud", "ImagePublicId", "CentroId"
)
VALUES (
    'Mía',
    'Gato',
    'Siamés',
    '1 año',
    'Hembra',
    'Miraflores, Lima',
    'Disponible',
    'Mía es una gatita tranquila, elegante y juguetona. Acostumbrada a vivir en departamento y utilizar arenero.',
    'https://images.unsplash.com/photo-1514888286974-6c03e2ca1dba?auto=format&fit=crop&w=800&q=80',
    'centro@adoptme.com',
    NOW(),
    -12.119800000000,
    -77.034500000000,
    NULL,
    3
);

-- 5.3 Conejo Disponible: Copito (Belier Enano) en Santiago de Surco (Centro 2)
INSERT INTO "Mascotas" (
    "Name", "Species", "Breed", "Age", "Gender", "Location", "Status",
    "Description", "ImageUrl", "CreadoPorCorreo", "FechaCreacion",
    "Latitud", "Longitud", "ImagePublicId", "CentroId"
)
VALUES (
    'Copito',
    'Conejo',
    'Belier Enano',
    '6 meses',
    'Macho',
    'Santiago de Surco, Lima',
    'Disponible',
    'Copito es un conejito orejón muy dócil y activo. Le encanta comer heno fresco y convivir en espacios interiores seguros.',
    'https://images.unsplash.com/photo-1585110396000-c9ffd4e4b308?auto=format&fit=crop&w=800&q=80',
    'centro@adoptme.com',
    NOW(),
    -12.135000000000,
    -76.982000000000,
    NULL,
    2
);

-- 5.4 Perro En Proceso: Luna (Beagle) en Ate Vitarte (Centro 4)
INSERT INTO "Mascotas" (
    "Name", "Species", "Breed", "Age", "Gender", "Location", "Status",
    "Description", "ImageUrl", "CreadoPorCorreo", "FechaCreacion",
    "Latitud", "Longitud", "ImagePublicId", "CentroId"
)
VALUES (
    'Luna',
    'Perro',
    'Beagle',
    '3 años',
    'Hembra',
    'Ate Vitarte, Lima',
    'En proceso',
    'Luna se encuentra actualmente en proceso de adopción y evaluación con una familia postulante. Es alegre y muy enérgica.',
    'https://images.unsplash.com/photo-1505628346881-b72b27e84530?auto=format&fit=crop&w=800&q=80',
    'centro@adoptme.com',
    NOW(),
    -12.048000000000,
    -76.925000000000,
    NULL,
    4
);

-- 5.5 Gato Disponible: Simba (Atigrado Europeo) en Comas (Centro 5)
INSERT INTO "Mascotas" (
    "Name", "Species", "Breed", "Age", "Gender", "Location", "Status",
    "Description", "ImageUrl", "CreadoPorCorreo", "FechaCreacion",
    "Latitud", "Longitud", "ImagePublicId", "CentroId"
)
VALUES (
    'Simba',
    'Gato',
    'Atigrado Europeo',
    '8 meses',
    'Macho',
    'Comas, Lima',
    'Disponible',
    'Simba es un gato curioso, cariñoso y muy afectuoso con las personas. Esterilizado y con cartilla de vacunación.',
    'https://images.unsplash.com/photo-1533738363-b7f9aef128ce?auto=format&fit=crop&w=800&q=80',
    'centro@adoptme.com',
    NOW(),
    -11.935000000000,
    -77.051000000000,
    NULL,
    5
);

-- 5.6 Conejo Disponible: Tambor (Holandés Enano) en SJL (Centro 1)
INSERT INTO "Mascotas" (
    "Name", "Species", "Breed", "Age", "Gender", "Location", "Status",
    "Description", "ImageUrl", "CreadoPorCorreo", "FechaCreacion",
    "Latitud", "Longitud", "ImagePublicId", "CentroId"
)
VALUES (
    'Tambor',
    'Conejo',
    'Holandés Enano',
    '1 año',
    'Macho',
    'San Juan de Lurigancho, Lima',
    'Disponible',
    'Tambor es un conejito pequeño, tranquilo y acostumbrado al contacto humano responsable.',
    'https://images.unsplash.com/photo-1518796745738-41048802f99a?auto=format&fit=crop&w=800&q=80',
    'centro@adoptme.com',
    NOW(),
    -12.006000000000,
    -77.008000000000,
    NULL,
    1
);

-- ------------------------------------------------------------------------------
-- PASO 6: INSERCIÓN DE SOLICITUD DEMOSTRATIVA COHERENTE (EN EVALUACIÓN)
-- ------------------------------------------------------------------------------
DO $$
DECLARE
    v_mascota_id INT;
    v_adoptante_id INT;
BEGIN
    SELECT "Id" INTO v_mascota_id FROM "Mascotas" WHERE "Name" = 'Luna' LIMIT 1;
    SELECT "Id" INTO v_adoptante_id FROM "Usuarios" WHERE "Correo" = 'usuario@adoptme.com' LIMIT 1;

    IF v_mascota_id IS NOT NULL AND v_adoptante_id IS NOT NULL THEN
        INSERT INTO "Solicitudes" (
            "MascotaId", "AdoptanteId", "PetName", "PetSpecies", "ApplicantName",
            "Phone", "Email", "Address", "HousingType", "TimeAvailability",
            "PreviousExperience", "Reason", "Status", "Observacion",
            "FechaSolicitud", "FechaActualizacion"
        )
        VALUES (
            v_mascota_id,
            v_adoptante_id,
            'Luna',
            'Perro',
            'Adoptante Demo',
            '999888777',
            'usuario@adoptme.com',
            'Av. Principal 123, Ate Vitarte',
            'Casa con patio cercado',
            'Tiempo completo y fines de semana',
            'Sí, he tenido perros anteriormente por más de 8 años.',
            'Deseamos brindarle un hogar amoroso y espacio para ejercitarse.',
            'En revisión',
            'Solicitud demostrativa en proceso de evaluación.',
            NOW(),
            NOW()
        );
    ELSE
        RAISE EXCEPTION 'Abortando transacción: No se pudo localizar la mascota Luna o el usuario adoptante demo.';
    END IF;
END $$;

-- ------------------------------------------------------------------------------
-- PASO 7: VALIDACIÓN ESTRICTA DE CONTEOS Y REGLAS DE INTEGRIDAD
-- ------------------------------------------------------------------------------
DO $$
DECLARE
    v_cnt_usuarios INT;
    v_cnt_centros INT;
    v_cnt_mascotas INT;
    v_cnt_solicitudes INT;
    v_cnt_disponibles INT;
    v_cnt_enproceso INT;
    v_cnt_sin_gps INT;
    v_cnt_con_publicid INT;
BEGIN
    SELECT COUNT(*) INTO v_cnt_usuarios FROM "Usuarios";
    SELECT COUNT(*) INTO v_cnt_centros FROM "Centros";
    SELECT COUNT(*) INTO v_cnt_mascotas FROM "Mascotas";
    SELECT COUNT(*) INTO v_cnt_solicitudes FROM "Solicitudes";

    SELECT COUNT(*) INTO v_cnt_disponibles FROM "Mascotas" WHERE "Status" = 'Disponible';
    SELECT COUNT(*) INTO v_cnt_enproceso FROM "Mascotas" WHERE "Status" = 'En proceso';
    SELECT COUNT(*) INTO v_cnt_sin_gps FROM "Mascotas" WHERE "Latitud" IS NULL OR "Longitud" IS NULL;
    SELECT COUNT(*) INTO v_cnt_con_publicid FROM "Mascotas" WHERE "ImagePublicId" IS NOT NULL;

    IF v_cnt_usuarios != 7 THEN
        RAISE EXCEPTION 'Conteo incorrecto de Usuarios: esperado 7, obtenido %', v_cnt_usuarios;
    END IF;

    IF v_cnt_centros != 5 THEN
        RAISE EXCEPTION 'Conteo incorrecto de Centros: esperado 5, obtenido %', v_cnt_centros;
    END IF;

    IF v_cnt_mascotas != 6 THEN
        RAISE EXCEPTION 'Conteo incorrecto de Mascotas: esperado 6, obtenido %', v_cnt_mascotas;
    END IF;

    IF v_cnt_solicitudes != 1 THEN
        RAISE EXCEPTION 'Conteo incorrecto de Solicitudes: esperado 1, obtenido %', v_cnt_solicitudes;
    END IF;

    IF v_cnt_disponibles != 5 OR v_cnt_enproceso != 1 THEN
        RAISE EXCEPTION 'Distribución de estados en Mascotas incorrecta: Disponibles %, En proceso %', v_cnt_disponibles, v_cnt_enproceso;
    END IF;

    IF v_cnt_sin_gps != 0 THEN
        RAISE EXCEPTION 'Existen % mascotas sin coordenadas geográficas válidas.', v_cnt_sin_gps;
    END IF;

    IF v_cnt_con_publicid != 0 THEN
        RAISE EXCEPTION 'Existen % mascotas con ImagePublicId no nulo para Unsplash.', v_cnt_con_publicid;
    END IF;

    -- Verificar que teddy@gmail.com no exista
    IF EXISTS (SELECT 1 FROM "Usuarios" WHERE "Correo" = 'teddy@gmail.com') THEN
        RAISE EXCEPTION 'El usuario teddy@gmail.com no fue eliminado.';
    END IF;
END $$;

-- ------------------------------------------------------------------------------
-- PASO 8: CONSULTAS INFORMATIVAS
-- ------------------------------------------------------------------------------
SELECT 'Usuarios finales:' AS metrica, COUNT(*) AS total FROM "Usuarios"
UNION ALL
SELECT 'Centros finales:', COUNT(*) FROM "Centros"
UNION ALL
SELECT 'Mascotas finales:', COUNT(*) FROM "Mascotas"
UNION ALL
SELECT 'Solicitudes finales:', COUNT(*) FROM "Solicitudes";

SELECT "Id", "Name", "Species", "Breed", "Status", "Latitud", "Longitud", "CentroId", "Location"
FROM "Mascotas"
ORDER BY "Id";

-- ------------------------------------------------------------------------------
-- CIERRE DE SEGURIDAD (ENSAYO OBLIGATORIO)
-- ------------------------------------------------------------------------------
ROLLBACK;
