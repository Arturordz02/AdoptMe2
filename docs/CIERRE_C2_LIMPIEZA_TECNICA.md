# INFORME TÉCNICO DE LIMPIEZA — FASE C2
## Respaldo Recuperable, Consolidación Android y Limpieza de Residuos

**Fecha de ejecución:** 10 de noviembre de 2026  
**Proyecto:** AdoptMe — Plataforma de Adopción Responsable de Mascotas  
**Repositorio local:** `D:\Escritorio D\T2_GRUPO3_ DESARROLLO_APP\AdoptMe2_Entrega4_Git`  
**Rama obligatoria:** `feature/ux-notificaciones-centros-responsive`  
**Base SHA esperada y confirmada:** `da712812c8cf3d4975bcab671cbfa807cab77057` (`origin/main`)  
**Backend de producción:** `https://adoptme2.onrender.com/` (Render Web Service)  
**Base de datos:** Neon PostgreSQL 16  
**Almacenamiento multimedia:** Cloudinary  
**Mapas y ubicación:** Google Maps Platform  

---

### 1. Estado Inicial Previo a la Limpieza

Antes de ejecutar las operaciones de limpieza autorizadas, se validó la línea base técnica establecida en la Fase C1:
- **Rama:** `feature/ux-notificaciones-centros-responsive`.
- **HEAD:** `da712812c8cf3d4975bcab671cbfa807cab77057`.
- **origin/main:** `da712812c8cf3d4975bcab671cbfa807cab77057`.
- **28 archivos tracked modificados.**
- **13 archivos untracked** (incluyendo informe de auditoría `docs/CIERRE_C1_AUDITORIA_TECNICA.md`).
- **Línea base de pruebas:** Backend 116/116, Android Debug 243/243, Android Release 243/243, assembleRelease BUILD SUCCESSFUL.

---

### 2. Identificador y SHA del Stash de Seguridad

Conforme al requerimiento de protección de cambios no consolidados en commit, se creó un stash integral que incluye tanto archivos tracked como untracked:

```bash
git stash push -u -m "backup-pre-cleanup-c2-2026-11-10"
```

- **Identificador de Stash:** `stash@{0}`
- **Mensaje descriptivo:** `On feature/ux-notificaciones-centros-responsive: backup-pre-cleanup-c2-2026-11-10`
- **SHA del Stash Commit:** `ff9785a1ba0849733017b257b8ba38e071c91ab0`

---

### 3. Confirmación de Reaplicación y Preservación del Stash

Inmediatamente tras su creación, el respaldo fue reaplicado al árbol de trabajo sin eliminarlo:

```bash
git stash apply "stash@{0}"
```

- **Resultado:** 100% exitoso, 0 conflictos.
- **Verificación de persistencia:** `git stash list` confirma que `stash@{0}` permanece almacenado y disponible para cualquier necesidad de rollback o recuperación completa.

---

### 4. Archivos Eliminados del Paquete `data/`

Se retiró la totalidad del directorio intermedio legado `Android/app/src/main/java/com/example/adoptme/data/` (14 archivos tracked):
1. `data/AppStorage.kt` (duplicado consolidado hacia `repository/AppStorage.kt`)
2. `data/MascotaStorage.kt` (duplicado consolidado hacia `repository/MascotaStorage.kt`)
3. `data/PetRepository.kt` (duplicado consolidado hacia `repository/PetRepository.kt`)
4. `data/datastore/CatalogoDataStore.kt` (duplicado consolidado hacia `datastore/CatalogoDataStore.kt`)
5. `data/local/AdoptMeDatabase.kt` (duplicado consolidado hacia `database/AdoptMeDatabase.kt`)
6. `data/local/MascotaVisitadaDao.kt` (duplicado consolidado hacia `dao/MascotaVisitadaDao.kt`)
7. `data/local/MascotaVisitadaEntity.kt` (duplicado consolidado hacia `entity/MascotaVisitadaEntity.kt`)
8. `data/remote/MascotaDto.kt` (duplicado consolidado hacia `model/MascotaDto.kt`)
9. `data/remote/MascotaMapper.kt` (duplicado consolidado hacia `mapper/MascotaMapper.kt`)
10. `data/remote/MascotasApi.kt` (duplicado consolidado hacia `api/MascotasApi.kt`)
11. `data/remote/RetrofitClient.kt` (duplicado consolidado hacia `api/RetrofitClient.kt`)
12. `data/remote/SolicitudDtos.kt` (duplicado consolidado hacia `model/SolicitudDtos.kt`)
13. `data/remote/SolicitudMapper.kt` (duplicado consolidado hacia `mapper/SolicitudMapper.kt`)
14. `data/remote/SolicitudesApi.kt` y `data/remote/UsuariosApi.kt` (duplicados consolidados hacia `api/`)

---

### 5. Resultado Posterior al Bloque `data/`

