# INFORME TÉCNICO DE AUDITORÍA — FASE C1
## Auditoría de Cierre Técnico Previa a Limpieza e Integración

**Fecha de ejecución:** 10 de noviembre de 2026  
**Proyecto:** AdoptMe — Plataforma de Adopción Responsable de Mascotas  
**Repositorio local:** `D:\Escritorio D\T2_GRUPO3_ DESARROLLO_APP\AdoptMe2_Entrega4_Git`  
**Rama activa:** `feature/ux-notificaciones-centros-responsive`  
**Base SHA esperada y confirmada:** `da712812c8cf3d4975bcab671cbfa807cab77057` (`origin/main`)  
**Backend de producción:** `https://adoptme2.onrender.com/` (Render Web Service)  
**Base de datos relacional:** Neon PostgreSQL 16 (con migración UX-B3 aplicada e integridad preservada)  
**Almacenamiento multimedia:** Cloudinary  
**Servicios de geolocalización:** Google Maps Platform  

---

### 1. Resumen Ejecutivo

La presente auditoría técnica constituye el punto de control estricto e inspección no destructiva previo a cualquier tarea de limpieza, consolidación o integración del proyecto AdoptMe. 

Durante las fases anteriores (`UX-A`, `UX-B1`, `UX-B2` y `UX-B3`), se implementaron con éxito:
- Limpieza y estado leído/no leído de notificaciones y mensajes locales con badges numéricos en barras de navegación.
- Geolocalización inteligente con autocompletado de dirección física y distrito desde el mapa (Google Maps) y taxonomía de errores de red.
- Rediseño responsivo y panel operativo del Centro con KPIs dinámicos, scroll adaptativo y alto contraste accesible.
- Máquina de estados completa para Centros de Adopción, distinguiendo entre **Solicitud Inicial Rechazada**, **Centro Aprobado Suspendido Operativamente** (con motivo obligatorio y auditoría de admin), **Centro Inactivo Heredado** y **Vinculación Responsable 1:1**.

La base de datos en Neon PostgreSQL se encuentra migrada de manera idempotente conservando **exactamente las 18 mascotas históricas** y los **6 centros registrados**. El backend ASP.NET Core 8 cuenta con 116 pruebas aprobadas (100%), y Android cuenta con 243 pruebas aprobadas (100%) tanto en Debug como en Release, ensamblando exitosamente el APK final (`app-release-unsigned.apk`, 13.89 MB).

No se ha realizado ninguna eliminación, refactorización, commit, push, PR ni despliegue en esta fase.

---

### 2. Estado Real de Git

```
$ git status --short --branch
## feature/ux-notificaciones-centros-responsive...origin/main
 M Android/app/src/main/java/com/example/adoptme/AdoptMe/BarraNavegacionInferior.kt
 M Android/app/src/main/java/com/example/adoptme/MainActivity.kt
 M Android/app/src/main/java/com/example/adoptme/api/CentrosApi.kt
 M Android/app/src/main/java/com/example/adoptme/api/RetrofitClient.kt
 M Android/app/src/main/java/com/example/adoptme/components/BarraNavegacionInferior.kt
 M Android/app/src/main/java/com/example/adoptme/components/DialogoSelectorMapa.kt
 M Android/app/src/main/java/com/example/adoptme/data/AppStorage.kt
 M Android/app/src/main/java/com/example/adoptme/model/CentroModels.kt
 M Android/app/src/main/java/com/example/adoptme/model/MensajeChat.kt
 M Android/app/src/main/java/com/example/adoptme/model/NotificationItem.kt
 M Android/app/src/main/java/com/example/adoptme/repository/AppStorage.kt
 M Android/app/src/main/java/com/example/adoptme/screens/MisPublicacionesScreen.kt
 M Android/app/src/main/java/com/example/adoptme/screens/PantallaAdministrador.kt
 M Android/app/src/main/java/com/example/adoptme/screens/PantallaCentro.kt
 M Android/app/src/main/java/com/example/adoptme/screens/PantallaEstadoCentro.kt
 M Android/app/src/main/java/com/example/adoptme/screens/PantallaNotificaciones.kt
 M Android/app/src/main/java/com/example/adoptme/screens/PantallaPrincipalAdoptante.kt
 M Android/app/src/main/java/com/example/adoptme/screens/PantallaPublicarMascota.kt
 M Android/app/src/main/java/com/example/adoptme/screens/PantallaRegistro.kt
 M Android/app/src/main/java/com/example/adoptme/viewmodel/AdminCentrosViewModel.kt
 M Backend/AdoptMe.Api.Tests/CentrosDinamicosTests.cs
 M Backend/AdoptMe.Api/Controllers/AdminCentrosController.cs
 M Backend/AdoptMe.Api/Controllers/CentrosController.cs
 M Backend/AdoptMe.Api/Data/AdoptMeDbContext.cs
 M Backend/AdoptMe.Api/Models/Centro.cs
 M Backend/AdoptMe.Api/Models/CentroAdminDto.cs
 M Backend/AdoptMe.Api/Models/CentroPropietarioDto.cs
 M Backend/AdoptMe.Api/Services/CentroAuthorizationService.cs
?? Android/app/src/main/java/com/example/adoptme/utils/ReverseGeocodingHelper.kt
?? Android/app/src/test/java/com/example/adoptme/GeolocalizacionYPanelCentroTest.kt
?? Android/app/src/test/java/com/example/adoptme/NotificacionesYMensajesNoLeidosTest.kt
?? Android/app/src/test/java/com/example/adoptme/SuspensionYVinculacionCentroTest.kt
?? Backend/AdoptMe.Api.Tests/CentrosSuspensionTests.cs
?? Backend/AdoptMe.Api/Data/Neon_UX_Centros_Suspension.sql
?? Backend/AdoptMe.Api/Data/Neon_UX_Centros_Suspension_PROPUESTA.sql
?? Backend/AdoptMe.Api/Models/SuspenderCentroDto.cs
?? docs/UX_A_AUDITORIA_NOTIFICACIONES_CENTROS_RESPONSIVE.md
?? docs/UX_B1_NOTIFICACIONES_MENSAJES_NO_LEIDOS.md
?? docs/UX_B2_MAPA_PANEL_CENTRO_RESPONSIVE.md
?? docs/UX_B3_SUSPENSION_CENTROS.md
```

