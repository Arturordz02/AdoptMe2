# CIERRE FASE C5 — CIERRE FUNCIONAL Y PREPARACIÓN DE INTEGRACIÓN DEL REPOSITORIO DE TRABAJO

## 1. Estado Inicial

- **Repositorio de trabajo:** `D:\Escritorio D\T2_GRUPO3_ DESARROLLO_APP\AdoptMe2_Entrega4_Git`
- **Rama activa:** `feature/ux-notificaciones-centros-responsive`
- **HEAD / origin/main local:** `da712812c8cf3d4975bcab671cbfa807cab77057`
- **applicationId:** `com.example.adoptme` (intacto)
- **Nombre de aplicación:** `AdoptMe` (intacto)
- **Backend predeterminado:** `https://adoptme2.onrender.com/` (intacto)
- **Naturaleza del repositorio:** Continúa siendo el espacio de trabajo completo y activo acumulado de las fases anteriores (UX-A, UX-B1, UX-B2, UX-B3, C1, C2, C3 y C4).
- **Desglose de archivos en Git:**
  - En stage: **0** archivos.
  - Modificados: **42** archivos.
  - Eliminados intencionalmente: **39** archivos (38 archivos legados retirados en C2 + `Neon_UX_Centros_Suspension_PROPUESTA.sql`).
  - No rastreados (untracked): **27** entradas (recursos gráficos derivados de densidades, clases de prueba nuevas, helpers y documentación).
