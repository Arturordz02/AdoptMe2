# INFORME DE CIERRE TÉCNICO Y FUNCIONAL — FASE C7
## AdoptMe — Plataforma de Adopción Responsable de Mascotas

**Fecha de ejecución:** 10 de noviembre de 2026  
**Repositorio local:** `D:\Escritorio D\T2_GRUPO3_ DESARROLLO_APP\AdoptMe2_Entrega4_Git`  
**Rama activa:** `feature/ux-notificaciones-centros-responsive`  
**HEAD previo:** `ac4d8ca8025bc143201ff84b33e5d7bcf6338e1f`  
**Ambiente de producción:** Render Web API (`https://adoptme2.onrender.com/`)  
**Base de datos:** Neon PostgreSQL 16 (sin migraciones directas en producción)  

---

## 1. RESUMEN EJECUTIVO

Tras la integración local de C6, las validaciones funcionales en emulador Android detectaron discrepancias y flujos inconclusos en solicitudes de adopción, reflejo de suspensión institucional, apelaciones de centros, gestión de publicaciones de centros, bandeja de mensajería del adoptante, marcadores visuales del mapa y accesibilidad en botones de retorno.

La **Fase C7** resolvió de forma integral y robusta la totalidad de estos hallazgos a nivel de arquitectura, contratos de datos, backend ASP.NET Core 8, persistencia local y remota, e interfaz Jetpack Compose, garantizando que tanto el backend como la aplicación Android mantengan una cobertura de pruebas automatizadas impecable y un empaquetado release 100% exitoso.

---

## 2. DETALLE DE CORRECCIONES Y MEJORAS IMPLEMENTADAS

### PARTE A — Solicitudes de Adopción y Estados Terminales
1. **Causa Raíz Diagnosticada y Solucionada:**
   - La acción de rechazar en `MainActivity.kt` modificaba únicamente una copia en memoria y no invocaba `PUT /api/Solicitudes/{id}/estado`. Al realizarse cualquier otra acción en la interfaz, `recargarSolicitudesTrigger` disparaba la recarga completa desde Neon/Render, sobreescribiendo el estado local a "Pendiente" o "En revisión" y volviendo a mostrar los botones.
   - Se conectó la llamada HTTP Retrofit con manejo exhaustivo de códigos de error (400, 401, 403, 404, 409).
2. **Bloqueo de Estados Terminales (409 Conflict):**
   - En `SolicitudesController.cs`, se implementó validación estricta: una solicitud en estado `Aprobada` o `Rechazada` rechaza cualquier modificación subsiguiente retornando `409 Conflict`.
3. **Motivo de Rechazo Obligatorio ($\ge 5$ caracteres):**
   - `TarjetaAdmin.kt` despliega un diálogo modal de confirmación con contador de caracteres en tiempo real. El botón "Confirmar Rechazo" permanece deshabilitado hasta alcanzar al menos 5 caracteres no vacíos.
   - El motivo se persiste en la columna `Observacion` en el backend y se almacena en `AppStorage` y la entidad local `AdoptionRequest`.
   - Si la solicitud ya está rechazada, se ocultan los botones y se renderiza un badge con el motivo registrado.
4. **Autorechazo de Solicitudes Huérfanas:**
   - Al aprobar una solicitud, el backend rechaza automáticamente las demás solicitudes pendientes para la misma mascota con el mensaje explícito: *"Rechazada automáticamente: la mascota fue adoptada a través de otra solicitud aprobada."*.

---

### PARTE B — Suspensión Institucional y Flujo de Apelaciones
1. **Modelo y DTOs de Apelación:**
   - Se crearon las entidades `CentroApelacion`, `CrearApelacionDto`, `ResolverApelacionDto` y `CentroApelacionDto` en el backend.
   - Script SQL idempotente documentado: `Backend/AdoptMe.Api/Data/Neon_UX_Centros_Apelaciones.sql` con banderas de seguridad.
2. **Endpoints en Backend:**
   - `POST /api/centros/mi-centro/apelaciones`: Registro de apelación institucional con motivo obligatorio $\ge 10$ caracteres.
   - `GET /api/centros/mi-centro/apelacion-pendiente`: Consulta reactiva de apelación en trámite.
   - `GET /api/centros/mi-centro/apelaciones`: Historial completo de apelaciones del centro.
   - `GET /api/admin/centros/apelaciones`: Bandeja administrativa de apelaciones.
   - `PUT /api/admin/centros/apelaciones/{id}/aprobar`: Reactivación automática del centro, limpieza de motivo de suspensión y cierre de apelación.
   - `PUT /api/admin/centros/apelaciones/{id}/rechazar`: Rechazo fundamentado ($\ge 5$ caracteres) manteniendo la suspensión operativa.