- **Rama activa:** `feature/ux-notificaciones-centros-responsive`
- **SHA de HEAD:** `da712812c8cf3d4975bcab671cbfa807cab77057`
- **SHA de origin/main:** `da712812c8cf3d4975bcab671cbfa807cab77057`
- **Base esperada:** Idéntica a `da712812c8cf3d4975bcab671cbfa807cab77057` (sin divergencia de commits remotos o no integrados).
- **Archivos modificados (tracked):** 28 archivos (20 en Android, 8 en Backend).
- **Archivos nuevos no rastreados (untracked):** 12 archivos (4 de código/pruebas Android, 4 de Backend/SQL, 4 de documentación técnica en `docs/`).
- **Archivos eliminados:** 0.
- **Archivos en stage (staged):** 0.
- **Commits locales pendientes de push:** 0.
- **Archivos generados accidentalmente:** 0 (directorios de compilación `build/`, `bin/`, `obj/` permanecen debidamente excluidos por `.gitignore`).

---

### 3. Inventario del Diff Acumulado

| Archivo | Estado Git | Fase Probable | Responsabilidad / Símbolos | Riesgo | Acción Recomendada |
|---|---|---|---|---|---|
| `Android/.../repository/AppStorage.kt` | Modificado | UX-B1 | Gestión local SharedPreferences: notificaciones leídas, conteo por conversación, aislamiento por cuenta. | Bajo | Conservar como fuente canónica activa. |
| `Android/.../data/AppStorage.kt` | Modificado | UX-B1 | Duplicado de `repository/AppStorage.kt`. | Medio | Retirar en fase de limpieza. |
| `Android/.../model/NotificationItem.kt` | Modificado | UX-B1 | Campo `isRead: Boolean = false`. | Bajo | Conservar. |
| `Android/.../model/MensajeChat.kt` | Modificado | UX-B1 | Campos `esLeido: Boolean = false`, `destinatarioId`. | Bajo | Conservar. |
| `Android/.../screens/PantallaNotificaciones.kt` | Modificado | UX-B1 | UI de notificaciones con badges, marcado como leída y vaciado. | Bajo | Conservar. |
| `Android/.../components/BarraNavegacionInferior.kt` | Modificado | UX-B1 | Badge numérico reactivo sobre pestaña de notificaciones. | Bajo | Conservar. |
| `Android/.../AdoptMe/BarraNavegacionInferior.kt` | Modificado | UX-B1 | Duplicado legado de la barra de navegación. | Medio | Retirar al retirar paquete `AdoptMe`. |
| `Android/.../screens/PantallaPrincipalAdoptante.kt` | Modificado | UX-B1 | Inyección de badge en barra inferior de adoptante. | Bajo | Conservar. |
| `Android/.../utils/ReverseGeocodingHelper.kt` | No rastreado | UX-B2 | Helper de geocodificación inversa limpia para dirección y distrito. | Bajo | Conservar y versionar. |
| `Android/.../components/DialogoSelectorMapa.kt` | Modificado | UX-B2 | Autocompletado de dirección y distrito desde coordenadas. | Bajo | Conservar. |
| `Android/.../screens/PantallaCentro.kt` | Modificado | UX-B2 | Rediseño completo del panel del Centro, métricas y contraste. | Bajo | Conservar. |
| `Android/.../screens/PantallaRegistro.kt` | Modificado | UX-B2 | Llenado automático de dirección y distrito de centro. | Bajo | Conservar. |
| `Android/.../screens/PantallaPublicarMascota.kt` | Modificado | UX-B2 | Llenado automático de ubicación de mascota. | Bajo | Conservar. |
| `Android/.../screens/MisPublicacionesScreen.kt` | Modificado | UX-B2 | Contraste tipográfico y layout responsivo. | Bajo | Conservar. |
| `Android/.../api/RetrofitClient.kt` | Modificado | UX-B2 | Taxonomía de errores `CategoriaErrorConexion` y resolución de red. | Bajo | Conservar. |
| `Backend/.../Models/Centro.cs` | Modificado | UX-B3 | Propiedades de auditoría de suspensión y reactivación. | Bajo | Conservar. |
| `Backend/.../Models/SuspenderCentroDto.cs` | No rastreado | UX-B3 | DTO de entrada para suspensión con validación de motivo >= 5. | Bajo | Conservar y versionar. |
| `Backend/.../Models/CentroAdminDto.cs` | Modificado | UX-B3 | Mapeo de campos de suspensión y adminId. | Bajo | Conservar. |
| `Backend/.../Models/CentroPropietarioDto.cs` | Modificado | UX-B3 | Mapeo de motivo y fecha de suspensión. | Bajo | Conservar. |
| `Backend/.../Data/AdoptMeDbContext.cs` | Modificado | UX-B3 | Fluent API para relación `SuspendidoPorAdmin`. | Bajo | Conservar. |
| `Backend/.../Controllers/AdminCentrosController.cs` | Modificado | UX-B3 | Endpoints suspender, reactivar, rechazar (protegido), vincular. | Bajo | Conservar. |
| `Backend/.../Controllers/CentrosController.cs` | Modificado | UX-B3 | Bloqueo 403 de mutaciones si centro inactivo/suspendido. | Bajo | Conservar. |
| `Backend/.../Services/CentroAuthorizationService.cs` | Modificado | UX-B3 | Mensaje descriptivo de rechazo o suspensión operativa. | Bajo | Conservar. |
| `Backend/.../CentrosDinamicosTests.cs` | Modificado | UX-B3 | Actualización de Test18f para validar 409 al sobrescribir titular. | Bajo | Conservar. |
| `Backend/.../CentrosSuspensionTests.cs` | No rastreado | UX-B3 | 32 pruebas unitarias de backend para suspensión y vinculación. | Bajo | Conservar y versionar. |
| `Backend/.../Neon_UX_Centros_Suspension.sql` | No rastreado | UX-B3 | Migración SQL definitiva e idempotente ejecutada en Neon. | Bajo | Conservar como registro oficial. |
| `Backend/.../Neon_UX_Centros_Suspension_PROPUESTA.sql`| No rastreado | UX-B3 | Propuesta inicial de diseño de migración. | Bajo | Archivar o retirar en limpieza. |
| `Android/.../model/CentroModels.kt` | Modificado | UX-B3 | DTOs Android de suspensión y `SuspenderCentroRequest`. | Bajo | Conservar. |
| `Android/.../api/CentrosApi.kt` | Modificado | UX-B3 | Interfaces Retrofit suspender, reactivar, vincular. | Bajo | Conservar. |
| `Android/.../viewmodel/AdminCentrosViewModel.kt` | Modificado | UX-B3 | Métodos de suspensión, reactivación y vinculación. | Bajo | Conservar. |
| `Android/.../screens/PantallaEstadoCentro.kt` | Modificado | UX-B3 | Renderizado de `TarjetaEstadoSuspendido` vs `TarjetaEstadoInactivo`. | Bajo | Conservar. |
| `Android/.../screens/PantallaAdministrador.kt` | Modificado | UX-B3 | Acciones de suspender, reactivar y vincular (1:1). | Bajo | Conservar. |
| `Android/.../MainActivity.kt` | Modificado | UX-B3 | Limpieza reactiva de notificaciones al iniciar sesión. | Bajo | Conservar. |
| `Android/.../NotificacionesYMensajesNoLeidosTest.kt`| No rastreado | UX-B1 | 15 pruebas unitarias de notificaciones y chat. | Bajo | Conservar y versionar. |
| `Android/.../GeolocalizacionYPanelCentroTest.kt`| No rastreado | UX-B2 | 17 pruebas unitarias de geocodificación y errores. | Bajo | Conservar y versionar. |
| `Android/.../SuspensionYVinculacionCentroTest.kt`| No rastreado | UX-B3 | 21 pruebas unitarias de suspensión y vinculación. | Bajo | Conservar y versionar. |
| `docs/UX_A_...md` | No rastreado | UX-A | Documento de diseño y auditoría inicial. | Bajo | Conservar en docs/. |
| `docs/UX_B1_...md` | No rastreado | UX-B1 | Documento de entrega UX-B1. | Bajo | Conservar en docs/. |
| `docs/UX_B2_...md` | No rastreado | UX-B2 | Documento de entrega UX-B2. | Bajo | Conservar en docs/. |
| `docs/UX_B3_...md` | No rastreado | UX-B3 | Documento de entrega UX-B3. | Bajo | Conservar en docs/. |

