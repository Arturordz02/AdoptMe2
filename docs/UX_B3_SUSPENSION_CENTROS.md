# INFORME TÉCNICO DE IMPLEMENTACIÓN — FASE UX-B3
## Rechazo Inicial, Suspensión Operativa, Reactivación y Vinculación Responsable de Centros

**Fecha de ejecución:** 10 de noviembre de 2026  
**Proyecto:** AdoptMe — Plataforma de Adopción Responsable de Mascotas  
**Rama activa de trabajo:** `feature/ux-notificaciones-centros-responsive`  
**Base SHA de origen:** `da712812c8cf3d4975bcab671cbfa807cab77057` (`origin/main`)  
**Entorno de datos:** Neon PostgreSQL (Migración de esquema idempotente aplicada)  
**Backend de producción configurado:** `https://adoptme2.onrender.com/` (sin requerir dotnet local ni adb reverse)  

---

### 1. Resumen Ejecutivo de la Fase UX-B3

La Fase UX-B3 culmina el ciclo funcional del sistema de Centros de Adopción, estableciendo una rigurosa separación de responsabilidades y estados entre:
1. **Rechazo de una solicitud inicial de registro:** Ocurre únicamente durante la evaluación inicial de un Centro en estado `Pendiente`. El centro nunca llegó a operar ni a publicar.
2. **Suspensión operativa de un Centro aprobado:** Ocurre sobre un Centro previamente `Aprobado` ante faltas a las normativas de la plataforma o por solicitud preventiva. El estado formal permanece como `Aprobado`, pero su bandera operativa pasa a `Activo = false`, registrando obligatoriamente un `MotivoSuspension` (mínimo 5 caracteres), fecha y administrador responsable. Sus publicaciones previas se pausan y se bloquea la carga de nuevas mascotas tanto a nivel cliente como en servidor previo a cualquier consumo de Cloudinary.
3. **Reactivación de un Centro suspendido:** Permite al administrador reanudar las operaciones institucionales (`Activo = true`), registrando `FechaReactivacion` y conservando la traza histórica de auditoría.
4. **Manejo coherente de Centros inactivos heredados:** Centros en estado `Aprobado` con `Activo = false` pero sin `MotivoSuspension` (estado previo a la migración) se presentan con dignidad como "Centro inactivo", sin inventar motivos ni categorizarlos erróneamente como "Rechazados".
5. **Vinculación responsable de titulares (1:1):** Se estandarizó la terminología de "Asignar" a "Vincular responsable". El sistema valida que el usuario posea rol `Centro`, que no tenga otro centro vinculado (en virtud del índice único de PostgreSQL) y prohíbe explícitamente la sobrescritura no autorizada de centros que ya cuenten con titular asignado.

---

### 2. Máquina de Estados y Matriz de Transiciones

```
                               [ Registro Institucional ]
                                           │
                                           ▼
                                    ┌──────────────┐
                    ┌───────────────┤  PENDIENTE   ├──────────────┐
                    │               └──────────────┘              │
                    │ (Aprobar)                      (Rechazar)   │
                    ▼                                             ▼
          ┌───────────────────┐                         ┌───────────────────┐
          │     APROBADO      │                         │     RECHAZADO     │
          │  (Activo = true)  │                         │ (Activo = false)  │
          └─────────┬─────────┘                         └───────────────────┘
                    │                                             ▲
       (Suspender)  │  ▲ (Reactivar)                              │
                    ▼  │                                          │ (Bloqueado)
          ┌───────────────────┐                                   │
          │     APROBADO      │ ─── [ Intentar Rechazar ] ────────┘
          │ (Activo = false)  │       (400 Bad Request: "Un centro aprobado no
          │ MotivoSuspension  │        puede ser rechazado, debe ser suspendido")
          └───────────────────┘
```

| Estado Actual | Activo | Motivo Suspensión | Categorización UI | Acciones Permitidas (Admin) | Acciones Permitidas (Centro) |
|---|---|---|---|---|---|
| `Pendiente` | `false` | `null` | Solicitud Pendiente | Aprobar, Rechazar (con motivo), Vincular | Esperar revisión, Editar perfil |
| `Rechazado` | `false` | `null` | Solicitud Rechazada | Ninguna de cambio de estado | Editar observaciones, Reenviar solicitud |
| `Aprobado` | `true` | Cursado o `null` | Operativo Activo | Suspender (con motivo), Vincular (si sin titular) | Publicar mascotas, Editar perfil, Gestionar solicitudes |
| `Aprobado` | `false` | Con texto (>= 5 car.) | Centro Suspendido | Reactivar, Vincular (si sin titular) | Ver motivo de suspensión, Actualizar estado |
| `Aprobado` | `false` | `null` o vacío | Centro Inactivo Heredado | Activar / Reactivar, Vincular | Ver estado inactivo, Actualizar estado |