3. **Interfaz de Centro Suspendido (`PantallaEstadoCentro.kt`):**
   - Banner de alerta visual destacada con motivo y fecha de la suspensión.
   - Desglose de consecuencias operativas (catálogo oculto, edición bloqueada).
   - Botón y modal interactivo para formular la apelación formal.
   - Visualización del estado de la apelación pendiente.
4. **Bandeja de Apelaciones en Panel Admin (`PantallaAdministrador.kt`):**
   - Sub-pestaña "Apelaciones" dentro de la sección de Centros.
   - Tarjetas informativas con datos del albergue, motivo de suspensión original, fecha de apelación y alegato del centro.
   - Modales de resolución para Aprobar (reactivando inmediatamente el centro) o Rechazar (exigiendo fundamento técnico).

---

### PARTE C — Publicaciones del Centro y Modo Solo Lectura
1. **Punto de Entrada Intuitivo:**
   - En `PantallaCentro.kt`, la tarjeta métrica de "Publicaciones" se configuró como interactiva con indicador visual y callback `onVerPublicaciones`.
2. **Navegación y Filtrado:**
   - En `MainActivity.kt`, se enruta a `MisPublicacionesScreen` filtrando estrictamente las mascotas pertenecientes al `centroId` activo o creadas por su correo.
3. **Modo Solo Lectura Institucional:**
   - Cuando un centro se encuentra suspendido, `MisPublicacionesScreen` activa el modo `esModoSoloLectura = true`.
   - Se presenta un banner informativo superior (`⚠️ Funciones de publicación deshabilitadas por suspensión`).
   - Se deshabilitan los botones de "Editar" y "Eliminar", mostrando el candado de seguridad `🔒 Edición y retiro no disponibles por suspensión administrativa`.

---

### PARTE D — Bandeja de Conversaciones del Adoptante
1. **Problema Solucionado:**
   - El icono de Chat en la barra inferior del adoptante abría arbitrariamente el centro con ID 1 en lugar de una bandeja propia.
2. **Nuevo Componente `BandejaConversacionesAdoptante.kt`:**
   - Listado ordenado de conversaciones activas por fecha y hora descendente.
   - Badges reactivos para mensajes no leídos por centro.
   - Vista vacía con mensaje amigable e invitación *"Explorar Centros"* que redirige a la pestaña de albergues.
3. **Persistencia Reactiva en `AppStorage.kt`:**
   - Almacenamiento y recuperación mediante `registrarConversacionAdoptante(...)` y `cargarConversacionesAdoptante(...)`.
   - Actualización sincrónica tanto al iniciar chat desde ficha o mapa como al recibir o enviar mensajes.

---

### PARTE E — Marcadores Personalizados Google Maps por Especie
1. **Iconografía de Alta Fidelidad:**
   - Integración de los recursos entregados en `Android/app/src/main/res/drawable-nodpi/`:
     * `perro_icono.png` (caninos/perros).
     * `gato_icono.png` (felinos/gatos).
     * `conejo_icono.png` (conejos).
     * `ic_marcador_centro.png` (centros de adopción).
2. **Dimensionamiento y Renderizado Eficiente:**
   - Escalado programático seguro a 42dp/44dp mediante canvas en memoria.
   - Caching estricto con `remember(contexto)` para evitar recreación de bitmaps en recomposiciones del mapa.
   - Ocultación garantizada de centros suspendidos o inactivos del mapa público.

---

### PARTE F — Recurso Visual Unificado de Navegación Atrás
1. **Reemplazo Integral:**
   - Se sustituyeron iconos vectoriales genéricos y caracteres de texto plano ("←") por `flecha_izquierda.png`.
2. **Pantallas Actualizadas:**
   - `PantallaChat.kt`
   - `PantallaDetalleMascota.kt`
   - `MapaAdopcionesScreen.kt`
   - `DialogoSelectorMapa.kt`
   - `MisPublicacionesScreen.kt`
3. **Estándares de Accesibilidad:**
   - Target táctil mínimo garantizado de $48 \times 48\,\text{dp}$.
   - Tamaño visual del icono ajustado entre $20\,\text{dp}$ y $24\,\text{dp}$.
   - `contentDescription = "Volver"` en todos los componentes.

---

## 3. RESULTADOS DE PRUEBAS AUTOMATIZADAS Y COMPILACIÓN