---

### 4. Mapa de Duplicados y Referencias Cruzadas

La inspección estática y de dependencias revela que el proyecto alberga tres familias principales de duplicación originadas en la transición histórica entre entregas previas y la arquitectura actual:

```
                            ARQUITECTURA DE CÓDIGO ANDROID
                                          │
            ┌─────────────────────────────┼─────────────────────────────┐
            ▼                             ▼                             ▼
   [ PAQUETE CANÓNICO ]         [ PAQUETE DATA LEGADO ]       [ PAQUETE ADOPTME LEGADO ]
   (En uso activo real)         (Copia intermedia antigua)    (23 pantallas antiguas)
   ├── screens/                 ├── data/local/               ├── AdoptMe/PantallaInicio
   ├── components/              ├── data/remote/              ├── AdoptMe/PantallaAdmin
   ├── repository/              ├── data/datastore/           ├── AdoptMe/PantallaCentro
   ├── mapper/                  ├── data/AppStorage.kt        └── AdoptMe/... (20 más)
   ├── model/                   └── data/MascotaStorage.kt
   ├── api/
   ├── database/
   └── datastore/
```

#### Tabla de Equivalencias y Estado de Uso:

| Componente Funcional | Archivo Canónico Activo | Archivo Duplicado Legado | ¿Quién importa el legado? | Estado |
|---|---|---|---|---|
| **Almacenamiento Local** | `repository/AppStorage.kt` | `data/AppStorage.kt` | Únicamente `AdoptMe/PantallaPrincipalAdoptante.kt` | Duplicado exacto. |
| **Almacenamiento Mascotas** | `repository/MascotaStorage.kt` | `data/MascotaStorage.kt` | Únicamente `AdoptMe/PantallaPrincipalAdoptante.kt` | Duplicado exacto. |
| **Repositorio Mascotas** | `repository/PetRepository.kt` | `data/PetRepository.kt` | Nadie (inutilizado). | Duplicado inactivo. |
| **DataStore Catálogo** | `datastore/CatalogoDataStore.kt` | `data/datastore/CatalogoDataStore.kt` | Únicamente `AdoptMe/PantallaPrincipalAdoptante.kt` | Duplicado exacto. |
| **Mapeador Mascotas** | `mapper/MascotaMapper.kt` | `data/remote/MascotaMapper.kt` | Únicamente `AdoptMe/PantallaPublicarMascota.kt` | Legado incompleto (sin Cloudinary ni GPS). |
| **API Mascotas** | `api/MascotasApi.kt` | `data/remote/MascotasApi.kt` | Nadie fuera de `data/`. | Duplicado inactivo. |
| **API Solicitudes** | `api/SolicitudesApi.kt` | `data/remote/SolicitudesApi.kt` | Nadie fuera de `data/`. | Duplicado inactivo. |
| **API Usuarios** | `api/UsuariosApi.kt` | `data/remote/UsuariosApi.kt` | Nadie fuera de `data/`. | Duplicado inactivo. |
| **Retrofit Client** | `api/RetrofitClient.kt` | `data/remote/RetrofitClient.kt` | Solo archivos de `AdoptMe/`. | Legado incompleto (sin endpoints de centros). |
| **DTOs Solicitudes** | `model/SolicitudDtos.kt` | `data/remote/SolicitudDtos.kt` | Solo archivos de `AdoptMe/`. | Duplicado inactivo. |
| **Mapeador Solicitudes** | `mapper/SolicitudMapper.kt` | `data/remote/SolicitudMapper.kt` | Solo archivos de `AdoptMe/`. | Duplicado inactivo. |
| **Room Database** | `database/AdoptMeDatabase.kt` | `data/local/AdoptMeDatabase.kt` | Nadie fuera de `data/`. | Duplicado inactivo. |
| **Room DAO** | `dao/MascotaVisitadaDao.kt` | `data/local/MascotaVisitadaDao.kt` | Nadie fuera de `data/`. | Duplicado inactivo. |
| **Room Entity** | `entity/MascotaVisitadaEntity.kt` | `data/local/MascotaVisitadaEntity.kt` | Solo archivos de `AdoptMe/`. | Duplicado inactivo. |
| **Preferencias SharedPreferences** | `preferences/Preferencias.kt` | `SharedPreferences/Preferencias.kt` | Solo archivos de `AdoptMe/` y `data/`. | Duplicado legado. |
| **Barra Navegación** | `components/BarraNavegacionInferior.kt` | `AdoptMe/BarraNavegacionInferior.kt` | Nadie. | Duplicado en `AdoptMe/`. |
| **Pantalla Admin** | `screens/PantallaAdministrador.kt` | `AdoptMe/PantallaAdministrador.kt` | Nadie. | Duplicado en `AdoptMe/`. |
| **Pantalla Centro** | `screens/PantallaCentro.kt` | `AdoptMe/PantallaCentro.kt` | Nadie. | Duplicado en `AdoptMe/`. |
| **Pantalla Publicar** | `screens/PantallaPublicarMascota.kt` | `AdoptMe/PantallaPublicarMascota.kt` | Nadie. | Duplicado en `AdoptMe/`. |
| **Pantalla Mis Publicaciones**| `screens/MisPublicacionesScreen.kt` | `AdoptMe/MisPublicacionesScreen.kt` | Nadie. | Duplicado en `AdoptMe/`. |

