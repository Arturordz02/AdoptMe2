# INFORME TÉCNICO DE IMPLEMENTACIÓN Y VALIDACIÓN E2E — FASE D
## Integración Android de Centros Dinámicos y Eliminación Completa del Hardcoding

**Proyecto:** AdoptMe — Plataforma de Adopción Responsable de Mascotas
**Entorno de Producción:** ASP.NET Core 8 Web API en [Render](https://adoptme2.onrender.com/) + PostgreSQL 16 en [Neon](https://neon.tech/) + CDN en [Cloudinary](https://cloudinary.com/)
**Repositorio Local:** `D:\Escritorio D\T2_GRUPO3_ DESARROLLO_APP\AdoptMe2_Entrega4_Git`
**Rama de Trabajo:** `feature/centros-dinamicos`
**Fecha:** 2026-11-09
**Estado:** COMPLETADA EXITOSAMENTE Y AUDITADA (Detenido antes de Commit, Push, PR, Merge o Despliegue en Render)

---

## 1. Resumen Ejecutivo y Objetivos Cumplidos

La **Fase D** culmina la transición integral de la plataforma AdoptMe hacia una arquitectura de centros de adopción completamente dinámica y desacoplada en Android Jetpack Compose, consumiendo la API REST de Centros ya validada contra PostgreSQL en Neon.

### Objetivos Alcanzados:
1. **Eliminación Total del Hardcoding Funcional:**
   - Erradicación de listas y arreglos estáticos de centros en cliente (`CentrosDemostrativos`, `CentrosDisponiblesDemo`).
   - Eliminación de asignaciones forzadas como `idCentro = 1` o `idCentroSeleccionado = 1`.
   - Consulta dinámica de centros aprobados desde `GET /api/centros` para el catálogo público y el mapa interactivo de Google Maps.
2. **Registro de Usuarios Diferenciado:**
   - Pantalla de registro con selector de rol pedagógico y seguro: solo "Adoptante" o "Centro de Adopción" (el rol "Admin" está terminantemente excluido de la interfaz de registro público).
   - Formulario ampliado con datos institucionales para centros: Nombre, Razón Social/Descripción, Dirección, Distrito, Teléfono de contacto, Horario de atención y Coordenadas geográficas vía selector interactivo de mapa.
   - Envío directo a `POST /api/usuarios/registro-centro`.
3. **Flujo de Vida del Centro y Pantalla de Estados (`PantallaEstadoCentro.kt`):**
   - Centro registrado nace en estado `Pendiente`.
   - Si el Administrador rechaza la solicitud (`Rechazado`), el centro visualiza el motivo de rechazo remitido por el administrador y dispone de un botón para corregir y reenviar a revisión tras confirmación en diálogo modal.
   - Si la solicitud es aprobada (`Aprobado`), el centro accede inmediatamente a su panel operativo `PantallaCentro.kt`.
   - Soporte para estado `Inactivo` (centro suspendido por administración).
4. **Panel Administrativo de Centros (`PantallaAdministrador.kt`):**
   - Pestaña "🏡 Centros" añadida a la interfaz administrativa.
   - Filtros de estado por chips: Todos, Pendientes, Aprobados, Rechazados.
   - Acciones seguras: Aprobar, Rechazar (con diálogo modal y motivo obligatorio de mínimo 5 caracteres), Activar/Desactivar visibilidad pública y reasignación de usuario propietario.
5. **Edición Sensible del Perfil del Centro y Carga Segura de Logotipo:**
   - Detección reactiva de modificaciones sensibles en `PantallaCentro.kt`: si el centro cambia su nombre institucional o traslada su ubicación GPS en una distancia Haversine $> 500$ metros, se muestra un banner amarillo informativo alertando que al guardar retornará a estado `Pendiente`.
   - Carga de logotipo institucional con gestión pedagógica de permisos:
     - Diálogo explicativo previo antes de invocar el contrato nativo `ActivityResultContracts.TakePicture()` con `FileProvider.getUriForFile()`.
     - Soporte para `ActivityResultContracts.PickVisualMedia()` (Photo Picker del SO).
     - Cero permisos masivos de almacenamiento (`READ_EXTERNAL_STORAGE`, `WRITE_EXTERNAL_STORAGE`, `READ_MEDIA_IMAGES` ausentes).
6. **Publicación Institucional de Mascotas:**
   - `PantallaPublicarMascota.kt` detecta dinámicamente si el publicador es un representante de centro (`idCentroActivo != null`), vinculando automáticamente la mascota al identificador institucional sin permitir selectores manuales arbitrarios.
7. **Persistencia y Gobernanza de Sesión:**
   - `Preferencias.kt` persiste de forma atómica y segura: `tokenSesion`, `rolUsuario`, `idCentroActivo`, `estadoCentroActivo`, `centroActivoFlag` y `centroDemostrativoFlag`.
   - Limpieza automática de credenciales obsoletas al cerrar sesión (`limpiarSesionActiva()`).

---

## 2. Métricas de Pruebas Automatizadas

Se cumplió y superó la meta estipulada de pruebas unitarias e integración en ambas capas del proyecto:

| Módulo / Capa | Pruebas Previo a Fase D | Pruebas Creadas en Fase D | Total Pruebas | Resultado |
|---|:---:|:---:|:---:|:---:|
| **Backend ASP.NET Core** (`AdoptMe.Api.Tests`) | 81 | 0 (conservadas) | **81** | ✅ 81/81 SUPERADAS (100%) |
| **Android Jetpack Compose** (`app:testDebugUnitTest`) | 140 | 32 (`CentrosDinamicosAndroidTest.kt`) | **172** | ✅ 172/172 SUPERADAS (100%) |
| **Total General Solución** | **221** | **32** | **253** | ✅ **253/253 SUPERADAS (100%)** |

### Suite Android: 32 Nuevas Pruebas Automatizadas en `CentrosDinamicosAndroidTest.kt`:
1. `dto_centroPublicoDto_deserializaCorrectamente`: Mapeo JSON de centros públicos con coordenadas y estado.
2. `dto_centroPropietarioDto_deserializaCorrectamente`: Campos de propietario (estado, motivo de rechazo, fechas).
3. `dto_registroCentroRequest_serializaCorrectamente`: Carga útil de registro institucional.
4. `dto_registroCentroResponse_deserializaCorrectamente`: Respuesta de registro con Centro ID y token.
5. `dto_rechazarCentroRequest_validaLongitudMinima`: Validación del motivo de rechazo $\ge 5$ caracteres.
6. `dto_asignarPropietarioRequest_serializaCorrectamente`: Asignación de nuevo usuario responsable.
7. `mapper_toCentroAdopcion_mapeaCamposCorrectamente`: Conversión de DTO de red a modelo de UI `CentroAdopcion`.
8. `mapper_toCentroAdopcion_manejaCamposNulosConDefaults`: Resiliencia ante datos opcionales.
9. `preferencias_guardarYRecuperarSesionCentro`: Persistencia de sesión completa para centros.
10. `preferencias_actualizarEstadoCentro_persisteCambio`: Transición dinámica de estado en SharedPreferences.
11. `preferencias_limpiarSesionActiva_eliminaTodosLosCampos`: Cierre de sesión limpio y atómico.
12. `preferencias_tokenSesion_borradoCorrecto`: Eliminación individual de token JWT.
13. `distanciaHaversine_calculaDistanciaAproximadaCorrecta`: Validación matemática de cálculo de distancia en metros.
14. `distanciaHaversine_detectaCambioMayorA500Metros`: Identificación de desplazamiento sensible $> 500$ m.
15. `distanciaHaversine_ignoraCambiosMenoresA500Metros`: Aprobación de desplazamientos menores sin requerir re-aprobación.
16. `deteccionCambioSensible_cambioDeNombre_esSensible`: Cambio de razón social activa retorno a Pendiente.
17. `deteccionCambioSensible_sinCambiosSensibles_noEsSensible`: Modificación de teléfono u horario no altera aprobación.
18. `selectorRol_soloMuestraAdoptanteYCentro`: Asegura ausencia de la opción "Admin" en registro.
19. `pantallaEstadoCentro_determinaMensajeSegunEstado`: Lógica de presentación visual para Pendiente, Rechazado e Inactivo.
20. `pantallaEstadoCentro_permiteReenvioSoloSiRechazado`: Validación de reglas de reenvío de solicitud.
21. `mascotaMapper_asignaCentroIdCorrectamente`: Mapeo de `CentroId` en creación de mascotas.
22. `retrofitClient_baseUrl_configuraYRestaurarCorrectamente`: Conmutación segura de URL base entre local y producción.
23. `retrofitClient_agregaBearerTokenCuandoExiste`: Inyección automática de cabecera `Authorization: Bearer <JWT>`.
24. `filtroAdminCentros_filtraPorEstadoCorrectamente`: Filtrado en memoria de centros administrativos.
25. `filtroAdminCentros_filtroTodosDevuelveTodos`: Integridad de lista completa.
26. `validadorFormularioCentro_camposObligatorios`: Validación de campos vacíos en registro.
27. `validadorFormularioCentro_coordenadasValidas`: Rango geográfico válido para Lima/Perú.
28. `validadorFormularioCentro_telefonoValido`: Formato de teléfono de contacto.
29. `validadorMotivoRechazo_rechazaTextoDemasiadoCorto`: Prevención de motivos ambiguos menores a 5 caracteres.
30. `validadorMotivoRechazo_aceptaTextoValido`: Aprobación de motivos descriptivos.
31. `dialogoConfirmacionReenvio_estadoInicial`: Estado inicial cerrado del diálogo modal.
32. `advertenciaCambioSensible_mensajeInformativo`: Integridad del texto de advertencia pedagógica.

---

## 3. Auditoría de Permisos y Manejo de Medios en Android

### 3.1. Ausencia Total de Permisos Masivos de Almacenamiento
En cumplimiento estricto con las políticas de privacidad de Google Play y las directivas de modernización de Android 13/14:
- `READ_EXTERNAL_STORAGE`: **AUSENTE** en `AndroidManifest.xml`.
- `WRITE_EXTERNAL_STORAGE`: **AUSENTE** en `AndroidManifest.xml`.
- `READ_MEDIA_IMAGES`: **AUSENTE** en `AndroidManifest.xml`.

### 3.2. Captura Fotográfica Segura
- **Diálogo Pedagógico Previo:** Se presenta un `AlertDialog` informativo al usuario explicando la razón del uso de la cámara exclusivamente para la fotografía del logotipo antes de invocar el contrato nativo.
- **FileProvider:** Se utiliza `FileProvider.getUriForFile()` para generar URIs privadas seguras en el directorio de caché de la app (`${applicationId}.provider`), evitando almacenamiento público.
- **Contrato:** Se utiliza exclusivamente `ActivityResultContracts.TakePicture()`, descartando `TakePicturePreview` (evitando imágenes en baja resolución o thumbnails).

### 3.3. Selección de Galería con Photo Picker
- Se invoca `ActivityResultContracts.PickVisualMedia()` con filtro `VisualMediaType.ImageOnly`, delegando la selección al selector de medios integrado del sistema operativo sin requerir ningún permiso en tiempo de ejecución.

---

## 4. Evidencias de Validación E2E Local (Los 20 Checkpoints Cumplidos)

Las pruebas locales se llevaron a cabo conectando el emulador Android (`emulator-5554`) con el backend ASP.NET Core local (`http://localhost:5217`) mediante `adb reverse tcp:5217 tcp:5217`, con persistencia directa en Neon PostgreSQL.

| # | Checkpoint Validado | Evidencia / Captura | Estado |
|---|---|---|:---:|
| 1 | Catálogo público dinámico consumiendo los 5 centros de Neon vía Retrofit | `Evidencias_Entrega4/e2e_centros_publicos.png` | ✅ Superado |
| 2 | Mapa de Google renderizando marcadores geográficos dinámicos | `Evidencias_Entrega4/e2e_mapa_centros.png` | ✅ Superado |
| 3 | Inicio de sesión con centro demostrativo (`centro@adoptme.com`), ID #1 resuelto dinámicamente | `Evidencias_Entrega4/e2e_centro_panel.png` | ✅ Superado |
| 4 | Pantalla de Registro: Selector diferenciado (solo Adoptante o Centro; cero Admin) | `Evidencias_Entrega4/e2e_registro_centro.png` | ✅ Superado |
| 5 | Selector interactivo de ubicación geográfica en mapa durante registro | En formulario de registro | ✅ Superado |
| 6 | Registro exitoso de nuevo centro (`Centro Test E2E`, ID #8) | `POST /api/usuarios/registro-centro` (HTTP 200) | ✅ Superado |
| 7 | Inicio de sesión del nuevo centro y enrutamiento automático a `PantallaEstadoCentro` | `Evidencias_Entrega4/e2e_centro_pendiente.png` | ✅ Superado |
| 8 | Rechazo de centro por Administrador con motivo obligatorio ($\ge 5$ caracteres) | `PUT /api/admin/centros/8/rechazar` (HTTP 200) | ✅ Superado |
| 9 | Visualización en tiempo real del motivo de rechazo en la pantalla de estado del centro | `Evidencias_Entrega4/e2e_centro_rechazado.png` | ✅ Superado |
| 10 | Diálogo de confirmación y reenvío de solicitud a estado Pendiente | Diálogo modal confirmado en app | ✅ Superado |
| 11 | Aprobación de centro por Administrador | `PUT /api/admin/centros/8/aprobar` (HTTP 200) | ✅ Superado |
| 12 | Enrutamiento automático al Panel Operativo (`PantallaCentro.kt`) tras aprobación | `Evidencias_Entrega4/e2e_centro_aprobado_panel.png` | ✅ Superado |
| 13 | Detección reactiva de cambio sensible (nombre o distancia GPS $> 500$ m) | `Evidencias_Entrega4/e2e_advertencia_sensible.png` | ✅ Superado |
| 14 | Banner amarillo con advertencia pedagógica de retorno a estado Pendiente | Banner visible en edición | ✅ Superado |
| 15 | Diálogo pedagógico previo antes de solicitar permiso de cámara para logotipo | `Evidencias_Entrega4/e2e_pedagogico_camara_logo.png` | ✅ Superado |
| 16 | Integración con FileProvider y TakePicture sin almacenamiento masivo | Validación de flujo de cámara | ✅ Superado |
| 17 | Apertura de pantalla Publicar Mascota vinculada automáticamente al Centro #8 | `Evidencias_Entrega4/e2e_publicar_centro.png` | ✅ Superado |
| 18 | Desactivación de centro por Administrador y exclusión automática del catálogo público | `PUT /api/admin/centros/8/desactivar` (HTTP 200) | ✅ Superado |
| 19 | Reactivación de centro por Administrador e inclusión inmediata en catálogo | `PUT /api/admin/centros/8/activar` (HTTP 200) | ✅ Superado |
| 20 | Aislamiento de conversaciones por Centro ID dinámico | Validación de parámetros en `PantallaChat.kt` | ✅ Superado |

---

## 5. Auditoría de Base de Datos Neon PostgreSQL y Limpieza

Una vez concluidas las pruebas E2E, se ejecutó una limpieza transaccional estricta en Neon PostgreSQL para garantizar la total integridad del entorno:

### Registros Depurados:
- **Centros eliminados:** 1 centro temporal de prueba (`Id` = 8, `CorreoContacto` = `'centro.test.e2e@adoptme.pe'`).
- **Usuarios eliminados:** 1 usuario temporal de prueba (`Id` = 30, `Correo` = `'centro.test.e2e@adoptme.pe'`).
- **Mascotas temporales:** 0 (no se publicaron mascotas temporales durante la validación de centros).

### Conteo Final Verificado en Neon:
```sql
SELECT COUNT(*) FROM "Centros";  -- Resultado: 5 (Esperado: 5)
SELECT COUNT(*) FROM "Mascotas"; -- Resultado: 18 (Esperado: 18)
```

Los 5 centros demostrativos históricos permanecen intactos y operativos:
1. `Albergue Huellitas del Norte - Lima (Demo)` (ID #1)
2. `Refugio Patitas Felices Surco (Demo)` (ID #2)
3. `Centro de Adopción Miraflores (Demo)` (ID #3)
4. `Hogar Temporal Ate Vitarte (Demo)` (ID #4)
5. `Modulo Animal Comas (Demo)` (ID #5)

---

## 6. Estado de Restauración del Repositorio

- **URL Base de Retrofit:** Restaurada explícitamente a `URL_PRODUCCION` (`https://adoptme2.onrender.com/`) mediante `RetrofitClient.restaurarBaseUrlProduccion()` en `MainActivity.kt`.
- **Servicios en segundo plano:** Backend local detenido (`task-6483` cancelado y cerrado).
- **Control de versiones:** Rama actual `feature/centros-dinamicos`.
- **Higiene de Git:**
  - `git diff --check`: 0 errores de sintaxis, formato o marcadores de conflicto.
  - Cero secretos expuestos (`secrets.properties` ignorado por git, cero claves o credenciales en código rastreado).
  - CERO ejecuciones de `git add`, `git commit`, `git push`, `gh pr create` o despliegue en Render.
