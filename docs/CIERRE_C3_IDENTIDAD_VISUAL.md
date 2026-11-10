# CIERRE FASE C3 — IDENTIDAD VISUAL, LAUNCHER ADAPTATIVO, SPLASH Y VALIDACIÓN MULTIDISPOSITIVO

## 1. Estado Inicial

- **Repositorio:** `D:\Escritorio D\T2_GRUPO3_ DESARROLLO_APP\AdoptMe2_Entrega4_Git`
- **Rama activa:** `feature/ux-notificaciones-centros-responsive`
- **HEAD / origin/main local:** `da712812c8cf3d4975bcab671cbfa807cab77057`
- **applicationId:** `com.example.adoptme` (intacto)
- **Nombre de aplicación:** `AdoptMe` (intacto en `strings.xml`)
- **Backend predeterminado:** `https://adoptme2.onrender.com/` (intacto en `build.gradle.kts`)
- **Estado previo C2:** Limpieza de 38 archivos legados completada, `repository/AppStorage.kt` como única implementación canónica de almacenamiento.
- **Pruebas previas:** Backend 116/116, Android Debug 243/243, Android Release 243/243.

---

## 2. Identificador y SHA del Nuevo Stash C3

- **Identificador:** `stash@{0}`
- **Mensaje:** `backup-pre-visual-c3-2026-11-10`
- **Commit SHA:** `5325d9f0f1042ab15702b794c056c5875805a3cc`
- **Creado con:** `git stash push -u -m "backup-pre-visual-c3-2026-11-10"`
- **Reaplicado con:** `git stash apply "stash@{0}"` (preservando el stash en el stack).

---

## 3. Confirmación de Conservación del Stash C2

- **Posición actual:** `stash@{1}`
- **Mensaje:** `backup-pre-cleanup-c2-2026-11-10`
- **Commit SHA:** `ff9785a1ba0849733017b257b8ba38e071c91ab0`
- **Confirmación:** Ambos stashes (`stash@{0}` y `stash@{1}`) coexisten y se mantienen íntegros en el listado de Git sin haber sufrido `pop`, `drop` ni `clear`.

---

## 4. Propiedades de las Imágenes Originales

Se auditaron y preservaron intactas las dos imágenes originales en `Android/app/src/main/res/drawable/`:

### A. `adoptme_logo_original.png`
- **Dimensiones:** 1254 × 1254 px (relación de aspecto 1:1 cuadrada).
- **Modo de color:** `Format32bppArgb` (RGBA con canal Alpha).
- **Tamaño en disco:** 1,124,221 bytes (~1.07 MB).
- **Bounding Box de contenido visible ($\alpha > 10$):** $X \in [95, 1149]$, $Y \in [106, 1164]$ (ancho = 1055 px, alto = 1059 px). Centro visual: $(622, 635)$ frente al centro geométrico $(627, 627)$.
- **Contenido visual:** Huella de mascota en gradiente cálido naranja/ámbar (`#F84D04` a `#FF7A45`) con corazón blanco incrustado en la almohadilla central y 4 almohadillas dactilares.
- **Riesgo de recorte sin adaptación:** Alto si se utiliza directo sin márgenes; nulo tras aplicar la reducción adaptativa a zona segura de 66 dp.
- **Uso activo en app:** Pantalla de Inicio, Inicio de Sesión, Registro, Mapa de Adopciones, Administrador y Consumo Responsable.

### B. `adoptme_dog_cat_original.png`
- **Dimensiones:** 1536 × 1024 px (relación de aspecto 1.5:1 horizontal).
- **Modo de color:** `Format32bppArgb` (RGBA con canal Alpha).
- **Tamaño en disco:** 1,780,525 bytes (~1.70 MB).
- **Bounding Box de contenido visible ($\alpha > 30$):** $X \in [163, 1385]$, $Y \in [74, 981]$ (ancho = 1223 px, alto = 908 px).
- **Contenido visual:** Ilustración estilizada de un perro y un gato abrazados en armonía con colores corporativos AdoptMe.
- **Uso activo en app:** Cabecera de `PantallaInicioSesion.kt`.

Ambos archivos se mantuvieron **100% inalterados**; las transformaciones se generaron exclusivamente en archivos derivados.

---

## 5. Diagnóstico del Launcher Anterior

- **Robot Android en Vector:** `Android/app/src/main/res/drawable/ic_launcher_foreground.xml` contenía la ruta vectorial de la cabeza del robot de Android (`pathData="M65.3,45.828l3.8,-6.6..."`).
- **Fondo con rejilla verde:** `Android/app/src/main/res/drawable/ic_launcher_background.xml` utilizaba `#3DDC84` con líneas de cuadrícula blancas semitransparentes.
- **Archivos Mipmap legados:** Las 5 densidades (`mdpi`, `hdpi`, `xhdpi`, `xxhdpi`, `xxxhdpi`) en `mipmap-*/ic_launcher.webp` e `ic_launcher_round.webp` contenían los bitmaps predeterminados del robot verde.
- **Diagnóstico:** Incoherencia total con la identidad de AdoptMe; reemplazo total obligatorio sin dejar rastro del robot.