---

### 5. Auditoría Exhaustiva de `AppStorage`

#### A. Archivos Analizados
- `Android/app/src/main/java/com/example/adoptme/repository/AppStorage.kt`
- `Android/app/src/main/java/com/example/adoptme/data/AppStorage.kt`

#### B. Quién Importa Cada Versión
- **`com.example.adoptme.repository.AppStorage`:**
  - `MainActivity.kt` (línea 35): Inicializa `almacenamientoApp` para toda la aplicación y lo inyecta a la navegación.
  - `screens/PantallaPrincipalAdoptante.kt` (línea 30): Recibe `almacenamientoApp` para contar no leídos y suscribirse a notificaciones.
  - `NotificacionesYMensajesNoLeidosTest.kt` (línea 8): Suite de 15 pruebas unitarias que valida almacenamiento, marcado como leídas, aislamiento y borrado.
- **`com.example.adoptme.data.AppStorage`:**
  - `AdoptMe/PantallaPrincipalAdoptante.kt` (línea 14): Pantalla del paquete legado no referenciada por `MainActivity`.

#### C. Comparativa de Métodos y Claves
El diff entre ambos archivos arroja únicamente diferencias cosméticas de import (`model.UsuarioRegistrado` vs `AdoptMe.UsuarioRegistrado`) y el paquete declarado.
Ambas clases:
- Utilizan el mismo archivo SharedPreferences: `"adoptme_app_storage"`.
- Utilizan idénticas claves y esquemas de persistencia JSON:
  - `notificaciones_$correo`
  - `chat_${correo}_centro_${idCentro}`
  - `conversaciones_centro_$correoCentro`
