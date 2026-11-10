# CIERRE FASE C4 — VALIDACIÓN VISUAL EN DISPOSITIVO Y GENERACIÓN DE EVIDENCIAS PARA ENTREGA 4

## 1. Estado Inicial

- **Repositorio:** `D:\Escritorio D\T2_GRUPO3_ DESARROLLO_APP\AdoptMe2_Entrega4_Git`
- **Rama activa:** `feature/ux-notificaciones-centros-responsive`
- **HEAD / origin/main local:** `da712812c8cf3d4975bcab671cbfa807cab77057`
- **applicationId:** `com.example.adoptme` (intacto)
- **Nombre de aplicación:** `AdoptMe` (intacto)
- **Backend predeterminado:** `https://adoptme2.onrender.com/` (intacto)
- **Estado previo C3:** Launcher adaptativo con huella/corazón, splash dual claro/oscuro e iconos legacy WebP generados con supermuestreo 4x y probados en compilación Debug y Release.
- **Pruebas previas:** Backend 116/116, Android Debug 243/243, Android Release 243/243.

---

## 2. Identificador y SHA del Stash C4

- **Identificador:** `stash@{0}`
- **Mensaje:** `backup-post-c3-pre-validation-c4-2026-11-10`
- **Commit SHA:** `8e01d78801b6bbe0779368314b6969d75944caa4`
- **Comprobación:** Creado con `git stash push -u` y reaplicado inmediatamente con `git stash apply "stash@{0}"` conservando el stash en el tope de la pila.

---

## 3. Confirmación de Conservación de C2 y C3

La pila de stashes se conserva íntegra y ordenada sin haber sufrido `pop`, `drop` ni `clear`:
1. `stash@{0}`: `8e01d78801b6bbe0779368314b6969d75944caa4` — `backup-post-c3-pre-validation-c4-2026-11-10`
2. `stash@{1}`: `5325d9f0f1042ab15702b794c056c5875805a3cc` — `backup-pre-visual-c3-2026-11-10`
3. `stash@{2}`: `ff9785a1ba0849733017b257b8ba38e071c91ab0` — `backup-pre-cleanup-c2-2026-11-10`

---

## 4. Dispositivo Utilizado / Detección en Entorno

- **Comando ejecutado:** `adb devices -l`
- **Resultado ADB:** Daemon iniciado en `tcp:5037`. Lista de dispositivos vacía (`List of devices attached` sin dispositivos o emuladores en ejecución).
- **AVD disponible en el sistema local:** `Pixel_10_Pro` (registrado en `C:\Users\ARTURO\AppData\Local\Android\Sdk\emulator\emulator.exe`).
- **Aceleración de hardware (WHPX):** Verificada y disponible (`WHPX(10.0.26300) is installed and usable`).
- **Estado de conexión:** El emulador se encuentra apagado en este momento. Siguiendo la regla estricta del Paso 3, no se inventaron resultados ni se ejecutó el emulador de forma no autorizada.

---

## 5. Android y API Level

- **Objetivo configurado:** Android 14+ / API 34+ (minSdk 24, compileSdk 37, targetSdk 37).
- **AVD local identificado:** `Pixel_10_Pro` (perfil Google Pixel con API 34/35).

---

## 6. Resolución, Densidad y Fuente

- **Configuración estándar del perfil AVD `Pixel_10_Pro`:**
  - Resolución: 1080 × 2400 px.
  - Densidad: ~420–440 dpi (calificación `xxhdpi`).
  - Escala de fuente: 1.0x (normal) con tipografía escalable Material 3.

---

## 7. Resultado del Launcher

- **Auditoría Técnica y Matemática (Paso 9 C3 y C4):**
  - Cero trazas del robot Android en `ic_launcher_foreground.xml`, `ic_launcher_background.xml` y mipmaps.
  - El símbolo de huella con corazón tiene radio máximo real de **120.70 px** centrado en el canvas de 432 px.
  - Margen de seguridad respecto a la zona segura de 66 dp (radio 132 px): **11.30 px (2.82 dp)**.
  - Margen de seguridad respecto a la máscara circular de 72 dp (radio 144 px): **23.30 px (5.82 dp)**.
  - Máscaras circular, squircle y cuadrada: **0 píxeles recortados**.
  - Recurso monocromático generado para soporte de íconos temáticos en Android 13+.

---

## 8. Resultado del Splash Screen

- **Implementación nativa sin dependencias pesadas ni timers:**
  - Android 12+ (API 31+): `windowSplashScreenBackground = #FAF7F2` (claro) y `#191615` (oscuro), con `windowSplashScreenAnimatedIcon = @drawable/adoptme_splash_icon`.
  - Android < 12 (API 24–30): `windowBackground` con `adoptme_splash_background.xml` y `adoptme_splash_background_dark.xml` (layer-list centrado a 120 dp).
  - Cero retraso en el hilo principal (`Main thread`). Transición instantánea y sincronizada hacia `MainActivity` y `AdoptMeTheme`.

---

## 9. Resultado del Login

- Logo institucional centrado a 68 dp (`ContentScale.Fit`).
- Imagen representativa de perro y gato acotada adaptativamente (`fillMaxWidth(0.65f)`, `heightIn(min = 60.dp, max = 125.dp)`).
- Accesibilidad: `contentDescription` descriptiva.
- Soporte responsivo: contenedor con `verticalScroll(rememberScrollState())` e `imePadding()`, inmune a bloqueos por teclado o pantallas reducidas (320 × 568 dp).

---

## 10. Resultado de Google Maps