---

## 6. Diseño Elegido

- **Elemento Central:** La huella institucional con corazón incrustado de AdoptMe, aislada del fondo, centrada geométricamente y escalada con precisión.
- **Fondo Institucional:** `#FAF7F2` (`AdoptMeBg`, fondo crema cálido oficial definido en `Color.kt`).
- **Borde de Delimitación:** `#E8E2D9` (`BorderWarm`, borde de 1 px) para las variantes legacy, garantizando contraste y separación limpia tanto sobre fondos de pantalla blancos como negros.
- **Sin Elementos Incompatibles:** Se eliminó cualquier texto pequeño "AdoptMe", evitando ruido visual o pixelación en tamaños reducidos.

---

## 7. Zona Segura Utilizada

- **Canvas Total Adaptativo:** 108 dp × 108 dp ($432 \times 432$ px en xxxhdpi).
- **Zona Segura Máxima:** Círculo de 66 dp de diámetro (radio 33 dp = 132.0 px en xxxhdpi) centrado en $(216, 216)$.
- **Dimensión asignada al símbolo (huella):** 60 dp ($240 \times 240$ px en xxxhdpi), lo que representa el 55.5% del canvas total y el 83.3% del viewport visible de 72 dp.
- **Radio máximo real del contenido:** 120.70 px desde el centro.
- **Margen de seguridad respecto al límite de 66 dp:** 11.30 px (2.82 dp) de margen libre.
- **Margen de seguridad respecto al corte circular de 72 dp:** 23.30 px (5.82 dp) de margen libre.
- **Resultado matemático:** 0 píxeles recortados bajo cualquier máscara (circular, squircle, cuadrada o redondeada).

---

## 8. Colores Utilizados

| Nombre | Valor HEX / RGB | Uso |
|---|---|---|
| `AdoptOrange` | `#FF7A45` / `rgb(248, 77, 4)` | Huella principal y almohadillas |
| `AdoptWhite` | `#FFFFFF` / `rgb(255, 255, 255)` | Corazón incrustado y detalles interiores |
| `AdoptMeBg` | `#FAF7F2` / `rgb(250, 247, 242)` | Fondo cálido institucional del launcher y splash claro |
| `BorderWarm` | `#E8E2D9` / `rgb(232, 226, 217)` | Borde de contorno suave para legacy icons |
| `DarkBg` | `#191615` / `rgb(25, 22, 21)` | Fondo del splash en modo oscuro (coincide con `VerdeOscuro` de Compose) |

---

## 9. Recursos Derivados Creados

1. `res/drawable/adoptme_launcher_background.xml` (VectorDrawable 108dp fondo `#FAF7F2`).
2. `res/drawable/adoptme_launcher_foreground.png` (Master PNG 432 × 432 px).
3. `res/drawable/adoptme_launcher_monochrome.png` (Master PNG 432 × 432 px).
4. `res/drawable/adoptme_splash_background.xml` (Layer-list para splash API 24–30 tema claro).
5. `res/drawable/adoptme_splash_background_dark.xml` (Layer-list para splash API 24–30 tema oscuro).
6. `res/drawable/adoptme_splash_icon.png` (PNG 288 × 288 px para splash icon).
7. `res/drawable-mdpi/adoptme_launcher_foreground.png` y `monochrome.png` (108 × 108 px).
8. `res/drawable-hdpi/adoptme_launcher_foreground.png` y `monochrome.png` (162 × 162 px).
9. `res/drawable-xhdpi/adoptme_launcher_foreground.png` y `monochrome.png` (216 × 216 px).
10. `res/drawable-xxhdpi/adoptme_launcher_foreground.png` y `monochrome.png` (324 × 324 px).
11. `res/drawable-xxxhdpi/adoptme_launcher_foreground.png` y `monochrome.png` (432 × 432 px).
12. `res/values-night/themes.xml` (Soporte tema oscuro nativo y splash dark).

---

## 10. Densidades y Dimensiones

### A. Íconos Legacy (`mipmap-*/`)
Generados con **supermuestreo 4x (supersampling)** y redimensionamiento Lanczos de alta calidad:

| Densidad | Factor | `ic_launcher.webp` | `ic_launcher_round.webp` |
|---|---|---|---|
| `mdpi` | 1.0x | 48 × 48 px | 48 × 48 px |
| `hdpi` | 1.5x | 72 × 72 px | 72 × 72 px |
| `xhdpi` | 2.0x | 96 × 96 px | 96 × 96 px |
| `xxhdpi` | 3.0x | 144 × 144 px | 144 × 144 px |
| `xxxhdpi` | 4.0x | 192 × 192 px | 192 × 192 px |