- Contienen los mismos métodos: `guardarNotificaciones`, `cargarNotificaciones`, `contarNotificacionesNoLeidas`, `marcarNotificacionComoLeida`, `marcarTodasNotificacionesComoLeidas`, `eliminarNotificacion`, `limpiarNotificacionesPrueba`, `guardarMensajesChat`, `cargarMensajesChat`, `contarMensajesNoLeidosConversacion`, `contarMensajesNoLeidosAdoptante`, `contarMensajesNoLeidosCentro`, etc.

#### D. Riesgo de Pérdida de Datos y Estrategia
- **Riesgo:** Nulo. Al compartir el mismo archivo SharedPreferences y las mismas claves, cualquier dato guardado por una versión es leído transparentemente por la otra.
- **Estrategia de consolidación:**
  1. Conservar `com.example.adoptme.repository.AppStorage` como la única implementación oficial.
  2. Al retirar el paquete `AdoptMe`, la referencia residual en `AdoptMe/PantallaPrincipalAdoptante.kt` desaparecerá naturalmente.
  3. Eliminar `Android/app/src/main/java/com/example/adoptme/data/AppStorage.kt`.

---

### 6. Auditoría del Paquete Legado (`AdoptMe/`)

El directorio `Android/app/src/main/java/com/example/adoptme/AdoptMe` contiene **23 archivos**:
`BarraNavegacionInferior.kt`, `CentrosScreen.kt`, `DialogoTestCompatibilidad.kt`, `InsigniaEstado.kt`, `MisPublicacionesScreen.kt`, `PantallaAdministrador.kt`, `PantallaAdopcion.kt`, `PantallaCentro.kt`, `PantallaChat.kt`, `PantallaConsumoResponsable.kt`, `PantallaDetalleMascota.kt`, `PantallaExito.kt`, `PantallaFavoritos.kt`, `PantallaInicio.kt`, `PantallaInicioSesion.kt`, `PantallaNotificaciones.kt`, `PantallaPerfil.kt`, `PantallaPreferencias.kt`, `PantallaPrincipalAdoptante.kt`, `PantallaPublicarMascota.kt`, `PantallaRegistro.kt`, `TarjetaAdmin.kt`, `TarjetaMascota.kt`.

#### Análisis de Referencias y Uso Real:
1. **Puntos de entrada (`MainActivity.kt` y `DetalleMascotaActivity.kt`):**
   - No importan ningún archivo del paquete `AdoptMe`.
   - Navegan exclusivamente hacia clases en `screens.*` y `components.*`.
2. **`AndroidManifest.xml`:**
   - No declara ninguna actividad en `AdoptMe.*`.
3. **Pruebas unitarias (`Android/app/src/test`):**
   - Ninguna de las 243 pruebas unitarias importa ni referencia clases de `AdoptMe`.
4. **Dependencias internas cruzadas:**
   - La función `formatearEdadCanonica` en `AdoptMe/PantallaPublicarMascota.kt` era importada por `data/remote/MascotaMapper.kt`. Sin embargo, `data/remote/MascotaMapper.kt` tampoco es utilizada por la aplicación real (la cual consume `utils.formatearEdadCanonica` en `utils/EdadFormatter.kt`).
   - `AdoptMe/PantallaPerfil.kt` declara `data class UsuarioRegistrado`, la cual era importada por `data/AppStorage.kt`. La aplicación activa utiliza `com.example.adoptme.model.UsuarioRegistrado`.

#### Conclusión del Paquete Legado:
El paquete `AdoptMe/` es código muerto generado en entregas tempranas y completamente desacoplado del flujo productivo. Se compila únicamente porque Gradle incluye todo el árbol de fuentes, pero no interviene en la ejecución. Su eliminación es viable y segura, sujeta al orden planificado.

---

### 7. Inventario de Referencias a Azure

Búsqueda exhaustiva en todo el repositorio (excluyendo carpetas gitignored):

| Ubicación | Línea | Contexto Exacto | Clasificación | Acción Recomendada |
|---|---|---|---|---|---|
| `LEEME.txt` | 23, 33, 41, 43, 44, 47, 50, 64, 72, 73, 75, 97 | URLs históricas de Azure App Service y Azure SQL de la Entrega 1. | 5. Documentación histórica | Actualizar o reemplazar `LEEME.txt` con la arquitectura vigente (Render + Neon). |
| `Android/.../MainActivity.kt` | 139, 144, 289 | `val mascotasAzure = RetrofitClient.mascotasApi.obtenerMascotas()` | 1. Código ejecutable activo (residuo de nomenclatura) | Renombrar variable local a `mascotasRemotas` o `mascotasApi`. No afecta funcionalidad. |
| `Android/.../screens/MapaAdopcionesScreen.kt` | 183 | `BitmapDescriptorFactory.HUE_AZURE` | 7. Falso positivo | **Conservar intacto**. Es una constante pública de Google Maps para el color celeste/azul del marcador. |

- **Dependencias Azure (SDKs / paquetes NuGet):** 0 instalados.
- **Cadenas de conexión a Azure:** 0 presentes en código fuente o configuración.
- **Endpoints de Azure activos:** 0. Toda la aplicación consume `https://adoptme2.onrender.com/`.

---

### 8. Arquitectura Activa Confirmada

