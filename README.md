# AdoptMe — Sistema de Adopción Responsable de Mascotas
## Entrega #4: Integración con Google Maps y Geolocalización

AdoptMe es una plataforma integral para la adopción y rescate responsable de mascotas, compuesta por una aplicación móvil nativa Android (desarrollada con **Kotlin y Jetpack Compose**) y un servicio backend REST (desarrollado con **ASP.NET Core 8 y PostgreSQL en Neon**).

---

## 1. Arquitectura del Sistema

- **Cliente Móvil (Android):**
  - **Lenguaje y UI:** Kotlin 2.2.10, Jetpack Compose con Material 3.
  - **Consumo REST:** Retrofit 2.11 + OkHttp 4.12 con interceptores de logging y autenticación JWT.
  - **Persistencia Local:** Room SQLite (caché de mascotas visitadas), Preferences DataStore (catálogo) y SharedPreferences (configuración, sesión y términos).
  - **Mapas y Ubicación:** Google Maps Compose (4.4.1), Play Services Maps (19.0.0) y Play Services Location (21.3.0).
  - **Inyección de Credenciales:** `secrets-gradle-plugin` (2.0.1) desacoplado de Git.
- **Backend (Web API):**
  - **Framework:** ASP.NET Core 8 Web API (C#).
  - **Documentación:** Swagger UI (`/swagger`).
  - **Base de Datos:** PostgreSQL en la nube (Neon Database).
  - **Despliegue:** Render.

---

## 2. Configuración de Google Maps API Key

### Aclaración Crítica de Funcionamiento:
- **Compilación asegurada:** El proyecto incluye `secrets.defaults.properties` con un valor de respaldo (`MAPS_API_KEY=REEMPLAZAR`), lo que permite clonar y compilar el proyecto de inmediato con Gradle sin errores de compilación ni excepciones de clave ausente.
- **Visualización del mapa:** Para que Google Maps **renderice y visualice los mapas en pantalla**, es **indispensable contar con una API Key válida** creada en Google Cloud Console con el SDK **"Maps SDK for Android"** habilitado y la facturación/cuota activa. Con una clave ficticia o sin clave, los servidores de Google rechazan la solicitud de tiles y el componente de mapa se muestra en blanco.

### Pasos para configurar tu clave de Google Maps:
1. Dirígete a la carpeta `Android/`.
2. Copia la plantilla `secrets.properties.example` y nómbrala como `secrets.properties`:
   ```properties
   MAPS_API_KEY=AIzaSyTuClaveRealDeGoogleMaps
   ```
3. El archivo `secrets.properties` se encuentra listado en el `.gitignore` del proyecto para evitar cualquier filtración o exposición accidental de credenciales.

---

## 3. Base de Datos: Script DDL para Neon PostgreSQL

Ubicación del script:
`Backend/AdoptMe.Api/Data/Neon_Fase4_Coordenadas_Mascotas.sql`

```sql
-- Agregar columna Latitud si no existe
ALTER TABLE "Mascotas"
ADD COLUMN IF NOT EXISTS "Latitud" double precision NULL;

-- Agregar columna Longitud si no existe
ALTER TABLE "Mascotas"
ADD COLUMN IF NOT EXISTS "Longitud" double precision NULL;
```

- **Idempotencia:** El uso de `IF NOT EXISTS` previene fallos ante ejecuciones repetidas.
- **Retrocompatibilidad:** Al admitir valores `NULL`, todos los registros de mascotas creados en etapas anteriores permanecen 100% funcionales.

---

## 4. Política de Privacidad y Términos de Adopción (Ley N° 30407)

- **Permisos de Ubicación:** Se solicitan en tiempo de ejecución (`ACCESS_FINE_LOCATION` y `ACCESS_COARSE_LOCATION`) previa explicación en un diálogo explicativo (`DialogoExplicacionUbicacion`). La aplicación no solicita ubicación en segundo plano y continúa funcionando si el usuario prefiere no otorgar permisos (permitiendo ingreso manual de coordenadas o distrito).
- **Auditoría de Consentimiento:**
  - Se registra el correo del usuario normalizado.
  - Se guarda la versión exacta de los términos aceptada (`1.0_2026_E4`).
  - Se registra la marca temporal completa (fecha y hora en estándar ISO 8601: `yyyy-MM-dd'T'HH:mm:ss'Z'`).
  - La persistencia se encuentra estrictamente separada por cada usuario en `Preferencias`.
- **Marco Legal:** Integra los deberes de vacunación, esterilización y bienestar contemplados en la **Ley N° 30407 (Ley de Protección y Bienestar Animal del Perú)**.

---

## 5. Instrucciones de Ejecución y Pruebas

### Backend (.NET 8):
```powershell
cd Backend
dotnet build
dotnet test
dotnet run --project AdoptMe.Api
```

### Android:
```powershell
cd Android
.\gradlew.bat clean compileDebugKotlin testDebugUnitTest
```

---

## 6. Resumen de Pruebas Unitarias

| Suite de Pruebas | Archivo | Pruebas | Resultado |
| :--- | :--- | :---: | :---: |
| **Backend — Coordenadas y Límites** | `MascotasCoordenadasTests.cs` | 10 | Superadas (100%) |
| **Backend — Solicitudes y Estados** | `SolicitudesAdminTests.cs` | 10 | Superadas (100%) |
| **Android — Distancia Haversine** | `HaversineUtilTest.kt` | 7 | Superadas (100%) |
| **Android — Modelos y Mappers Coordenadas** | `MascotaCoordenadasTest.kt` | 10 | Superadas (100%) |
| **Android — Términos y Separación de Usuarios** | `PreferenciasTerminosTest.kt` | 5 | Superadas (100%) |
| **Android — Sincronización Solicitudes** | `SolicitudesSincronizacionTest.kt` | 6 | Superadas (100%) |
| **Android — Regresión General** | `ExampleUnitTest.kt` | 96 | Superadas (100%) |
| **Total de Pruebas Unitarias** | — | **144** | **144 / 144 Superadas (100%)** |
