# FASE UX-B2 — INFORME DE GEOLOCALIZACIÓN INTELIGENTE, REDISEÑO DEL PANEL DEL CENTRO, RESPONSIVE, CONTRASTE Y TAXONOMÍA DE CONEXIÓN

**Proyecto:** AdoptMe — Plataforma de Adopción Responsable  
**Rama activa:** `feature/ux-notificaciones-centros-responsive`  
**Base origin/main:** `da712812c8cf3d4975bcab671cbfa807cab77057`  
**Entorno predeterminado:** Render (`https://adoptme2.onrender.com/`)  
**Fecha de ejecución:** 10 de Noviembre de 2026  

---

## 1. RESUMEN EJECUTIVO

La **Fase UX-B2** consolida la experiencia visual, operativa y técnica de la aplicación AdoptMe tras la implementación de notificaciones y conteo de mensajes no leídos (UX-B1). Esta fase resuelve las fricciones críticas identificadas en la auditoría UX-A respecto a:
1. **Geolocalización inversa inteligente:** Resolución automática de calle/número y distrito al tocar el mapa interactivo o activar el GPS, eliminando la duplicación de tokens ("Lima, Lima, Lima") y respetando las ediciones manuales del usuario.
2. **Jerarquía visual clara de ubicación:** Dirección física como dato primario destacado, distrito como dato secundario agrupador y coordenadas numéricas como metadato técnico no invasivo.
3. **Rediseño completo del Panel Operativo del Centro (`PantallaCentro.kt`):**
   - Cabecera institucional completa con logotipo remoto/local, badges oficiales (*✓ Verificado*, *Activo*, *Demostrativo*), dirección física, distrito, horario de atención y canales de contacto directo.
   - Cuadrícula 2x2 de métricas operativas clave (*Publicaciones*, *Disponibles*, *Conversaciones*, *No leídos* con alerta visual).
   - Acciones operativas responsive adaptativas que previenen el desbordamiento o compresión excesiva en pantallas estrechas (320–360 dp) o con escalado tipográfico alto (hasta 1.5x).
   - Integración del selector de mapa en el diálogo de edición institucional con detección de modificaciones sensibles (> 500m).
4. **Taxonomía semántica de errores de conexión en `RetrofitClient.kt`:** Mensajes pedagógicos diferenciados para caídas de internet (`UnknownHostException`), cold start/timeout de Render (HTTP 502/503/504, `SocketTimeoutException`), credenciales inválidas (401), acceso denegado (403) y fallos de servidor (500).
5. **Accesibilidad, contraste y adaptabilidad responsive:** Migración a tokens semánticos de `MaterialTheme.colorScheme` eliminando contrastes grises defectuosos y garantizando soporte de teclado virtual (`imePadding`) y pantallas desde 320x568 dp hasta tablets medianas.

---

## 2. ARQUITECTURA DE GEOLOCALIZACIÓN INTELIGENTE

### 2.1 Componente `ReverseGeocodingHelper.kt`
Ubicado en `com.example.adoptme.utils`, gestiona la resolución asíncrona mediante corrutinas en `Dispatchers.IO` y provee algoritmos puros para formatear componentes geográficos peruanos.

- **Compatibilidad API dual:** Utiliza el callback nativo `Geocoder.GeocodeListener` en Android 13+ (API 33 Tiramisu) y la invocación síncrona desacoplada en versiones anteriores.
- **Cadena de fallback de distrito:** Evalúa ordenadamente `subLocality` (distrito formal en Lima) → `locality` (ciudad o distrito alternativo) → `subAdminArea` (provincia) → `adminArea` (departamento).
- **Construcción de dirección física:** Combina calle (`thoroughfare`) y numeración (`subThoroughfare`), o nombre de referencia (`featureName` si no es una coordenada numérica), evitando concatenar números si ya están presentes en el nombre de la vía.
- **Filtro antiduplicados (`limpiarDuplicados`):** Normaliza cadenas para erradicar repeticiones sucesivas causadas por datos administrativos redundantes del proveedor de mapas.
- **Preservación inmutable de coordenadas:** Ante ausencia de señal, fallo de red (`IOException`) o falla del servicio `Geocoder`, el helper genera un `resultadoFallback` que preserva intactas las coordenadas (`latitud`, `longitud`), permitiendo al usuario continuar y escribir manualmente su dirección.

