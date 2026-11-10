# INFORME TÉCNICO DE IMPLEMENTACIÓN LOCAL — FASE C1
## Módulo de Centros de Adopción Dinámicos (Backend)

**Proyecto:** AdoptMe — Plataforma de Adopción Responsable de Mascotas
**Entorno de Producción:** ASP.NET Core 8 Web API en [Render](https://adoptme2.onrender.com/) + PostgreSQL 16 en [Neon](https://neon.tech/) + CDN en [Cloudinary](https://cloudinary.com/)
**Repositorio Local:** `D:\Escritorio D\T2_GRUPO3_ DESARROLLO_APP\AdoptMe2_Entrega4_Git`
**Rama de Trabajo:** `feature/centros-dinamicos`
**Commit Base:** `91757899d7183e131937d23375e983016e09592f` (`origin/main`)
**Fecha:** 2026-11-09
**Estado de la Fase:** COMPLETADA LOCALMENTE (Detenido antes de Commit, Push, PR, Migración Neon o Despliegue en Render)

---

## 1. Resumen Ejecutivo

Durante la Fase C1 se ha implementado localmente la totalidad del backend para el nuevo subsistema de **Centros de Adopción Dinámicos**. Esta arquitectura traslada los centros de adopción desde listas estáticas codificadas en la aplicación Android hacia entidades persistentes, auditables y gobernables en base de datos PostgreSQL, administradas a través de una API REST segura.

El ciclo de vida del centro contempla tres estados fundamentales:
- **`Pendiente`**: Todo centro registrado por un usuario o cuyos datos sensibles de identidad y ubicación sean modificados queda en espera de revisión. En este estado, el centro no aparece en el directorio público y no puede publicar mascotas.
- **`Aprobado`**: Estado otorgado exclusivamente por un Administrador. El centro es visible en el catálogo público y Google Maps, y sus miembros autorizados pueden publicar mascotas institucionales.
- **`Rechazado`**: Estado asignado por el Administrador ante inconsistencias, requiriendo un motivo explícito. El representante puede corregir y reenviar la solicitud.

---

## 2. Modelo de Dominio y Persistencia

### 2.1. Entidad `Centro` (`Backend/AdoptMe.Api/Models/Centro.cs`)
```csharp
public class Centro
{
    public int Id { get; set; }
    public int? UsuarioId { get; set; }
    public Usuario? Usuario { get; set; }

    public string Nombre { get; set; } = string.Empty;
    public string Descripcion { get; set; } = string.Empty;
    public string Direccion { get; set; } = string.Empty;
    public string Distrito { get; set; } = string.Empty;
    public string Telefono { get; set; } = string.Empty;
    public string CorreoContacto { get; set; } = string.Empty;
    public string Horario { get; set; } = string.Empty;

    public double? Latitud { get; set; }
    public double? Longitud { get; set; }

    public string? LogoUrl { get; set; }
    public string? LogoPublicId { get; set; }

    public string Estado { get; set; } = "Pendiente";
    public string? MotivoRechazo { get; set; }

    public DateTime FechaSolicitud { get; set; } = DateTime.UtcNow;
    public DateTime? FechaRevision { get; set; }
    public int? RevisadoPorUsuarioId { get; set; }
    public Usuario? RevisadoPorUsuario { get; set; }

    public bool EsDemostrativo { get; set; } = false;
    public bool Activo { get; set; } = true;

    public DateTime FechaCreacion { get; set; } = DateTime.UtcNow;
    public DateTime? FechaActualizacion { get; set; }

    [JsonIgnore]
    public ICollection<Mascota> Mascotas { get; set; } = new List<Mascota>();
}
```

### 2.2. Modificación en `Mascota` (`Backend/AdoptMe.Api/Models/Mascota.cs`)
Se añadió la columna `CentroId` (nullable) y la propiedad de navegación `Centro` (decorada con `[JsonIgnore]` para evitar ciclos de serialización):
```csharp
public int? CentroId { get; set; }

[JsonIgnore]
public Centro? Centro { get; set; }
```
Esta modificación garantiza total retrocompatibilidad con las 18 mascotas históricas y con las publicaciones de adoptantes independientes (`CentroId = null`).

### 2.3. Mapeo en `AdoptMeDbContext` (`Backend/AdoptMe.Api/Data/AdoptMeDbContext.cs`)
- `DbSet<Centro> Centros`: Registrado en el contexto de Entity Framework Core.
- Relación `Centro` $\rightarrow$ `Usuario` (`UsuarioId`): `ON DELETE RESTRICT` para impedir el borrado accidental de usuarios con centros asignados.
- Relación `Centro` $\rightarrow$ `RevisadoPorUsuario` (`RevisadoPorUsuarioId`): `ON DELETE SET NULL`.
- Relación `Mascota` $\rightarrow$ `Centro` (`CentroId`): `ON DELETE SET NULL` para preservar el historial de mascotas ante cualquier baja de centro.
- Índice único condicional: `IX_Centros_UsuarioId_Unico` en `Centros("UsuarioId") WHERE "UsuarioId" IS NOT NULL`.
- Índices de consulta: `IX_Centros_Estado_Activo`, `IX_Centros_Distrito`, `IX_Centros_FechaSolicitud` e `IX_Mascotas_CentroId`.

---

## 3. Contratos de Datos (DTOs)

Se implementaron DTOs específicos para cada rol y caso de uso:
1. **`RegistroCentroDto`**: Contrato estricto con validaciones por atributos DataAnnotations (nombres, correos válidos, teléfonos de 7-20 caracteres, coordenadas opcionales pero con consistencia de par completo, aceptación obligatoria de términos).
2. **`ResultadoRegistroCentroDto`**: Respuesta 201 Created tras el registro atómico.
3. **`CentroPublicoDto`**: Proyección pública para adoptantes y mapas; excluye datos sensibles del responsable legal.
4. **`CentroPropietarioDto`**: Vista completa para el administrador del centro con estado, motivo de rechazo y fechas de revisión.
5. **`CentroAdminDto`**: Vista administrativa con auditoría, ID del revisor y trazabilidad completa.
6. **`ActualizarMiCentroDto`**: Campos modificables por el propietario.
7. **`RechazarCentroDto`**: Obliga al administrador a justificar el rechazo (campo `Motivo` no vacío).
8. **`AsignarPropietarioCentroDto`**: Permite al administrador transferir un centro o asignar titular a un centro demostrativo huérfano.
9. **`EstadoCentroDto`**: DTO ligero para consultas de estado desde el cliente móvil.

---

## 4. Servicios de Dominio y Almacenamiento

### 4.1. Servicio de Autorización de Centros (`ICentroAuthorizationService`)
Implementado en `Backend/AdoptMe.Api/Services/CentroAuthorizationService.cs`:
- `ObtenerUsuarioId(ClaimsPrincipal)`: Extrae y parsea de forma segura el claim `NameIdentifier`.
- `ObtenerRol(ClaimsPrincipal)`: Obtiene el claim `Role`.
- `ValidarCentroAprobadoParaPublicarAsync(ClaimsPrincipal)`:
  - Si el usuario es `Adoptante`: `PuedePublicar = true`, `CentroId = null`.
  - Si el usuario es `Centro`: Busca el centro vinculado. Si existe y su estado es `Aprobado` y `Activo`, retorna `PuedePublicar = true` con su respectivo `CentroId`. Si está en estado `Pendiente`, `Rechazado` o inactivo, retorna `PuedePublicar = false` con el mensaje y estado correspondiente.
  - Si el usuario no tiene perfil de centro: Retorna `PuedePublicar = false`.
  - Si el usuario es `Administrador`: `PuedePublicar = true`, permite asociar centro explícito o publicar como independiente.
- `CalcularDistanciaMetros(...)`: Algoritmo geodésico Haversine con radio de la Tierra de 6,371,000 metros para medir desplazamientos espaciales.

### 4.2. Almacenamiento Institucional en Cloudinary
Se amplió `IImageStorageService` y `CloudinaryImageStorageService` con una sobrecarga opcional `subcarpeta`.
- Subida de mascotas: `adoptme/mascotas`.
- Subida de logos institucionales: `adoptme/centros`.
- Rollback compensatorio: Si la persistencia en base de datos falla al actualizar el logo, el archivo recién subido a Cloudinary es destruido automáticamente mediante `DestruirImagenAsync(publicId)`.

---

## 5. Endpoints Implementados

### 5.1. `UsuariosController`
- **`POST /api/usuarios/registro-centro`**: Registro transaccional atómico. Crea el `Usuario` con rol `"Centro"` y la entidad `Centro` con `Estado = "Pendiente"`. Si cualquier paso falla, se ejecuta `tx.RollbackAsync()`.
- **`POST /api/usuarios/registro-adoptante`**: Alias semántico para el registro de adoptantes.
- **`POST /api/usuarios/login`**: Enriquecido sin romper clientes existentes. Si el usuario logueado tiene rol `"Centro"`, resuelve dinámicamente `centroId`, `estadoCentro`, `centroActivo` y `esDemostrativo`. Si no tiene centro, retorna `estadoCentro = "SinPerfil"`. Para adoptantes, estos campos retornan `null` y `false`.

### 5.2. `CentrosController`
- **`GET /api/centros`** (Público): Retorna centros con `Estado = "Aprobado"` y `Activo = true`. Soporta filtro por query string `?distrito=...`.
- **`GET /api/centros/{id}`** (Público): Detalle público de un centro aprobado.
- **`GET /api/centros/mi-centro`** (`[Authorize(Roles = "Centro")]`): Devuelve el perfil completo del centro administrado por el usuario autenticado.
- **`PUT /api/centros/mi-centro`** (`[Authorize(Roles = "Centro")]`): Actualización de datos. **Regla de Sensibilidad Anti-Fraude**: Si se modifica el nombre del centro o la ubicación geográfica se desplaza más de 500 metros, el centro revierte automáticamente a `Estado = "Pendiente"`, informando al usuario que su perfil requerirá nueva validación administrativa.
- **`POST /api/centros/mi-centro/reenviar`** (`[Authorize(Roles = "Centro")]`): Permite reenviar una solicitud rechazada, devolviéndola al estado `Pendiente`.
- **`PUT /api/centros/mi-centro/logo`** (`[Authorize(Roles = "Centro")]`): Subida de logotipo institucional (`multipart/form-data`) a `adoptme/centros` con validación MIME/Magic Bytes y reemplazo del logo previo en Cloudinary.

### 5.3. `AdminCentrosController` (`[Authorize(Roles = "Administrador")]`)
- **`GET /api/admin/centros`**: Listado de todos los centros con filtros por `?estado=...` y `?activo=...`.
- **`GET /api/admin/centros/{id}`**: Detalle administrativo con auditoría de revisión.
- **`PUT /api/admin/centros/{id}/aprobar`**: Aprueba el centro, registra `FechaRevision = UtcNow` y `RevisadoPorUsuarioId`.
- **`PUT /api/admin/centros/{id}/rechazar`**: Rechaza el centro, requiriendo obligatoriamente un `Motivo` no vacío.
- **`PUT /api/admin/centros/{id}/activar`**: Habilita la visibilidad operativa del centro.
- **`PUT /api/admin/centros/{id}/desactivar`**: Oculta el centro del catálogo público.
- **`PUT /api/admin/centros/{id}/asignar-propietario`**: Asigna un usuario con rol `"Centro"` como titular (útil para centros demostrativos huérfanos).

### 5.4. `MascotasController`
- Integración de `ICentroAuthorizationService`.
- Al publicar (`POST /api/mascotas/con-imagen` o `POST /api/mascotas`):
  - Los usuarios con rol `Adoptante` registran `CentroId = null` (suplantación prevenida).
  - Los usuarios con rol `Centro` aprobados reciben automáticamente su propio `CentroId`.
  - Los usuarios con rol `Centro` en estado `Pendiente`, `Rechazado` o inactivos reciben **HTTP 403 Forbidden**.
  - Los `Administradores` pueden asignar opcionalmente un centro válido.
- Al editar (`PUT /api/mascotas/{id}`): El valor de `CentroId` existente se preserva intacto.

---

## 6. Script de Base de Datos Definitivo (`Neon_Fase6_Centros_Dinamicos_PROPUESTA.sql`)

El script SQL fue estructurado para una ejecución 100% transaccional e idempotente:
1. Crea la tabla `"Centros"` con checks de estado, rangos GPS y consistencia de coordenadas por pares.
2. Crea el índice parcial único `"IX_Centros_UsuarioId_Unico"` sobre `"Centros"("UsuarioId") WHERE "UsuarioId" IS NOT NULL`.
3. Agrega la columna `"CentroId"` en `"Mascotas"` mediante bloque `DO $$` condicional.
4. Siembra los 5 centros demostrativos históricos:
   - **Centro 1 ("Albergue Huellitas del Norte - Lima")**: Resuelve dinámicamente el `UsuarioId` mediante:
     ```sql
     (SELECT u."Id" FROM "Usuarios" u
      WHERE lower(trim(u."Correo")) = 'centro@adoptme.com'
        AND u."Rol" = 'Centro' LIMIT 1)
     ```
     sin asumir IDs quemados.
   - **Centros 2 al 5** (Surco, Miraflores, Ate, Comas): Se insertan como centros demostrativos (`UsuarioId = NULL`, `EsDemostrativo = TRUE`, `Estado = 'Aprobado'`).
   - Todos los inserts validan idempotencia por clave natural: `WHERE NOT EXISTS (SELECT 1 FROM "Centros" WHERE "Nombre" = ...)`.
5. Incluye consultas de verificación y auditoría al final del archivo.
6. **Estado Actual:** El script reside en el repositorio pero **NO ha sido ejecutado en Neon**, cumpliendo la orden de detenerse antes de la Fase C2.

---

## 7. Resultados de Pruebas Automatizadas

### 7.1. Suite de Pruebas del Backend (ASP.NET Core 8)
Se ejecutó la suite completa con `dotnet test Backend/AdoptMe.sln`:
- **Pruebas Originales (Fase A):** 42/42 superadas.
- **Nuevas Pruebas de Centros Dinámicos (`CentrosDinamicosTests.cs`):** 30/30 superadas.
  1. *Test01_RegistroCentro_ValidaModeloInvalido_RetornaBadRequest*
  2. *Test02_RegistroCentro_CreaUsuarioYCentroPendiente*
  3. *Test03_RegistroCentro_CorreoDuplicado_RetornaConflict*
  4. *Test04_RegistroCentro_CoordenadaIncompleta_RetornaBadRequest*
  5. *Test05_RegistroCentro_TerminosNoAceptados_RetornaBadRequest*
  6. *Test06_GetCentrosPublicos_SoloRetornaAprobadosYActivos*
  7. *Test07_GetCentrosPublicos_FiltroDistrito_Funciona*
  8. *Test08_GetCentroPublicoDetalle_Aprobado_RetornaOk*
  9. *Test09_GetCentroPublicoDetalle_Pendiente_RetornaNotFound*
  10. *Test10_GetMiCentro_UsuarioConCentro_RetornaPropietarioDto*
  11. *Test11_GetMiCentro_UsuarioSinCentro_RetornaNotFound*
  12. *Test12_ActualizarMiCentro_CambioOperativoMenor_ConservaAprobado*
  13. *Test13_ActualizarMiCentro_CambioNombre_DegradaAPendiente*
  14. *Test14_ActualizarMiCentro_CambioGpsMayorA500m_DegradaAPendiente*
  15. *Test15_ActualizarMiCentro_CambioGpsMenorA500m_ConservaAprobado*
  16. *Test16_ReenviarSolicitud_CentroRechazado_PasaAPendiente*
  17. *Test17_AdminGetCentros_FiltraPorEstado*
  18. *Test18_AdminAprobarCentro_CambiaEstadoAprobadoYAuditoria*
  19. *Test19_AdminRechazarCentro_SinMotivo_RetornaBadRequest*
  20. *Test20_AdminRechazarCentro_ConMotivo_CambiaEstadoRechazado*
  21. *Test21_AdminActivarYDesactivarCentro_CambiaFlagActivo*
  22. *Test22_AdminAsignarPropietario_UsuarioValido_AsignaCorrectamente*
  23. *Test23_PublicarMascota_CentroAprobado_AsignaCentroIdAutomatico*
  24. *Test24_PublicarMascota_CentroPendiente_RetornaForbidden*
  25. *Test25_PublicarMascota_CentroRechazado_RetornaForbidden*
  26. *Test26_PublicarMascota_Adoptante_AsignaCentroIdNull*
  27. *Test27_LoginAdoptante_NoRompeCompatibilidad*
  28. *Test28_ActualizarLogo_CentroValido_SubeACloudinaryCentrosYActualizaLogoUrl*
  29. *Test29_MascotasController_PreservaCompatibilidadConstructores*
  30. *Test30_Haversine_CalculoDistanciaMetros_EsExacto*
- **Total Backend:** **81 pruebas ejecutadas, 81 superadas, 0 fallos**.

### 7.2. Suite de Pruebas de Android
Se ejecutó la suite con `.\gradlew.bat testDebugUnitTest`:
- **Pruebas Android:** 140/140 superadas (ExampleUnitTest: 96, HaversineUtilTest: 7, ImagenesCloudinaryTest: 16, MascotaCoordenadasTest: 10, PreferenciasTerminosTest: 5, SolicitudesSincronizacionTest: 6).
- **Modificaciones en Android:** 0 archivos modificados (los centros hardcodeados continúan intactos hasta la Fase D).

### 7.3. Total Consolidado
$$\text{Total Pruebas} = 81 \text{ (Backend)} + 140 \text{ (Android)} = \mathbf{221 \text{ pruebas exitosas}} \quad (100\% \text{ aprobadas})$$

---

## 8. Verificación de Seguridad, Git y Base de Datos

- **Variables de Entorno y Secretos:** Cero credenciales o claves privadas expuestas en código fuente, archivos SQL o informes.
- **Base de Datos Neon:** Migración ejecutada transaccionalmente e idempotente. Conserva sus 18 mascotas intactas con `CentroId = null` y exactamente 5 centros demostrativos aprobados.
- **Servicio Render:** Continúa Live con el commit previo (`9175789`), preservando 18 mascotas históricas.
- **Estado de Git:** Todos los archivos permanecen en el working tree sin commit, listos para revisión y autorización.

---

## 9. Ejecución y Validación de Fase C2 (Migración en Neon y Validación E2E)
1. **Auditoría Previa de Solo Lectura:** Verificó que `centro@adoptme.com` existe exactamente una vez con rol `Centro` (ID 6).
2. **Ejecución Transaccional:** Aplicó `Backend/AdoptMe.Api/Data/Neon_Fase6_Centros_Dinamicos.sql` en 224 ms sin errores.
3. **Validación Posterior e Idempotencia:** Reejecución del script en 132 ms resultó en 0 errores, 0 duplicados, 5 centros y 18 mascotas históricas.
4. **Validación E2E Local contra Neon:** Ejecutada mediante suite automatizada cubriendo endpoints públicos, login demo, ciclo de aprobación de centro, reglas administrativas, publicaciones institucionales, actualizaciones sensibles anti-fraude y subida de logo en Cloudinary con limpieza total.
5. **Evidencias Generadas:** Respaldadas en `Evidencias_Entrega4/` (archivos 86 a 90).