- **Análisis de dependencias cruzadas:** Se detectó que el paquete legado `AdoptMe/` importaba clases de `data/remote` y `data/local`. Al tratarse de dos paquetes legados mutuamente acoplados e independientes del flujo canónico, su retiro coordinado eliminó simultáneamente las referencias cruzadas muertas.
- **Consolidación de AppStorage:** `Android/app/src/main/java/com/example/adoptme/repository/AppStorage.kt` permanece como la única implementación activa y canónica.

---

### 6. Archivos Eliminados de `SharedPreferences/`

Se retiró el paquete redundante `Android/app/src/main/java/com/example/adoptme/SharedPreferences/` (1 archivo tracked):
1. `SharedPreferences/Preferencias.kt`

---

### 7. Resultado Posterior a `SharedPreferences/`

- La aplicación canónica consume exclusivamente `com.example.adoptme.preferences.Preferencias`.
- No existía ninguna clase activa vinculada a `SharedPreferences/Preferencias.kt`.

---

### 8. Archivos Eliminados de `AdoptMe/`

Se retiró el árbol completo de pantallas legadas `Android/app/src/main/java/com/example/adoptme/AdoptMe/` (**23 archivos tracked**):
1. `BarraNavegacionInferior.kt`
2. `CentrosScreen.kt`
3. `DialogoTestCompatibilidad.kt`
4. `InsigniaEstado.kt`
5. `MisPublicacionesScreen.kt`
6. `PantallaAdministrador.kt`
7. `PantallaAdopcion.kt`
8. `PantallaCentro.kt`
9. `PantallaChat.kt`
10. `PantallaConsumoResponsable.kt`
11. `PantallaDetalleMascota.kt`
12. `PantallaExito.kt`
13. `PantallaFavoritos.kt`
14. `PantallaInicio.kt`
15. `PantallaInicioSesion.kt`
16. `PantallaNotificaciones.kt`
17. `PantallaPerfil.kt`
18. `PantallaPreferencias.kt`
19. `PantallaPrincipalAdoptante.kt`
20. `PantallaPublicarMascota.kt`
21. `PantallaRegistro.kt`
22. `TarjetaAdmin.kt`
23. `TarjetaMascota.kt`

---

### 9. Resultado Posterior a `AdoptMe/`

- **Compilación de Android:**
  ```bash
  .\gradlew.bat compileDebugKotlin testDebugUnitTest
  BUILD SUCCESSFUL in 11s (28 actionable tasks: 8 executed, 20 up-to-date)
  ```
- **Pruebas unitarias de Android:** **243 / 243 pruebas aprobadas (100%)**.
- Se verificó que ni `MainActivity.kt` ni `DetalleMascotaActivity.kt` ni `AndroidManifest.xml` requerían ninguna clase de este paquete.

---

### 10. Cambio de `mascotasAzure` a `mascotasRemotas`

En `Android/app/src/main/java/com/example/adoptme/MainActivity.kt`:
- **Líneas 139-144:**
  ```kotlin
  // ANTES:
  val mascotasAzure = RetrofitClient.mascotasApi.obtenerMascotas()
  val mascotasRemotas = mascotasAzure.map { it.toPet() }
  Log.d("ADOPTME_RETROFIT", "Consulta exitosa. Mascotas recibidas: ${mascotasAzure.size}")

  // AHORA:
  val mascotasRemotas = RetrofitClient.mascotasApi.obtenerMascotas().map { it.toPet() }
  Log.d("ADOPTME_RETROFIT", "Consulta exitosa. Mascotas recibidas: ${mascotasRemotas.size}")
  ```
- **Líneas 289-293:**
  ```kotlin
  // ANTES:
  val mascotasAzure = RetrofitClient.mascotasApi.obtenerMascotas()
  val mascotasRemotas = mascotasAzure.map { it.toPet() }
  mascotas.clear()
  mascotas.addAll(mascotasRemotas)

  // AHORA:
  val mascotasRemotas = RetrofitClient.mascotasApi.obtenerMascotas().map { it.toPet() }
  mascotas.clear()
  mascotas.addAll(mascotasRemotas)
  ```
- Se conservó intacta la constante de Google Maps `BitmapDescriptorFactory.HUE_AZURE` en `MapaAdopcionesScreen.kt`.

---

### 11. Actualización Realizada en `LEEME.txt`

Se reescribió `LEEME.txt` para reflejar con absoluta precisión la arquitectura técnica vigente:
- Reemplazo de referencias obsoletas a Azure App Service y Azure SQL por **Render** (`https://adoptme2.onrender.com/`) y **Neon PostgreSQL 16**.
- Inclusión formal de **Cloudinary** para multimedia y **Google Maps Platform** para geolocalización.
- Documentación del mecanismo opt-in de desarrollo local (`-PusarBackendLocal=true`) y aclaración explícita de que la variante Release siempre consume Render sin comandos manuales ni `adb reverse`.
- Aclaración de que ni Firebase ni Azure forman parte de la arquitectura del proyecto.
- Cero contraseñas, tokens, API keys o cadenas de conexión incluidas.

---

### 12. SQL `PROPUESTA` Eliminado