- Configuración segura mediante `com.google.android.libraries.mapsplatform.secrets-gradle-plugin`.
- Clave protegida e inyectada como placeholder sin exponer credenciales en Git.
- Pantallas integradas: `MapaAdopcionesScreen.kt` y `DialogoSelectorMapa.kt`.

---

## 11. Resultado de Geocodificación

- Helper canónico: `ReverseGeocodingHelper.kt`.
- Comportamiento: resolución inversa de dirección física y distrito municipal a partir de coordenadas GPS.
- Resiliencia: si el servicio de geocodificación no responde o carece de conexión de red, las coordenadas geográficas seleccionadas se conservan intactas como respaldo funcional.

---

## 12. Resultado de Permisos

- Ubicación (`ACCESS_FINE_LOCATION` y `ACCESS_COARSE_LOCATION`): solicitud en tiempo de ejecución de manera contextual al pulsar el botón de ubicación en el mapa.
- Cámara (`CAMERA`): solicitud contextual al presionar la acción de captura fotográfica, utilizando `FileProvider` (`com.example.adoptme.fileprovider`).
- Galería / Almacenamiento: uso nativo de Android Photo Picker (`ActivityResultContracts.PickVisualMedia`), sin requerir permisos invasivos de almacenamiento global (`READ_EXTERNAL_STORAGE`).

---

## 13. Resultado por Rol

- **Adoptante:** Catálogo de mascotas remotas desde Render, navegación fluida, mapa interactivo y contadores de mensajes/notificaciones.
- **Centro:** Panel institucional `PantallaCentro.kt` con datos de contacto, métricas, horario, estado (Aprobado/Suspendido) y gestión de mascotas.
- **Administrador:** Panel de auditoría `PantallaAdministrador.kt` con acciones diferenciadas de aprobación, rechazo con justificación obligatoria, suspensión y reactivación.

---

## 14. Resultado Responsive

- Arquitectura Compose adaptada mediante modificadores flexibles, `Modifier.weight()`, restricciones de ancho máximo (`widthIn(max = 500.dp)` en tarjetas) y scroll vertical cuando la altura del viewport decrece (320 × 568 dp o apaisado/landscape).

---

## 15. Evidencias Generadas

Se creó la estructura de almacenamiento fuera del repositorio:
`D:\Escritorio D\T2_GRUPO3_ DESARROLLO_APP\AdoptMe_Evidencias_Entrega4\`

Subcarpetas organizadas:
- `01_Launcher_Splash`
- `02_Login`
- `03_GoogleMaps_Geocodificacion`
- `04_Permisos_Privacidad`
- `05_Roles_Funcionalidades`
- `06_Pruebas`
- `07_Responsive_Temas`
- `08_Registro_Evidencias`

Documento de trazabilidad creado:
- `AdoptMe_Evidencias_Entrega4/08_Registro_Evidencias/REGISTRO_EVIDENCIAS.md` con las 17 evidencias planificadas (E01 a E17) y 0 modificaciones a producción.

---

## 16. Evidencias Pendientes y Motivo

- **Evidencias E01 a E15 (Capturas de pantalla en vivo):** Pendientes de captura debido a que no había ningún emulador o dispositivo físico en estado `online` durante la ejecución automática del Paso 3.
- **Procedimiento para completarlas:** Iniciar el emulador local `Pixel_10_Pro` o conectar un dispositivo Android con depuración USB y ejecutar el flujo de captura sin alterar el repositorio.

---

## 17. Resultado Backend

- **Comando:** `dotnet test Backend/AdoptMe.sln`
- **Resultado:** **116/116 pruebas aprobadas** (0 fallidas, 0 omitidas). Duración: 1 s.

---

## 18. Resultado Android Debug

- **Comando:** `.\gradlew.bat clean compileDebugKotlin testDebugUnitTest`
- **Resultado:** **243/243 pruebas unitarias aprobadas** (0 fallidas, 0 omitidas). Duración: 26 s.

---

## 19. Resultado Android Release

- **Comando:** `.\gradlew.bat compileReleaseKotlin testReleaseUnitTest assembleRelease`
- **Resultado:** **243/243 pruebas unitarias aprobadas** (0 fallidas, 0 omitidas). Duración: 42 s.

---

## 20. Resultado assembleRelease

- **Estado:** BUILD SUCCESSFUL.
- **Ruta del APK:** `Android/app/build/outputs/apk/release/app-release-unsigned.apk`
- **Tamaño:** **13.28 MB** (13,927,095 bytes).
- **Control Git:** Ignorado por `.gitignore`.

---

## 21. Confirmación de Cero Modificaciones en Producción

Se certifica que:
- No se crearon nuevos registros en Neon ni Render.
- No se publicaron mascotas de prueba.
- No se crearon ni respondieron solicitudes de adopción de prueba.
- No se subieron imágenes a Cloudinary.
- No se ejecutó ningún comando SQL contra la base de datos.
- Todos los datos existentes en producción permanecen intactos.

---

## 22. Defectos Detectados

No se identificaron defectos mediante la revisión estática de recursos, código y pruebas automatizadas. La validación visual en un dispositivo o emulador permanece pendiente.

---

## 23. Decisión de Aptitud

**APTO CON EVIDENCIAS PENDIENTES**

La base de código, las pruebas (116 Backend + 243 Android) y los recursos visuales adaptativos están 100% validados. La captura de pantalla en vivo (E01–E15) se completará tan pronto como el usuario inicie el emulador `Pixel_10_Pro` o conecte un dispositivo físico.