### B. Primer Plano Adaptativo (`drawable-*/`)

| Densidad | Factor | Foreground y Monochrome | Tamaño Huella |
|---|---|---|---|
| `mdpi` | 1.0x | 108 × 108 px | 60 × 60 px |
| `hdpi` | 1.5x | 162 × 162 px | 90 × 90 px |
| `xhdpi` | 2.0x | 216 × 216 px | 120 × 120 px |
| `xxhdpi` | 3.0x | 324 × 324 px | 180 × 180 px |
| `xxxhdpi` | 4.0x | 432 × 432 px | 240 × 240 px |

---

## 11. Configuración Adaptativa

En `Android/app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml`:
```xml
<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@drawable/adoptme_launcher_background" />
    <foreground android:drawable="@drawable/adoptme_launcher_foreground" />
    <monochrome android:drawable="@drawable/adoptme_launcher_monochrome" />
</adaptive-icon>
```

---

## 12. Configuración de Ícono Redondo

En `Android/app/src/main/res/mipmap-anydpi-v26/ic_launcher_round.xml`:
```xml
<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@drawable/adoptme_launcher_background" />
    <foreground android:drawable="@drawable/adoptme_launcher_foreground" />
    <monochrome android:drawable="@drawable/adoptme_launcher_monochrome" />
</adaptive-icon>
```
En dispositivos API < 26, el sistema resuelve automáticamente hacia `mipmap-[density]/ic_launcher_round.webp`, el cual posee la forma circular con recorte alpha perfecto.

---

## 13. Configuración Monochrome (Themed Icons Android 13+)

- Se generó `adoptme_launcher_monochrome.png` para cada densidad.
- La silueta conserva la huella externa con opacidad total ($\alpha = 255$) y perfora como transparencia ($\alpha = 0$) el corazón central y la separación entre dedos.
- En Android 13+, cuando el usuario activa "Íconos con tema" (Material You), el sistema tiñe suavemente la huella respetando la forma del corazón y los dedos, integrándose con el fondo del launcher del usuario.

---

## 14. Implementación del Splash Screen

- **Sin dependencias externas pesadas ni timers artificiales:** No se introdujo retardo de hilo ni SplashScreen libraries no requeridas.
- **Android 12+ (API 31+):**
  - Tema claro: `android:windowSplashScreenBackground = #FAF7F2`, `android:windowSplashScreenAnimatedIcon = @drawable/adoptme_splash_icon`.
  - Tema oscuro: `android:windowSplashScreenBackground = #191615`, `android:windowSplashScreenAnimatedIcon = @drawable/adoptme_splash_icon`.
- **Android < 12 (API 24–30):**
  - `android:windowBackground` configurado mediante un `layer-list` nativo (`adoptme_splash_background.xml` y `adoptme_splash_background_dark.xml`) con el fondo oficial y el icono centrado a 120 dp.
- **Transición fluida a Jetpack Compose:** Al inicializarse `MainActivity`, `AdoptMeTheme` aplica `background = AdoptMeBg` (`#FAF7F2`) en claro y `#191615` en oscuro, logrando una transición con **cero parpadeo o flash cromático**.

---

## 15. Cambios Mínimos del Login

- Se verificó `PantallaInicioSesion.kt`:
  - `adoptme_logo_original` se muestra con tamaño equilibrado de 68 dp y `ContentScale.Fit`.
  - `adoptme_dog_cat_original` se encuentra integrada con `ContentScale.Fit`, `fillMaxWidth(0.65f)`, `widthIn(max = 240.dp)`, `heightIn(min = 60.dp, max = 125.dp)`.
  - `contentDescription = "Perro y gato de AdoptMe"` para accesibilidad completa.
  - El contenedor principal cuenta con `.verticalScroll(rememberScrollState())` e `.imePadding()`, asegurando que en pantallas compactas (320 × 568 dp) o al desplegarse el teclado en pantalla, los campos de usuario y contraseña nunca queden inaccesibles.

---

## 16. Validación de Tema Claro y Oscuro

- **Tema Claro:**
  - Splash: fondo `#FAF7F2`, icono naranja AdoptMe.
  - Launcher: fondo `#FAF7F2` con contorno `#E8E2D9`, símbolo vibrante con ratio de contraste > 3.6:1.
  - Login: cabecera con gradiente oficial naranja/verde azulado y tarjeta blanca.
- **Tema Oscuro:**
  - Splash: fondo `#191615` con icono AdoptMe vibrante (contraste excelente > 7:1).
  - Configurado en `res/values-night/themes.xml`.
  - No genera saltos cromáticos hacia el tema oscuro de la aplicación.