### 2.2 Integración en `DialogoSelectorMapa.kt`
- Incorpora `LaunchedEffect(puntoSeleccionado)` que activa `ReverseGeocodingHelper.obtenerDireccion`.
- Muestra feedback visual inmediato: indicador de carga *"Consultando dirección física..."* mientras se resuelve y una tarjeta con la dirección y distrito detectados.
- Expone callback retrocompatible `alConfirmar(lat, lon, distrito)` y el nuevo callback enriquecido `alConfirmarDetallado(lat, lon, resultado: ResultadoGeocodificacion)`.

---

## 3. INTEGRACIÓN EN FORMULARIOS Y FLUJOS OPERATIVOS

| Pantalla / Componente | Integración de Geolocalización | Jerarquía Visual Implementada | Comportamiento ante Edición Manual |
| :--- | :--- | :--- | :--- |
| **`PantallaRegistro.kt`** (Registro de Centro) | Selector de mapa con callback detallado que autocompleta `direccionCentro` y `distritoCentro`. | Tarjeta con estado de coordenadas fijadas; campos editables de Dirección física (principal) y Distrito (secundario). | El usuario puede modificar el texto libremente; las recomposiciones no sobreescriben los campos. |
| **`PantallaCentro.kt`** (`DialogoEditarMiCentroConSensibilidad`) | Botón interactivo *"📍 Ubicar en mapa interactivo"* que abre el selector y recalcula si el desplazamiento excede 500 metros. | Muestra coordenadas formateadas como dato técnico secundario bajo los campos de dirección. | Los cambios manuales se respetan y activan la advertencia de paso a revisión pendiente si superan el umbral. |
| **`PantallaPublicarMascota.kt`** | Al presionar GPS o seleccionar mapa, resuelve automáticamente la dirección y distrito en el campo `ubicacion`. | Tarjeta con coordenadas fijadas y botón rápido para quitar o cambiar punto. | Conserva el texto si el usuario ajusta el distrito o agrega referencias de entrega. |
| **`MisPublicacionesScreen.kt`** (`DialogoEditarMascota`) | Añadido selector de mapa con autocompletado y persistencia de `latitud` y `longitud` en el modelo `Pet`. | Muestra coordenadas de la publicación existente y botón *"Cambiar en mapa"*. | Permite editar tanto el texto descriptivo de ubicación como el punto geográfico. |

---

## 4. REDISEÑO DEL PANEL OPERATIVO DEL CENTRO (`PantallaCentro.kt`)

### 4.1 Cabecera Institucional
- **Identidad visual:** Avatar circular con logotipo remoto (cargado vía Coil con placeholder reactivo) o disparador de captura con cámara / selector de galería seguro.
- **Badges de confianza:**
  - `✓ Verificado` (verde esmeralda, visible si estado es *Aprobado*).
  - `Activo (ID #X)` (teal institucional, mostrando el ID real de persistencia).
  - `Demostrativo` (naranja cálido, cuando aplica).
- **Datos institucionales:** Dirección física completa, distrito, horario de atención y teléfonos de contacto agrupados en tipografía legible.

### 4.2 Cuadrícula 2x2 de Métricas
Cuatro tarjetas operativas optimizadas (`TarjetaMetricaCentro`) que brindan visibilidad ejecutiva al responsable:
1. **Publicaciones:** Conteo total de mascotas creadas por este centro.
2. **Disponibles:** Cantidad de mascotas con estado *"Disponible"*.
3. **Conversaciones:** Total de chats activos iniciados por adoptantes hacia este centro.
4. **No leídos:** Contador de mensajes pendientes de lectura (con contenedor de advertencia si `totalNoLeidosCentro > 0` y tope visual `99+`).

### 4.3 Acciones Operativas Adaptativas
- Implementación de `BoxWithConstraints`:
  - **Ancho < 360 dp (pantallas angostas / font scaling elevado):** Disposición en columna con *"Publicar mascota"* como botón principal prominente a todo lo ancho, seguido de fila secundaria compacta (*Editar*, *Logo*, *Salir*).
  - **Ancho >= 360 dp (pantallas estándar y medianas):** Fila equilibrada con pesos proporcionales (*Editar perfil*, *Cambiar logo*, *Publicar mascota*), evitando filas sobrecargadas que trunquen etiquetas.

---

## 5. TAXONOMÍA SEMÁNTICA DE ERRORES DE RED (`RetrofitClient.kt`)