- **Implementación canónica:** [`repository/AppStorage.kt`](file:///D:/Escritorio%20D/T2_GRUPO3_%20DESARROLLO_APP/AdoptMe2_Entrega4_Git/Android/app/src/main/java/com/example/adoptme/repository/AppStorage.kt) permanece como la única y definitiva implementación de almacenamiento local.

---

## 2. Stashes Conservados

Se conservan íntegros los 3 respaldos en la pila de Git, verificados mediante `git stash list`:
1. `stash@{0}`: `8e01d78801b6bbe0779368314b6969d75944caa4` — `backup-post-c3-pre-validation-c4-2026-11-10`
2. `stash@{1}`: `5325d9f0f1042ab15702b794c056c5875805a3cc` — `backup-pre-visual-c3-2026-11-10`
3. `stash@{2}`: `ff9785a1ba0849733017b257b8ba38e071c91ab0` — `backup-pre-cleanup-c2-2026-11-10`

---

## 3. Coherencia de Arquitectura Verificada

### A. Android
- **`MainActivity.kt`:** Utiliza exclusivamente paquetes canónicos (`model`, `repository`, `preferences`, `database`, `datastore`, `screens`, `viewmodel`, `api`).
- **Eliminación de paquetes legados:** Cero imports hacia `com.example.adoptme.AdoptMe`, `com.example.adoptme.data` (remoto/local legado) y `com.example.adoptme.SharedPreferences`.
- **Conectividad:** `RetrofitClient.kt` apunta de forma predeterminada a `https://adoptme2.onrender.com/` sin requerir backend local ni `adb reverse`.
- **Persistencia:** Room SQLite (`adoptme_local.db`) para mascotas visitadas; Preferences DataStore para el catálogo; SharedPreferences (`Preferencias.kt`) para sesión, JWT, términos y contadores locales.
- **Localización y Google Maps:** `ReverseGeocodingHelper.kt` integrado y operativo; Maps Compose y Play Services configurados con inyección segura de clave en `secrets.properties`.
- **Medios:** `PhotoPicker` de Android nativo (`PickVisualMedia`) para selección segura sin permisos de almacenamiento global, y `FileProvider` para la cámara.
- **Limpieza de dependencias:** Cero dependencias, configuración o endpoints de Firebase o Azure.

### B. Backend (ASP.NET Core 8 Web API)
- **ORM y Base de Datos:** Entity Framework Core con PostgreSQL (`Npgsql.EntityFrameworkCore.PostgreSQL`) apuntando a Neon PostgreSQL 16.
- **Seguridad:** Autenticación por Bearer JWT con validación estricta de Claims (`ClaimTypes.Role`, `ClaimTypes.Email`, `ClaimTypes.NameIdentifier`).
- **Controladores y Endpoints:**
  - `UsuariosController.cs` (Registro, login y perfil).
  - `MascotasController.cs` (Catálogo público, publicación y edición de mascotas).
  - `SolicitudesController.cs` (Creación y seguimiento de adopciones).
  - `CentrosController.cs` (Consulta pública geolocalizada y panel "Mi Centro").
  - `AdminCentrosController.cs` (Aprobación, rechazo motivado, suspensión, reactivación y vinculación).
- **Lógica de Suspensión:** `CentroAuthorizationService.cs` valida que los centros suspendidos (`Activo = false` o `Estado != "Aprobado"`) tengan bloqueadas las operaciones institucionales, devolviendo HTTP 403 con el motivo correspondiente.

---

## 4. Matriz de Requisitos de Entrega 4

| Requisito Académico | Implementación Real en AdoptMe | Evidencia Técnica / Archivos Clave |
|---|---|---|
| **1. Persistencia local** | Room SQLite (`AdoptMeDatabase`), Preferences DataStore y SharedPreferences (`Preferencias.kt`, `AppStorage.kt`). | `AdoptMeDatabase.kt`, `CatalogoDataStore.kt`, `Preferencias.kt` |
| **2. Servicios REST** | API RESTful en ASP.NET Core 8 consumida mediante Retrofit + OkHttp con serialización Gson y manejo tipado de errores. | `RetrofitClient.kt`, `CentrosApi.kt`, `CentrosController.cs` |
| **3. Firebase** | **Firebase no se utiliza en AdoptMe.** Su inclusión fue considerada opcional y el proyecto cubre persistencia y servicios remotos mediante Room, DataStore, SharedPreferences, ASP.NET Core REST, Neon PostgreSQL y Cloudinary. | `build.gradle.kts` (cero dependencias de Firebase) |
| **4. Google Maps API** | SDK Google Maps v19 + Maps Compose v4.4.1 con clave inyectada por Secrets Gradle Plugin. | `MapaAdopcionesScreen.kt`, `DialogoSelectorMapa.kt`, `secrets.properties` |
| **5. Geolocalización** | Google Location Services + geocodificación inversa para transformar coordenadas GPS en dirección y distrito. | `ReverseGeocodingHelper.kt`, `CentrosController.cs` (Haversine) |
| **6. Permisos de ubicación** | Solicitud contextual de `ACCESS_FINE_LOCATION` y `ACCESS_COARSE_LOCATION` en tiempo de ejecución. | `DialogoSelectorMapa.kt`, `AndroidManifest.xml` |
| **7. Permiso de cámara** | Solicitud contextual de `CAMERA` con captura segura mediante `FileProvider` temporal. | `AndroidManifest.xml`, `file_paths.xml`, `PantallaPublicarMascota.kt` |
| **8. Photo Picker** | Uso del Android Photo Picker moderno del sistema (`ActivityResultContracts.PickVisualMedia`). | `PantallaPublicarMascota.kt` (sin permisos invasivos de storage) |
| **9. Privacidad** | Aislamiento estricto de datos locales por usuario, contraseñas hash BCrypt, tokens JWT y sin rastreadores externos. | `Preferencias.kt`, `PasswordHasher.cs`, `DialogoTerminosYPrivacidad.kt` |
| **10. Términos y condiciones** | Diálogo modal interactivo con tabs de Términos y Privacidad, aceptación persistida en preferencias. | `DialogoTerminosYPrivacidad.kt`, `PantallaRegistro.kt` |
| **11. Pruebas funcionales** | 116 pruebas automatizadas en Backend y 243 pruebas unitarias en Android (Debug y Release). | `AdoptMe.Api.Tests`, `app/src/test/java/com/example/adoptme` |
| **12. Depuración y optimización** | Configuración ProGuard/R8, descarte de 38 archivos legados y reducción de APK de 13.89 MB a 13.28 MB. | `app/build.gradle.kts`, `app-release-unsigned.apk` |
| **13. Producto funcional por roles** | Tres roles interactivos completos: Adoptante (catálogo, mapa, chat), Centro (gestión institucional), Administrador (auditoría). | `PantallaPrincipalAdoptante.kt`, `PantallaCentro.kt`, `PantallaAdministrador.kt` |

---

## 5. Resultado de Pruebas Automatizadas

- **Backend (.NET 8):**
  - Comando: `dotnet test Backend/AdoptMe.sln`
  - Aprobadas: **116** | Fallidas: **0** | Omitidas: **0** | Total: **116** (1 s).
- **Android Debug:**
  - Comando: `.\gradlew.bat testDebugUnitTest`
  - Aprobadas: **243** | Fallidas: **0** | Omitidas: **0** | Total: **243** (5 s).
- **Android Release:**
  - Comando: `.\gradlew.bat testReleaseUnitTest`
  - Aprobadas: **243** | Fallidas: **0** | Omitidas: **0** | Total: **243** (9 s).
- **Compilación de APK Release (`assembleRelease`):**
  - Estado: **BUILD SUCCESSFUL** (16 s).
  - Ubicación: `Android/app/build/outputs/apk/release/app-release-unsigned.apk`.
  - Tamaño: **13.28 MB** (13,927,095 bytes).
- **Verificación Git (`git diff --check`):**
  - Salida: Limpia, 0 errores de sintaxis, espacios o formato.

---

## 6. Estado Honesto de la Fase C4

- Los recursos de **Launcher, Splash Screen, Pantalla de Login y adaptabilidad Responsive** fueron auditados y verificados a nivel de código, especificación de densidad, geometría vectorial y pruebas automatizadas.
- **No se realizó validación visual en vivo** porque ningún emulador ni dispositivo físico se encontraba en ejecución / online al ejecutar el Paso 3.
- Las capturas **E01 a E15 permanecen pendientes** de captura real en hardware/emulador.
- **Declaración oficial:**
  > *“No se identificaron defectos mediante la revisión estática de recursos, código y pruebas automatizadas. La validación visual en un dispositivo o emulador permanece pendiente.”*
- En esta Fase C5 **no se generaron capturas simuladas ni evidencias inventadas**.

---

## 7. Estrategia Propuesta de Commits para Integración Git

Dado que múltiples archivos centrales (como `MainActivity.kt`, `PantallaCentro.kt`, `AppStorage.kt` y `CentrosController.cs`) acumulan cambios de varias fases funcionales, **separar commits por líneas individuales mediante `git add -p` acarrea un riesgo elevado de introducir roturas intermedias de compilación**.

Se evalúan y recomiendan las siguientes opciones de integración:

### Opción Recomendada: 6 a 8 Commits Funcionales por Capas

1. **Commit 1: `feat(backend): completar gestion, suspension y seguridad de centros`**
   - **Archivos:** `Backend/AdoptMe.Api/Controllers/AdminCentrosController.cs`, `CentrosController.cs`, `Data/AdoptMeDbContext.cs`, `Models/Centro*.cs`, `Models/SuspenderCentroDto.cs`, `Services/CentroAuthorizationService.cs`, `Data/Neon_UX_Centros_Suspension.sql`.
   - **Dependencias:** Autocontenido en Backend. Compilación y 116 pruebas pasan de forma autónoma.
2. **Commit 2: `feat(android): integrar centros dinamicos, geolocalizacion y reverse geocoding`**
   - **Archivos:** `Android/app/src/main/java/com/example/adoptme/api/CentrosApi.kt`, `components/DialogoSelectorMapa.kt`, `utils/ReverseGeocodingHelper.kt`, `model/CentroModels.kt`, `viewmodel/AdminCentrosViewModel.kt`, `screens/PantallaEstadoCentro.kt`.
3. **Commit 3: `feat(android): agregar persistencia y badges para notificaciones y mensajes no leidos`**
   - **Archivos:** `model/MensajeChat.kt`, `model/NotificationItem.kt`, `components/BarraNavegacionInferior.kt`, `screens/PantallaNotificaciones.kt`.
4. **Commit 4: `feat(android): adaptar panel del centro, estados de conexion y responsive`**
   - **Archivos:** `screens/PantallaCentro.kt`, `screens/PantallaAdministrador.kt`, `screens/PantallaPrincipalAdoptante.kt`, `screens/PantallaRegistro.kt`, `screens/PantallaPublicarMascota.kt`, `screens/MisPublicacionesScreen.kt`.
5. **Commit 5: `refactor(android): retirar paquetes legados y consolidar AppStorage canonical`**
   - **Archivos:** Eliminación de los 38 archivos en `AdoptMe/`, `data/`, `SharedPreferences/`, consolidación en `repository/AppStorage.kt`, ajuste de variables en `MainActivity.kt` (`mascotasRemotas`), reescritura de `LEEME.txt`.
6. **Commit 6: `style(android): implementar launcher adaptativo, splash dual y login renovado`**
   - **Archivos:** `res/drawable/adoptme_*`, `res/drawable/ic_launcher_*`, `res/mipmap-*/`, `res/values/themes.xml`, `res/values-night/themes.xml`.
7. **Commit 7: `test: ampliar bateria de pruebas unitarias en backend y android`**
   - **Archivos:** `Backend/AdoptMe.Api.Tests/*`, `Android/app/src/test/java/com/example/adoptme/*`.
8. **Commit 8: `docs: registrar informes de cierre tecnico C1 a C5 y Entrega 4`**
   - **Archivos:** `docs/*.md`.

> [!WARNING] Riesgo de Separación de Archivos
> Los archivos `MainActivity.kt`, `repository/AppStorage.kt` y `PantallaCentro.kt` tienen dependencias directas entre las fases UX-B1, UX-B2, UX-B3 y C2. Si se separan en commits diferentes, los commits intermedios podrían no compilar si no se orquestan con precisión. Se recomienda aplicar la integración validando la compilación en cada paso intermedio o agruparlos en hitos arquitecturales consolidados.

---

## 8. Riesgos Pendientes

- **Validación visual viva:** Permanece pendiente la comprobación en pantalla física de la animación de splash y el contraste dinámico en un dispositivo real.
- **Secretos y credenciales:** Las claves de Google Maps y Cloudinary se mantienen inyectadas mediante variables de entorno seguras (`secrets.properties`); debe verificarse que no se incluyan archivos `.properties` privados en el stage final.

---

## 9. Confirmaciones de Cierre C5

1. **Espacio de trabajo:** El repositorio local `AdoptMe2_Entrega4_Git` continúa siendo el entorno activo y completo de desarrollo.
2. **Copia limpia:** **No se ha creado** todavía la carpeta limpia “AdoptMe”.
3. **ZIP de entrega:** **No se ha generado** todavía el archivo comprimido final.
4. **Evidencias e imágenes:** **Cero capturas o imágenes generadas** durante esta fase.
5. **Producción:** **Cero registros creados, modificados o eliminados** en Neon, Render o Cloudinary.
6. **Operaciones Git:** **Cero commits, pushes, PRs, merges o despliegues** ejecutados.