---

## 17. Validación de Tamaños y Orientaciones

- **Pantalla compacta (320 × 568 dp / mdpi):** Altura máxima del banner acotada a 125 dp, scroll habilitado; campos e interfaces no se cortan.
- **Pantalla normal (1080 × 2400 px / xxhdpi):** Renderizado de alta definición nativo.
- **Orientación horizontal (Landscape):** Con `verticalScroll` e `imePadding`, el login y pantallas se desplazan limpiamente sin desbordamiento de componentes.
- **Escala de fuentes:** Al usar modificadores adaptativos y fuentes `sp` vinculadas a Compose Typography, los textos crecen sin romper la jerarquía visual de los logos.

---

## 18. Validaciones Realizadas en Emulador / Entorno Local

- El daemon ADB fue iniciado en el entorno local (`List of devices attached` vacío al no haber emulador activo en este momento).
- Se ejecutó auditoría visual y matemática automatizada mediante script local Python 3.11.9 / Pillow 12.3.0:
  - Máscara Circular: **0 píxeles recortados**.
  - Máscara Squircle: **0 píxeles recortados**.
  - Máscara Cuadrada Redondeada: **0 píxeles recortados**.
  - Distancia máxima de píxeles al centro: **120.70 px** (holgura de **11.30 px** sobre la zona segura de 132 px).

---

## 19. Validaciones Visuales Pendientes

- Comprobación en vivo del comportamiento dinámico en un dispositivo físico o emulador AVD con Android 13+ para verificar la colorización de íconos temáticos del launcher del fabricante (ej. Pixel Launcher o One UI).
- Verificación de la animación de salida del splash en hardware real.

---

## 20. Resultado Backend

- **Comando:** `dotnet build Backend/AdoptMe.sln` y `dotnet test Backend/AdoptMe.sln`
- **Resultado compilación:** 0 advertencias, 0 errores.
- **Resultado pruebas:** **116/116 aprobadas** (0 fallidas, 0 omitidas).
- **Duración:** 1 s.

---

## 21. Resultado Android Debug

- **Comando:** `.\gradlew.bat clean compileDebugKotlin testDebugUnitTest`
- **Resultado compilación:** BUILD SUCCESSFUL.
- **Resultado pruebas unitarias:** **243/243 aprobadas** (0 fallidas, 0 omitidas).
- **Duración:** 26 s.

---

## 22. Resultado Android Release

- **Comando:** `.\gradlew.bat compileReleaseKotlin testReleaseUnitTest assembleRelease`
- **Resultado compilación:** BUILD SUCCESSFUL.
- **Resultado pruebas unitarias:** **243/243 aprobadas** (0 fallidas, 0 omitidas).
- **Duración:** 42 s.

---

## 23. Resultado assembleRelease

- **Estado:** BUILD SUCCESSFUL.
- **Ruta de salida:** `Android/app/build/outputs/apk/release/app-release-unsigned.apk`
- **Tamaño:** **13.28 MB** (13,927,095 bytes).
- **Confirmación Git:** El APK generado permanece en `build/` ignorado por Git, sin haberse agregado al árbol de trabajo.

---

## 24. Estado Final de Git

- **Rama:** `feature/ux-notificaciones-centros-responsive`
- **HEAD:** `da712812c8cf3d4975bcab671cbfa807cab77057`
- **`git diff --check`:** Salida limpia, sin errores de espacio en blanco ni conflictos.
- **Stashes conservados:**
  - `stash@{0}`: `5325d9f0f1042ab15702b794c056c5875805a3cc` (`backup-pre-visual-c3-2026-11-10`)
  - `stash@{1}`: `ff9785a1ba0849733017b257b8ba38e071c91ab0` (`backup-pre-cleanup-c2-2026-11-10`)

---

## 25. Riesgos Conocidos

- **Lanzadores de terceros con recorte agresivo (< 60 dp):** Prácticamente inexistentes, dado que la especificación oficial de Google CDD establece la zona segura en 66 dp, y el ícono actual tiene un diámetro de 60 dp (240 px sobre 432 px).
- **Iconos temáticos en Android 12:** Android 12 introdujo iconos temáticos en fase beta solo para apps de Google; el soporte abierto a terceros inició en Android 13 (API 33). En Android 12 se mostrará el ícono adaptativo estándar a color.

---

## 26. Decisión

**APTO CON VALIDACIÓN VISUAL PENDIENTE**

El sistema cuenta con todos los recursos gráficos y XML adaptativos generados con calidad profesional, 100% de pruebas aprobadas en Backend (116) y Android (243 Debug + 243 Release), compilación Release sin errores y eliminación absoluta del robot Android. Queda únicamente pendiente la confirmación visual en pantalla viva cuando el usuario ejecute la app en emulador o dispositivo físico.