```
                                ARQUITECTURA OFICIAL ADOPTME
                                
   ┌──────────────────────────────────────────────────────────────────────────┐
   │                          CLIENTE ANDROID                                 │
   │  - Kotlin + Jetpack Compose                                              │
   │  - applicationId: "com.example.adoptme" (Nombre: "AdoptMe")              │
   │  - Retrofit Client -> https://adoptme2.onrender.com/ (Render Producción) │
   │  - Base de datos local Room (MascotaVisitada) + DataStore + SharedPreferences │
   │  - Notificaciones y Mensajería local aislada por usuario                 │
   └────────────────────────┬─────────────────────────────────────────────────┘
                            │ HTTPS / REST API / JWT
                            ▼
   ┌──────────────────────────────────────────────────────────────────────────┐
   │                       BACKEND EN LA NUBE (RENDER)                        │
   │  - ASP.NET Core 8 Web API                                                │
   │  - Swagger / OpenAPI                                                     │
   │  - Autenticación JWT Bearer Multirol (Adoptante, Centro, Admin)          │
   └───────────────┬──────────────────────────────┬───────────────────────────┘
                   │                              │
                   ▼                              ▼
   ┌──────────────────────────────┐ ┌─────────────────────────────────────────┐
   │    NEON POSTGRESQL 16        │ │            CLOUDINARY                   │
   │  - Centros Dinámicos         │ │  - Almacenamiento seguro persistente    │
   │  - Mascotas (18 históricas)  │ │  - Subida controlada desde backend      │
   │  - Solicitudes y Usuarios    │ │  - Transformación y CDN de imágenes     │
   │  - Migración UX-B3 aplicada  │ └─────────────────────────────────────────┘
   └──────────────────────────────┘
```

- **Firebase:** No utilizado ni configurado.
- **Azure:** No forma parte de la arquitectura productiva.
- **Modo Local:** Solo opt-in explícito mediante `-PusarBackendLocal=true`. Por defecto, Android Studio ejecuta directo contra Render.

---

### 9. Estado de Configuración y Secretos

#### Verificación de Archivos y Exclusiones:
1. `Android/app/build.gradle.kts`:
   - `applicationId`: `"com.example.adoptme"` (estrictamente respetado).
   - `BASE_URL`: `"https://adoptme2.onrender.com/"` inyectada en `defaultConfig`, `release` y `debug` por defecto.
   - Google Maps API Key cargada desde `secrets.properties` mediante `com.google.android.libraries.mapsplatform.secrets-gradle-plugin`.
2. `Android/secrets.defaults.properties`:
   - Versionado en Git. Contiene únicamente plantilla segura: `MAPS_API_KEY=REEMPLAZAR`.
3. `Android/secrets.properties`:
   - **Ignorado por Git** (`!! Android/secrets.properties` confirmado por `git status --ignored`). No versionado.
4. `Android/local.properties`:
   - **Ignorado por Git** (`!! Android/local.properties` confirmado por `git status --ignored`). No versionado.
5. `Backend/AdoptMe.Api/appsettings.json`:
   - Versionado con `"DefaultConnection": ""` vacía. Cero credenciales expuestas en Git.
6. `.gitignore` raíz y submódulos:
   - Cobertura exhaustiva de secretos (`secrets.json`, `secrets.properties`, `.env`), binarios (`*.dll`, `*.apk`, `*.aab`), carpetas de compilación (`bin/`, `obj/`, `build/`, `.gradle/`) y configuración local de IDEs (`.idea/`, `.vscode/`).

---

### 10. Estado de Identidad Visual y Recursos Android

#### A. Recursos Existentes en `Android/app/src/main/res/drawable`:
- `adoptme_logo_original.png` (1.12 MB, imagen de huella institucional y corazón). **Presente en el repositorio**.
- `adoptme_dog_cat_original.png` (1.78 MB, ilustración de perro y gato con corazón). **Presente en el repositorio**.
- Ambas imágenes se utilizan actualmente dentro de pantallas: `PantallaInicioSesion.kt`, `PantallaRegistro.kt`, `PantallaInicio.kt`, `PantallaAdministrador.kt`, `MapaAdopcionesScreen.kt` y `PantallaConsumoResponsable.kt`.

#### B. Estado del Ícono del Launcher (`ic_launcher`):
- `res/mipmap-anydpi-v26/ic_launcher.xml` e `ic_launcher_round.xml` apuntan a `@drawable/ic_launcher_foreground` y `@drawable/ic_launcher_background`.
- `res/drawable/ic_launcher_foreground.xml` contiene actualmente el vector predeterminado de Android (robot verde de Android).
- Las carpetas `mipmap-hdpi`, `mipmap-mdpi`, `mipmap-xhdpi`, `mipmap-xxhdpi`, `mipmap-xxxhdpi` contienen webp con el robot de Android.
- **Diagnóstico:** El ícono del lanzador no refleja aún la identidad visual de AdoptMe.

#### C. Riesgos Identificados y Recomendaciones de Diseño:
- **Riesgo de recorte por máscara adaptativa (squircle / círculo):** Los íconos adaptativos de Android requieren una zona segura central de 66dp de diámetro dentro del canvas total de 108dp. Si se coloca una imagen con texto cerca de los bordes, el texto o las orejas de las mascotas serán recortados en dispositivos Samsung, Pixel o Xiaomi.
- **Estrategia de integración propuesta (para la fase correspondiente):**
  1. Generar un `ic_launcher_foreground.xml` o PNG centrado con el logotipo de huella (`adoptme_logo_original`) dentro del área segura de 66dp sobre un fondo cálido (`#FFF8F0` o `#FF6B6B`).
  2. Mantener `adoptme_dog_cat_original.png` como arte de bienvenida en `PantallaInicioSesion` y Splash.