Se eliminó el archivo de diseño conceptual:
- `Backend/AdoptMe.Api/Data/Neon_UX_Centros_Suspension_PROPUESTA.sql` (borrador con tipos `VARCHAR(500)` superado por la migración definitiva).

---

### 13. SQL Definitivo Conservado

Se conservó intacto como registro histórico oficial de la base de datos:
- `Backend/AdoptMe.Api/Data/Neon_UX_Centros_Suspension.sql` (migración transaccional e idempotente con tipos `TEXT`, constraints e índice parcial aplicado exitosamente en Neon).
- Figura como untracked para su integración final en Git.

---

### 14. Referencias a Azure Restantes y Clasificación

Búsqueda global ejecutada post-limpieza:
1. `LEEME.txt`: Línea 30: `* Azure: NO forma parte de la arquitectura final del proyecto.` -> **Explicación requerida de no-uso**.
2. `Android/.../screens/MapaAdopcionesScreen.kt`: Línea 183: `BitmapDescriptorFactory.HUE_AZURE` -> **Falso positivo legítimo** (color de marcador de Google Maps).
3. Documentos de auditoría en `docs/`: Menciones contextuales del proceso de auditoría y descarte.
- **Total de dependencias, SDKs, conexiones o variables ejecutables de Azure:** **`0`**.

---

### 15. Resultado de Backend (.NET 8)

```bash
dotnet build Backend/AdoptMe.sln
Build succeeded. 0 Warning(s). 0 Error(s). Time Elapsed: 00:00:05.77

dotnet test Backend/AdoptMe.sln
Passed! - Failed: 0, Passed: 116, Skipped: 0, Total: 116, Duration: 1 s
```
- **116 / 116 pruebas aprobadas (100%)**.

---

### 16. Resultado Android Debug

```bash
.\gradlew.bat clean compileDebugKotlin testDebugUnitTest
BUILD SUCCESSFUL in 29s (29 actionable tasks: 28 executed, 1 up-to-date)
```
- **243 / 243 pruebas aprobadas (100%)**.

---

### 17. Resultado Android Release

```bash
.\gradlew.bat compileReleaseKotlin testReleaseUnitTest
BUILD SUCCESSFUL in 45s
```
- **243 / 243 pruebas aprobadas en variante Release (100%)**.

---

### 18. Resultado `assembleRelease`

```bash
.\gradlew.bat assembleRelease
BUILD SUCCESSFUL in 45s
```
- **APK generado:** `Android/app/build/outputs/apk/release/app-release-unsigned.apk`
- **Tamaño anterior (C1):** 13,895,939 bytes (~13.89 MB).
- **Tamaño actual (C2):** **13,568,259 bytes (~13.56 MB)**.
- **Optimización lograda:** Reducción neta de **~327.6 KB** de bytecode dex y metadatos no utilizados al eliminar los paquetes legados.

---

### 19. Estado Final de Git

- **Archivos tracked eliminados:** **`38`** (23 en `AdoptMe/`, 1 en `SharedPreferences/`, 14 en `data/`).
- **Archivos tracked modificados:** **`28`** (pantallas, viewmodels, controladores y `LEEME.txt`).
- **Archivos untracked conservados:** **`12`** (pruebas de UX, helper de geocodificación, script de migración SQL definitiva y documentación técnica en `docs/`).
- **`git diff --check`:** Limpio (0 errores de formato).
- **`git stash list`:** `stash@{0}` preservado íntegramente.

---

### 20. Comparación con la Línea Base

| Métrica / Aspecto | Línea Base C1 | Post-Limpieza C2 | Variación |
|---|---|---|---|
| **Pruebas Backend** | 116 / 116 | 116 / 116 | 100% preservadas |
| **Pruebas Android Debug** | 243 / 243 | 243 / 243 | 100% preservadas |
| **Pruebas Android Release** | 243 / 243 | 243 / 243 | 100% preservadas |
| **Tamaño APK Release** | 13.89 MB | 13.56 MB | **-327.68 KB (-2.36%)** |
| **Archivos de Código Android** | 73 archivos en `src/main/java` | 35 archivos en `src/main/java` | **-38 archivos legados (-52%)** |
| **Paquetes Android Duplicados** | 3 paquetes (`AdoptMe/`, `data/`, `SharedPreferences/`) | 0 paquetes duplicados | **100% consolidados** |
| **Referencias a Azure en código** | Variables `mascotasAzure` presentes | 0 variables Azure | **100% eliminadas** |

---

### 21. Riesgos o Incidencias

- **Incidencias encontradas:** Ninguna.
- **Riesgos mitigados:** Se eliminó por completo el riesgo de desincronización entre implementaciones duplicadas de `AppStorage`, reduciendo a la mitad el volumen de clases mantenibles sin alterar ningún comportamiento funcional en producción.

---

### 22. Decisión Final

# **APTO PARA IDENTIDAD VISUAL**

El repositorio se encuentra limpio de código muerto, debidamente respaldado en stash, con su arquitectura canónica consolidada y con todas las suites de prueba en verde. Está listo para proceder con la fase de identidad visual (ícono launcher adaptativo y recursos gráficos).