---

### 3. Migración y Auditoría de Esquema en Neon PostgreSQL

Se aplicó el script de migración idempotente `Neon_UX_Centros_Suspension.sql` verificado directamente sobre Neon PostgreSQL:

1. **Columnas agregadas:**
   - `"MotivoSuspension" TEXT NULL`
   - `"FechaSuspension" TIMESTAMPTZ NULL`
   - `"SuspendidoPorAdminId" INT NULL`
   - `"FechaReactivacion" TIMESTAMPTZ NULL`
2. **Claves foráneas y restricciones de integridad:**
   - `FK_Centros_Usuarios_SuspendidoPorAdminId`: `FOREIGN KEY ("SuspendidoPorAdminId") REFERENCES "Usuarios"("Id") ON DELETE SET NULL`
   - `CK_Centros_Suspension_Consistencia`: Asegura que si `MotivoSuspension` o `FechaSuspension` existen, el `Estado` deba ser `'Aprobado'`.
3. **Índice parcial de alto rendimiento:**
   - `IX_Centros_Suspension`: Sobre `("Activo", "FechaSuspension") WHERE ("Activo" = FALSE AND "MotivoSuspension" IS NOT NULL)`.
4. **Verificación de idempotencia:** La reejecución del script en una segunda pasada fue 100% limpia sin errores ni duplicaciones.
5. **Auditoría de integridad post-migración:**
   - Centros totales: **6** (5 demostrativos activos, 1 rechazado ID=10).
   - Mascotas históricas: **18** totalmente intactas (0 eliminadas, 0 huérfanas).
   - Usuarios registrados: **28** totalmente intactos.

---

### 4. Implementación en Backend ASP.NET Core 8

#### Archivos modificados y creados:
- `Backend/AdoptMe.Api/Models/Centro.cs`: Propiedades `MotivoSuspension`, `FechaSuspension`, `SuspendidoPorAdminId`, `SuspendidoPorAdmin`, `FechaReactivacion`.
- `Backend/AdoptMe.Api/Models/SuspenderCentroDto.cs`: Modelo de entrada con `[Required]` y `[MinLength(5)]`.
- `Backend/AdoptMe.Api/Models/CentroPropietarioDto.cs` y `CentroAdminDto.cs`: Mapeo de campos de suspensión y auditoría.
- `Backend/AdoptMe.Api/Data/AdoptMeDbContext.cs`: Configuración Fluent API de la relación opcional con el administrador que ejecutó la suspensión.
- `Backend/AdoptMe.Api/Controllers/AdminCentrosController.cs`:
  - `PUT api/admin/centros/{id}/suspender`: Valida estado `Aprobado`, establece `Activo = false`, registra motivo, fecha y admin autenticado.
  - `PUT api/admin/centros/{id}/reactivar`: Valida estado `Aprobado` e inactividad, reactiva `Activo = true`, actualiza `FechaReactivacion` conservando el motivo histórico para trazabilidad.
  - `PUT api/admin/centros/{id}/rechazar`: Protegido; rechaza con `400 Bad Request` si el centro ya fue `Aprobado` indicando usar `suspender`.
  - `PUT api/admin/centros/{id}/aprobar`: Valida que no provenga de un centro previamente rechazado sin reevaluación.
  - `PUT api/admin/centros/{id}/vincular-responsable` (y alias `asignar-propietario`): Valida que `UsuarioId` no esté ya asignado (retorna `409 Conflict`), verifica rol `Centro` del usuario y garantiza la regla 1:1.
- `Backend/AdoptMe.Api/Controllers/CentrosController.cs`:
  - Mapeo de metadatos de suspensión en `GET api/centros/mi-centro`.
  - Bloqueo con `403 Forbidden` en `PUT api/centros/mi-centro` y `PUT api/centros/mi-centro/logo` si el centro no se encuentra activo.
- `Backend/AdoptMe.Api/Services/CentroAuthorizationService.cs`: Mensaje pedagógico y descriptivo si el centro está inactivo o suspendido.
- `Backend/AdoptMe.Api/Controllers/MascotasController.cs`: Validación estricta que previene la subida a Cloudinary si el centro no está activo y aprobado.

---

### 5. Implementación en Android Kotlin & Jetpack Compose