---

### 11. Estado de SQL y Migraciones

En `Backend/AdoptMe.Api/Data` se identificaron 7 scripts SQL:
1. `Neon_Tablas_AdoptMe.sql`: Script inicial de creación de tablas.
2. `Neon_Fase2B_Solicitudes.sql`: Migración de solicitudes de adopción.
3. `Neon_Fase4_Coordenadas_Mascotas.sql`: Adición de columnas de latitud y longitud.
4. `Neon_Fase5_ImagePublicId_Mascotas.sql`: Adición de columna `ImagePublicId`.
5. `Neon_Fase6_Centros_Dinamicos.sql`: Creación de centros dinámicos, claves foráneas e índices.
6. `Neon_UX_Centros_Suspension_PROPUESTA.sql`: Propuesta conceptual redactada durante la Fase UX-A. Utilizaba `VARCHAR(500)` para `MotivoSuspension`.
7. `Neon_UX_Centros_Suspension.sql`: **MIGRACIÓN DEFINITIVA APLICADA EN NEON**.
   - Idempotente (`IF NOT EXISTS` en columnas, constraints e índice parcial).
   - Utiliza `TEXT` para `MotivoSuspension` (alineado con EF Core).
   - Configura `FK_Centros_Usuarios_SuspendidoPorAdminId`, `CK_Centros_Suspension_Consistencia` e `IX_Centros_Suspension`.
   - **No contiene credenciales ni datos sensibles.**

#### Recomendación:
Conservar `Neon_UX_Centros_Suspension.sql` como el registro histórico canónico de la migración de base de datos. `Neon_UX_Centros_Suspension_PROPUESTA.sql` puede ser archivado o eliminado durante la limpieza para evitar confusiones operativas.

---

### 12. Artefactos y Archivos que NO Deben Integrarse

Los siguientes archivos detectados en la auditoría deben ser excluidos del commit o limpiados antes del Pull Request:

1. `Backend/AdoptMe.Api/Data/Neon_UX_Centros_Suspension_PROPUESTA.sql`: Borrador redundante superado por `Neon_UX_Centros_Suspension.sql`.
2. Archivos en `data/` duplicados:
   - `Android/app/src/main/java/com/example/adoptme/data/AppStorage.kt`
   - `Android/app/src/main/java/com/example/adoptme/data/MascotaStorage.kt`
   - `Android/app/src/main/java/com/example/adoptme/data/PetRepository.kt`
   - `Android/app/src/main/java/com/example/adoptme/data/local/*`
   - `Android/app/src/main/java/com/example/adoptme/data/remote/*`
   - `Android/app/src/main/java/com/example/adoptme/data/datastore/*`
3. Paquete legado completo:
   - `Android/app/src/main/java/com/example/adoptme/AdoptMe/*` (23 archivos muertos).
   - `Android/app/src/main/java/com/example/adoptme/SharedPreferences/*` (1 archivo muerto).
4. Binarios de validación generados localmente:
   - `Android/app/build/outputs/apk/release/app-release-unsigned.apk` (correctamente ignorado por `.gitignore`, no debe forzarse su inclusión con `git add -f`).

---

### 13. Resultado Real de Validaciones y Pruebas

#### A. Backend ASP.NET Core 8
- **Compilación:**
  ```
  dotnet build Backend/AdoptMe.sln
  Build succeeded. 0 Warning(s). 0 Error(s). Time Elapsed: 00:00:02.91
  ```
- **Pruebas unitarias:**
  ```
  dotnet test Backend/AdoptMe.sln
  Passed! - Failed: 0, Passed: 116, Skipped: 0, Total: 116, Duration: 923 ms
  ```
  - `CentrosDinamicosTests`: 84 pruebas aprobadas.
  - `CentrosSuspensionTests`: 32 pruebas aprobadas.

#### B. Android Kotlin & Jetpack Compose
- **Compilación y Pruebas Debug:**
  ```
  .\gradlew.bat compileDebugKotlin testDebugUnitTest
  BUILD SUCCESSFUL in 3s (28 actionable tasks up-to-date)
  ```
  - Pruebas totales ejecutadas: **243 pruebas aprobadas (0 fallos, 0 errores, 0 omitidas)**.
- **Compilación y Pruebas Release:**
  ```
  .\gradlew.bat compileReleaseKotlin testReleaseUnitTest assembleRelease
  BUILD SUCCESSFUL in 3s (57 actionable tasks)
  ```
  - Pruebas Release: **243 pruebas aprobadas**.
  - Artefacto generado: `Android/app/build/outputs/apk/release/app-release-unsigned.apk` (tamaño: **13,895,939 bytes ~ 13.89 MB**).

---

### 14. Matriz de Clasificación de Riesgos