Se estructuró la enumeración `CategoriaErrorConexion` y las funciones `clasificarError` y `resolverMensajeErrorServidor`:

| Categoría | Excepciones / Códigos HTTP | Mensaje al Usuario | Comportamiento Sugerido |
| :--- | :--- | :--- | :--- |
| **`SIN_INTERNET`** | `UnknownHostException` | *"No hay conexión a Internet. Revisa tu red e inténtalo nuevamente."* | Notificación clara sin reintento automático infinito. |
| **`TIMEOUT_O_COLD_START`** | `SocketTimeoutException`, `TimeoutException`, `ConnectException`, HTTP 502, 503, 504 | *"El servidor está iniciando. Intenta nuevamente en unos segundos."* | Informa amigablemente el despertar de la instancia gratuita en Render. |
| **`AUTORIZACION`** | HTTP 401 | *"El correo o la contraseña no son correctos o tu sesión ha expirado."* | Invita al usuario a reingresar sus credenciales. |
| **`PROHIBIDO`** | HTTP 403 | *"Acceso denegado. No tienes autorización para realizar esta acción."* | Explica restricciones institucionales o centro inactivo. |
| **`ERROR_SERVIDOR`** | HTTP 500, 5xx | *"Ocurrió un problema temporal en el servidor. Inténtalo nuevamente más tarde."* | Sugiere reintento posterior sin culpar al usuario. |
| **`DESCONOCIDO`** | Errores genéricos | *"No se pudo conectar con el servidor. Revisa tu conexión a Internet e inténtalo nuevamente."* | Fallback seguro y pedagógico. |

---

## 6. MATRIZ DE PRUEBAS Y VALIDACIÓN TÉCNICA

### 6.1 Backend ASP.NET Core 8 (`Backend/AdoptMe.sln`)
- **Comando:** `dotnet test Backend/AdoptMe.sln`
- **Resultado:** **84/84 superadas** (0 fallas, 0 errores, duración 1s).
- **Integridad:** Las validaciones de CentroId nullable, aislamiento por centro y persistencia en Neon permanecen al 100%.

### 6.2 Pruebas Unitarias Android (`testDebugUnitTest` y `testReleaseUnitTest`)
- **Pruebas previas:** 205 pruebas.
- **Nuevas pruebas unitarias:** +17 pruebas en `GeolocalizacionYPanelCentroTest.kt`.
- **Total ejecutado y superado:** **222/222 pruebas superadas** (0 fallas, 0 omitidas).
  - Pruebas de geocodificación inversa: dirección física con calle y número, uso de featureName, descarte de coordenadas como texto de calle, fallback de distrito (subLocality → locality → subAdminArea), limpieza de duplicados y preservación de coordenadas.
  - Pruebas de taxonomía de red: clasificación de 401, 403, 500, 502, timeouts, caídas de conexión y excepciones genéricas.
  - Pruebas de compatibilidad regresiva: `EdadMascotaYConexionTest` y `NotificacionesYMensajesNoLeidosTest` superadas.

### 6.3 Verificación de Compilación Release
- **Comando:** `.\gradlew.bat compileReleaseKotlin testReleaseUnitTest assembleRelease`
- **Resultado:** **BUILD SUCCESSFUL in 1m 28s** (57 tareas ejecutadas, APK de Release empaquetado sin errores de R8 o ProGuard).

---

## 7. POLÍTICA DE SEGURIDAD Y ESTADO GIT

- **Base de Datos Neon PostgreSQL:** 0 modificaciones de esquema o datos en esta fase; se preservan intactas las 18 mascotas históricas.
- **Servidor Render:** Sin despliegues manuales; se mantiene como backend por defecto (`https://adoptme2.onrender.com/`).
- **Control de versiones:**
  - Sin `git commit`, `git push`, Pull Request ni `merge` automáticos.
  - `git diff --check` limpio de problemas de formato.
  - Auditoría de credenciales: ninguna clave privada, secreto o token expuesto.
- **Archivos nuevos creados en la fase:**
  - `Android/app/src/main/java/com/example/adoptme/utils/ReverseGeocodingHelper.kt`
  - `Android/app/src/test/java/com/example/adoptme/GeolocalizacionYPanelCentroTest.kt`
  - `docs/UX_B2_MAPA_PANEL_CENTRO_RESPONSIVE.md`