#### Archivos modificados y creados:
- `Android/app/src/main/java/com/example/adoptme/model/CentroModels.kt`:
  - `CentroPropietarioDto`, `CentroAdminDto`, `EstadoCentroDto`, `OperacionCentroResponse` actualizados con campos de suspensión.
  - Agregado `SuspenderCentroRequest(val motivo: String)`.
- `Android/app/src/main/java/com/example/adoptme/api/CentrosApi.kt`:
  - `@PUT("api/admin/centros/{id}/suspender")`
  - `@PUT("api/admin/centros/{id}/reactivar")`
  - `@PUT("api/admin/centros/{id}/vincular-responsable")`
- `Android/app/src/main/java/com/example/adoptme/viewmodel/AdminCentrosViewModel.kt`:
  - Métodos `suspenderCentro`, `reactivarCentro`, `vincularResponsable`.
  - Validación local de longitud mínima de motivo (>= 5 caracteres).
  - Manejo integral de errores HTTP 401, 403, 404, 409 (conflicto de titularidad).
- `Android/app/src/main/java/com/example/adoptme/screens/PantallaEstadoCentro.kt`:
  - Renderizado diferenciado de `TarjetaEstadoSuspendido` (muestra motivo oficial, fecha de suspensión, aviso de pausa operativa y botones de comprobación/cierre).
  - Renderizado respetuoso de `TarjetaEstadoInactivo` para centros legados.
  - `TarjetaEstadoRechazado` reservada exclusivamente para solicitudes iniciales no aprobadas con opción de subsanar y reenviar.
- `Android/app/src/main/java/com/example/adoptme/screens/PantallaAdministrador.kt`:
  - Pestaña de Centros con botones adaptativos:
    - `Pendiente`: "Aprobar", "Rechazar" (con diálogo de motivo).
    - `Aprobado` Activo: "Suspender" (con diálogo obligatorio de motivo >= 5 caracteres). No muestra "Rechazar".
    - `Aprobado` Inactivo/Suspendido: "Reactivar" (con diálogo de confirmación). No muestra "Rechazar".
    - Vinculación: Botón "Vincular" visible únicamente cuando `usuarioId == null`. Si ya posee titular, muestra sus datos y oculta el botón para evitar sobrescrituras accidentales.

---

### 6. Matriz de Pruebas y Resultados de Compilación

```
===================================================================================
SUITE DE PRUEBAS AUTOMATIZADAS — RESUMEN TOTAL DE EJECUCIÓN
===================================================================================
  1. Backend ASP.NET Core 8:
     - Pruebas unitarias totales: 116 (84 heredadas + 32 de suspensión y vinculación)
     - Fallos: 0 | Errores: 0 | Omitidas: 0 | Duración: 860 ms
     - Estado: PASSED (100%)

  2. Android Kotlin Unit Tests (Debug):
     - Pruebas unitarias totales: 243 (222 heredadas + 21 de suspensión y vinculación)
     - Fallos: 0 | Errores: 0 | Omitidas: 0 | Duración: 32 s
     - Estado: PASSED (100%)

  3. Android Kotlin Unit Tests (Release):
     - Pruebas unitarias totales: 243
     - Fallos: 0 | Errores: 0 | Omitidas: 0 | Duración: 57 s
     - Estado: PASSED (100%)

  4. Ensamblado de APK Release:
     - Tarea Gradle: assembleRelease
     - Artefacto generado: app-release-unsigned.apk (13.89 MB)
     - Estado: BUILD SUCCESSFUL (100%)
===================================================================================
```