### Backend ASP.NET Core 8 (`dotnet test`)
- **Total pruebas ejecutadas:** 135
- **Pruebas aprobadas:** 135 (100%)
- **Pruebas fallidas:** 0
- **Suites nuevas agregadas:**
  * `SolicitudesTerminalesTests.cs` (5 pruebas):
    - Bloqueo 409 Conflict en estados terminales.
    - Validación de motivo de rechazo $\ge 5$ caracteres (400 BadRequest).
    - Persistencia de observación en base de datos.
    - Autorechazo con motivo en cascada de solicitudes huérfanas.
  * `CentrosApelacionesTests.cs` (7 pruebas):
    - Registro de apelación por centro suspendido.
    - Validación de longitud mínima de apelación ($\ge 10$ caracteres).
    - Bloqueo de apelación para centros no suspendidos.
    - Bloqueo 409 Conflict ante apelaciones pendientes preexistentes.
    - Consulta reactiva de apelación pendiente.
    - Aprobación administrativa y reactivación de centro.
    - Rechazo administrativo manteniendo suspensión operativa.

### Android Unit Tests (`testDebugUnitTest` y `testReleaseUnitTest`)
- **Total pruebas ejecutadas:** 254
- **Pruebas aprobadas:** 254 (100%)
- **Pruebas fallidas:** 0
- **Suite nueva agregada:**
  * `CorreccionesC7UnitTest.kt` (11 pruebas):
    - Regla de validación de motivo de rechazo ($\ge 5$ caracteres).
    - Detección de estados terminales para ocultar acciones.
    - Mapeo y persistencia de `observacion` en DTOs y modelos de dominio.
    - Serialización y deserialización de contratos de apelación.
    - Estructura, persistencia y ordenamiento de la bandeja de chat del adoptante.
    - Clasificación y mapeo heurístico de especies a iconos de mapa.
    - Modo solo lectura en publicaciones de centros suspendidos.

### Compilación de Artefacto Release (`assembleRelease`)
- **Resultado Gradle:** `BUILD SUCCESSFUL in 24s`
- **Archivo generado:** `Android/app/build/outputs/apk/release/app-release-unsigned.apk`
- **Tamaño final:** `14,141,955 bytes` (~14.14 MB)
- **Estado ProGuard/R8:** Optimizado y conforme.

---

## 4. REGISTRO DE COMMITS ATÓMICOS EJECUTADOS (C7)

| Orden | Hash | Tipo / Mensaje | Componentes Afectados |
| :---: | :---: | :--- | :--- |
| **1** | `72c1831` | `fix(solicitudes): persistir rechazo y bloquear estados terminales` | `SolicitudesController.cs`, `AdoptionRequest.kt`, `SolicitudMapper.kt`, `TarjetaAdmin.kt` |
| **2** | `c98f738` | `feat(centros): completar suspension, apelaciones y publicaciones` | Modelos, DTOs y controladores de apelación, SQL migración, `MiCentroViewModel`, `AdminCentrosViewModel`, `PantallaEstadoCentro`, `PantallaCentro`, `MisPublicacionesScreen`, `PantallaAdministrador`, `MainActivity.kt` |
| **3** | `1e00a57` | `fix(chat): agregar bandeja de conversaciones por adoptante` | `MensajeChat.kt`, `AppStorage.kt`, `BandejaConversacionesAdoptante.kt`, `PantallaPrincipalAdoptante.kt` |
| **4** | `3898891` | `feat(maps): integrar marcadores personalizados por especie` | `MapaAdopcionesScreen.kt`, iconos en `drawable-nodpi/` |
| **5** | `791d15e` | `style(android): integrar recurso de navegacion atras` | `flecha_izquierda.png`, `PantallaChat.kt`, `PantallaDetalleMascota.kt`, `DialogoSelectorMapa.kt` |
| **6** | `1dae3c9` | `test: cubrir correcciones funcionales C7` | `SolicitudesTerminalesTests.cs`, `CentrosApelacionesTests.cs`, `CorreccionesC7UnitTest.kt` |
| **7** | `26e388a` | `docs: registrar cierre funcional C7` | `docs/CIERRE_C7_CORRECCIONES_FUNCIONALES.md` |

---

## 5. ESTADO DE SEGURIDAD Y PRESERVACIÓN DE STASHES

- **Stashes conservados:**
  * `stash@{0}`: `backup-pre-fixes-c7-2026-11-10`
  * `stash@{1}`: `backup-pre-cleanup-c4-2026-11-10`
  * `stash@{2}`: `backup-pre-identity-c3-2026-11-10`
  * `stash@{3}`: `backup-pre-cleanup-c2-2026-11-10`
  * `stash@{4}`: `backup-pre-audit-c1-2026-11-10`
- **Operaciones de Red/Producción:** Ninguna modificación ejecutada en Neon PostgreSQL de producción ni en Render.
- **Punto de Control:** Se detiene estrictamente la ejecución previo a `git push`, Pull Request, merge en `main` o generación de carpetas limpias externas.