| Riesgo | Nivel | Causa Raíz | Mitigación Planificada |
|---|---|---|---|
| **Pérdida accidental de cambios acumulados** | **ALTO** | Gran cantidad de archivos modificados (28) y no rastreados (12) en el árbol de trabajo sin commit. | Crear un stash o rama de respaldo local antes de cualquier borrado de paquetes legados. |
| **Rompimiento de compilación por eliminación de paquetes legados** | **MEDIO** | Dependencia residual de `data/remote/MascotaMapper.kt` hacia `AdoptMe/PantallaPublicarMascota.kt`. | Retirar `AdoptMe/` y `data/` en un orden secuencial verificado paso a paso con `compileDebugKotlin`. |
| **Recorte de ícono en launcher** | **MEDIO** | Inclusión de textos o logos no adaptados al área segura de 66dp de Android. | Ajustar el viewport del vector o generar densidades respetando la zona segura. |
| **Confusión entre script de migración SQL propuesto y definitivo** | **BAJO** | Coexistencia de dos archivos de suspensión en `Data/`. | Archivar/eliminar el borrador `PROPUESTA.sql` y conservar únicamente `Neon_UX_Centros_Suspension.sql`. |
| **Residuos de nomenclatura de Azure** | **BAJO** | Variables locales `mascotasAzure` en `MainActivity.kt`. | Renombrar variables cosméticamente a `mascotasRemotas`. |
| **Riesgos Bloqueantes** | **NINGUNO** | La base de datos, backend y cliente Android compilan al 100% y pasan todas sus suites de pruebas. | N/A. |

---

### 15. Plan de Limpieza e Integración Recomendado

El plan debe ejecutarse de forma estrictamente modular, ejecutando `compileDebugKotlin` y `dotnet test` tras cada bloque:

```
PASO 1: RESPALDO DE SEGURIDAD
  └── Verificar estado limpio sin commits en feature/ux-notificaciones-centros-responsive.
  └── Crear rama de respaldo local: git branch backup/pre-cleanup-ux-b3.

PASO 2: CONSOLIDACIÓN DE APPSTORAGE
  └── Confirmar que todos los imports apunten a com.example.adoptme.repository.AppStorage.
  └── Eliminar Android/app/src/main/java/com/example/adoptme/data/AppStorage.kt.
  └── Verificar: .\gradlew.bat compileDebugKotlin.

PASO 3: RETIRO SECUENCIAL DE PAQUETES LEGADOS Y DUPLICADOS
  └── Sub-bloque 3A: Eliminar paquete Android/app/src/main/java/com/example/adoptme/data/.
  └── Sub-bloque 3B: Eliminar paquete Android/app/src/main/java/com/example/adoptme/SharedPreferences/.
  └── Sub-bloque 3C: Eliminar paquete Android/app/src/main/java/com/example/adoptme/AdoptMe/ (23 pantallas muertas).
  └── Verificar: .\gradlew.bat compileDebugKotlin testDebugUnitTest (debe mantener 243/243 pruebas).

PASO 4: LIMPIEZA DE RESIDUOS DE AZURE Y SQL REDUNDANTE
  └── Renombrar variables 'mascotasAzure' en MainActivity.kt a 'mascotasRemotas'.
  └── Actualizar LEEME.txt reflejando la arquitectura Render + Neon.
  └── Eliminar Backend/AdoptMe.Api/Data/Neon_UX_Centros_Suspension_PROPUESTA.sql.
  └── Verificar: dotnet test Backend/AdoptMe.sln (116/116 pruebas).

PASO 5: INTEGRACIÓN DE IDENTIDAD VISUAL (LAUNCHER ICON)
  └── Crear ic_launcher_foreground adaptativo con adoptme_logo_original.png en zona segura (66dp).
  └── Generar mipmaps para mdpi, hdpi, xhdpi, xxhdpi, xxxhdpi.
  └── Verificar en emulador y compilar assembleRelease.

PASO 6: REGRESIÓN INTEGRAL FINAL
  └── dotnet test Backend/AdoptMe.sln (116/116).
  └── .\gradlew.bat clean compileDebugKotlin testDebugUnitTest (243/243).
  └── .\gradlew.bat compileReleaseKotlin testReleaseUnitTest assembleRelease (SUCCESS).
  └── git diff --check (sin whitespace errors ni archivos accidentales).
```

---

### 16. Propuesta de Commits Lógicos Posteriores

Para mantener un historial limpio, atómico y profesional de cara al Pull Request hacia `main`, se sugiere estructurar los cambios en commits semánticos:

1. `feat(ux): implementar estado leido de notificaciones, mensajes y badges` (UX-B1)
2. `feat(geo): incorporar geocodificacion inversa y panel operativo del centro` (UX-B2)
3. `feat(centros): incorporar suspension operativa, reactivacion y vinculacion 1:1` (UX-B3)
4. `refactor(android): consolidar AppStorage y retirar paquetes legados AdoptMe y data`
5. `style(android): integrar icono launcher adaptativo institucional y limpiar residuos de Azure`
6. `docs: incorporar informes de auditoria y cierre tecnico de fases UX`

---

### 17. Decisión Final de Aptitud

# **APTO CON CONDICIONES**

El proyecto AdoptMe cuenta con una arquitectura robusta, estable y totalmente validada. No existen impedimentos técnicos ni fallos en pruebas de compilación o backend.

---

### 18. Condiciones Exactas para Continuar con la Limpieza

1. **Crear una rama de respaldo local** (`backup/pre-cleanup-ux-b3`) antes de borrar cualquier archivo.
2. **Ejecutar la eliminación de duplicados en el orden prescrito** (`data/` -> `SharedPreferences/` -> `AdoptMe/`), compilando tras cada paso para garantizar que ninguna referencia oculta sea rota.
3. **No alterar `applicationId`** (`com.example.adoptme`), conservándolo intacto.
4. **Mantener Render (`https://adoptme2.onrender.com/`) como servidor predeterminado** sin requerir dotnet ni adb reverse.
5. **No ejecutar SQL destructivo ni alterar datos en Neon**.
6. **Esperar la autorización explícita del usuario** antes de iniciar la ejecución de la limpieza.