#### Detalle de las 32 pruebas Backend (`CentrosSuspensionTests.cs`):
1. `Test01_Admin_SuspenderCentro_AprobadoActivo_SuspendeExitosamente`
2. `Test02_Admin_SuspenderCentro_GuardaMotivoYFechaCorrectos`
3. `Test03_Admin_SuspenderCentro_RegistraAdminIdAutenticado`
4. `Test04_Admin_SuspenderCentro_MotivoMenorA5Chars_RetornaBadRequest`
5. `Test05_Admin_SuspenderCentro_NoAdmin_FallaEnAutorizacion`
6. `Test06_Admin_SuspenderCentro_CentroInexistente_RetornaNotFound`
7. `Test07_Admin_SuspenderCentro_CentroPendiente_RetornaBadRequest`
8. `Test08_Admin_SuspenderCentro_CentroRechazado_RetornaBadRequest`
9. `Test09_Admin_SuspenderCentro_CentroYaSuspendido_ActualizaMotivoYFecha`
10. `Test10_Admin_ReactivarCentro_Suspendido_ReactivaExitosamente`
11. `Test11_Admin_ReactivarCentro_EstableceFechaReactivacion`
12. `Test12_Admin_ReactivarCentro_ConservaMotivoHistorico`
13. `Test13_Admin_ReactivarCentro_CentroYaActivo_RetornaBadRequest`
14. `Test14_Admin_ReactivarCentro_CentroInexistente_RetornaNotFound`
15. `Test15_Admin_ReactivarCentro_CentroPendiente_RetornaBadRequest`
16. `Test16_Admin_ReactivarCentro_CentroRechazado_RetornaBadRequest`
17. `Test17_Admin_RechazarCentro_Aprobado_RetornaBadRequest_DebeSuspender`
18. `Test18_Admin_RechazarCentro_Pendiente_RechazaCorrectamente`
19. `Test19_Admin_AprobarCentro_RechazadoDirecto_RetornaBadRequest`
20. `Test20_Admin_VincularResponsable_CentroSinResponsable_VinculaExitosamente`
21. `Test21_Admin_VincularResponsable_UsuarioYaVinculadoAOtroCentro_RetornaConflict`
22. `Test22_Admin_VincularResponsable_UsuarioRolAdoptante_RetornaBadRequest`
23. `Test23_Admin_VincularResponsable_UsuarioInexistente_RetornaNotFound`
24. `Test24_Admin_VincularResponsable_CentroInexistente_RetornaNotFound`
25. `Test25_Admin_VincularResponsable_CentroYaTieneResponsable_RetornaConflict`
26. `Test26_Centro_ObtenerMiCentro_Suspendido_RetornaMotivoYFechaSuspension`
27. `Test27_Centro_ActualizarMiCentro_Suspendido_RetornaForbidden`
28. `Test28_Centro_ActualizarLogo_Suspendido_RetornaForbidden`
29. `Test29_Mascotas_PublicarMascota_CentroSuspendido_RetornaForbidden`
30. `Test30_Mascotas_PublicarMascotaMultipart_CentroSuspendido_NoSubeACloudinary`
31. `Test31_Publico_ListarCentros_CentroSuspendido_NoApareceEnCatalogo`
32. `Test32_Publico_ListarCentros_DemostrativosActivos_AparecenCompletos`

#### Detalle de las 21 pruebas Android (`SuspensionYVinculacionCentroTest.kt`):
1. `test01_CentroPropietarioDto_DeserializaCamposSuspensionCorrectamente`
2. `test02_CentroAdminDto_DeserializaAuditoriaSuspension`
3. `test03_EstadoCentroDto_DeserializaCamposSuspension`
4. `test04_SuspenderCentroRequest_SerializaCorrectamente`
5. `test05_OperacionCentroResponse_DeserializaCamposSuspension`
6. `test06_EstadoCentro_IdentificaAprobadoActivo`
7. `test07_EstadoCentro_IdentificaAprobadoSuspendidoConMotivo`
8. `test08_EstadoCentro_IdentificaAprobadoInactivoHeredadoSinMotivo`
9. `test09_EstadoCentro_IdentificaRechazadoInicial`
10. `test10_EstadoCentro_IdentificaPendiente`
11. `test11_AdminCentrosViewModel_SuspenderCentro_MotivoMenorA5Chars_RetornaErrorSinLlamarApi`
12. `test12_AdminCentrosViewModel_SuspenderCentro_Valido_PasaValidacionLocal`
13. `test13_AdminCentrosViewModel_ReactivarCentro_EstructuraCorrecta`
14. `test14_AdminCentrosViewModel_VincularResponsable_EstructuraCorrecta`
15. `test15_AdminCentrosViewModel_ProcesarErrorHttp409_RetornaMensajeConflictoClaro`
16. `test16_AdminCentrosViewModel_ProcesarErrorHttp403_RetornaMensajePermisoAdmin`
17. `test17_AdminCentrosViewModel_ProcesarErrorHttp404_RetornaMensajeCentroNoEncontrado`
18. `test18_ReglaNegocio_CentroSuspendido_NoEsRechazado`
19. `test19_ReglaNegocio_CentroAprobado_NoMuestraBotonRechazarEnAdmin`
20. `test20_ReglaNegocio_CentroConResponsable_NoMuestraBotonVincular`
21. `test21_ReglaNegocio_CentroSuspendido_NoPermiteReenviarSolicitud`

---

### 7. Seguridad, Integridad y Control de Versiones

- `git diff --check`: Verificado sin espacios en blanco ni conflictos de formato.
- Cero claves, secretos o cadenas de conexión expuestas.
- URL base predeterminada: `https://adoptme2.onrender.com/`.
- **Alto obligatorio cumplido:** No se ha realizado `git add`, `git commit`, `git push`, `gh pr create` ni despliegue manual a Render.
