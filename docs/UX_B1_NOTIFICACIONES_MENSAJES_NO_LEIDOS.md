# INFORME TÉCNICO DE IMPLEMENTACIÓN: FASE UX-B1
## NOTIFICACIONES Y MENSAJES NO LEÍDOS, BADGES Y AISLAMIENTO LOCAL

**Proyecto:** AdoptMe — Plataforma de Adopción Responsable  
**Repositorio local:** `D:\Escritorio D\T2_GRUPO3_ DESARROLLO_APP\AdoptMe2_Entrega4_Git`  
**Rama activa:** `feature/ux-notificaciones-centros-responsive`  
**Base de rama (SHA origin/main):** `da712812c8cf3d4975bcab671cbfa807cab77057`  
**Fecha de ejecución:** 10 de noviembre de 2026  
**Estado:** IMPLEMENTACIÓN Y VALIDACIÓN COMPLETADA — LISTO PARA AUTORIZACIÓN  

---

## 1. ARQUITECTURA REAL ENCONTRADA Y CONFIRMADA

### 1.1 Naturaleza del Almacenamiento (100% Local)
Tras la auditoría e inspección técnica del código fuente, se confirmó formalmente que:
- **Notificaciones y chats son 100% LOCALES** y residen exclusivamente en las `SharedPreferences` del dispositivo Android (`"adoptme_datos"`, `Context.MODE_PRIVATE`).
- **No existen endpoints en el backend de ASP.NET Core** ni tablas en Neon PostgreSQL para mensajes instantáneos ni notificaciones de usuario.
- **No existe Firebase ni sincronización remota** de mensajería; no se declaran capacidades en tiempo real inexistentes para mantener total transparencia técnica y arquitectónica.
- La persistencia y el aislamiento operan localmente mediante claves deterministas por cuenta:
  - `notificaciones_{correoUsuarioNormalizado}`
  - `chat_{correoAdoptanteNormalizado}_centro_{idCentro}`
  - `conversaciones_centro_{correoCentroNormalizado}`
  - `perfil_cache_{correoUsuarioNormalizado}`
  - `favoritos_{correoUsuarioNormalizado}`

### 1.2 Limitaciones Confirmadas de la Mensajería Local
1. Los mensajes enviados por un adoptante a un centro son visibles en el centro únicamente cuando se utiliza el mismo dispositivo/emulador (o la misma instancia de `SharedPreferences`).
2. Al no existir persistencia en la nube para el chat, un usuario que inicie sesión en otro teléfono no descargará mensajes previos de chat ni notificaciones locales anteriores.
3. El sistema cuenta con respuestas simuladas automáticas del centro para demostración pedagógica de interacción sin requerir dos dispositivos conectados a un socket de servidor.

---

## 2. MODELOS IMPLEMENTADOS Y MIGRACIÓN DETERMINISTA

### 2.1 Modelo de Notificaciones (`NotificationItem.kt`)
Se extendió el modelo de datos manteniendo retrocompatibilidad binaria absoluta con el código existente:
```kotlin
data class NotificationItem(
    val title: String,
    val message: String,
    val id: String = java.util.UUID.randomUUID().toString(),
    val correoPropietario: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val fecha: String = "",
    val leida: Boolean = false,
    val tipo: String = "SISTEMA"
)
```

**Regla de Migración Determinista de Registros Históricos:**
- Al deserializar el JSON de notificaciones desde `SharedPreferences`:
  - Si un registro carece de `id`, se genera un identificador determinista derivado del hash del título, mensaje e índice.
  - Si carece de `leida` (registros previos a esta versión), se interpreta como **`leida = true`** para evitar inflar indebidamente el badge inicial con avisos ya vistos en sesiones pasadas.
  - Toda nueva notificación creada a partir de esta fase se inicializa con **`leida = false`**, activando de inmediato el badge y el indicador visual.

### 2.2 Modelo de Mensajes y Conversaciones (`MensajeChat.kt`)
Se extendió `MensajeChat` y `ConversacionCentro` para registrar el estado de lectura:
```kotlin
data class MensajeChat(
    val id: Long = System.currentTimeMillis(),
    val idCentro: Int,
    val remitente: String,
    val receptor: String,
    val texto: String,
    val nombreCentro: String,
    val fecha: String,
    val esDelCentro: Boolean,
    val leido: Boolean = false,
    val fechaLectura: String? = null
)

data class ConversacionCentro(
    val idCentro: Int,
    val correoAdoptante: String,
    val correoCentro: String,
    val nombreCentro: String,
    val ultimoTexto: String,
    val fecha: String,
    val mensajesNoLeidos: Int = 0
)
```

---

## 3. LIMPIEZA CONTROLADA DE RESIDUOS E2E Y PATRONES TÉCNICOS

Para erradicar de forma segura avisos técnicos generados durante pruebas automatizadas previas, sin borrar notificaciones legítimas del usuario ni depender de coincidencias ambiguas en lenguaje natural, se establecieron los siguientes prefijos técnicos inequívocos:

```kotlin
val PREFIJOS_PRUEBA_E2E = listOf(
    "PRUEBA_",
    "PRUEBA_ANDROID_",
    "PRUEBA_PRODUCCION_",
    "PRUEBA_CAMARA_",
    "PRUEBA_SOLICITUD_",
    "PRUEBA_CENTRO_",
    "TEST_E2E_"
)
```

### 3.1 Garantías de la Limpieza:
1. **Idempotencia:** La función `limpiarNotificacionesPrueba(correo)` puede ejecutarse repetidamente; si no existen residuos, no altera el almacenamiento.
2. **Preservación Legítima:** Notificaciones con títulos como *"Mascota publicada 🐾"*, *"Solicitud de adopción"*, *"Bienvenido a AdoptMe"* o mensajes informativos reales se conservan intactos.
3. **No Resurgimiento:** Se ejecuta al iniciar sesión y al cargar datos, eliminando físicamente las entradas obsoletas de `SharedPreferences`.

---

## 4. FUNCIONAMIENTO DE BADGES Y EXPERIENCIA VISUAL

### 4.1 Badge en Navegación Inferior (Adoptante)
En `components/BarraNavegacionInferior.kt` se integró `BadgedBox` de Material3 en las pestañas de **Avisos** (`notifications`) y **Chat** (`chat`):
- **Conteo = 0:** No se muestra badge (icono despejado).
- **Conteo de 1 a 99:** Muestra el número exacto (`1`, `2`, `15`, etc.).
- **Conteo > 99:** Muestra el formato `"99+"`.
- **Tokens Semánticos:** Utiliza `MaterialTheme.colorScheme.error` como contenedor y `MaterialTheme.colorScheme.onError` como texto, asegurando contraste accesible WCAG AA tanto en temas claros como oscuros.

### 4.2 Pantalla de Notificaciones (`PantallaNotificaciones.kt`)
- **Diferenciación visual:** Notificaciones no leídas muestran borde con acento de color primario, fondo `surfaceVariant` y etiqueta destacada `"Nueva"`. Notificaciones leídas se muestran con fondo atenuado `surface`.
- **Acción individual:** Botón `"Leída"` en cada tarjeta para marcarla de forma independiente.
- **Acción global:** Botón `"Marcar todas"` en la cabecera, visible y habilitado únicamente si existen avisos sin leer.
- **Eliminación individual segura:** Botón de descarte con diálogo de confirmación emergente antes de borrar el registro.
- **Estado vacío:** Tarjeta ilustrada amigable (*"No tienes notificaciones registradas"*).

### 4.3 Bandeja y Conversaciones del Centro (`PantallaCentro.kt`)
- **Cabecera de bandeja:** Muestra un chip destacado con `errorContainer` indicando el total de mensajes no leídos del centro (ej. `"2 no leído(s)"`).
- **Tarjetas por conversación:** Cada tarjeta de adoptante que tenga mensajes pendientes muestra un badge `"N nuevo(s)"` y borde de alerta.
- **Marcado de lectura:** Al presionar `"Abrir chat"`, se marcan como leídos exclusivamente los mensajes recibidos del adoptante para esa conversación, actualizando inmediatamente el contador local y el total de la bandeja.

---

## 5. CUENTA DEMOSTRATIVA (`usuario@adoptme.com`)

- Se verificó que la cuenta demostrativa predeterminada `usuario@adoptme.com`:
  - Queda completamente limpia de registros `PRUEBA_*` en el almacenamiento local.
  - Conserva su historial legítimo (publicaciones, solicitudes y favoritos locales).
  - Los contadores de badge inician en su valor real calculado.
  - **Aclaración explícita:** La limpieza se realiza exclusivamente en el almacenamiento local del dispositivo/emulador; no se alteró la base de datos Neon ni la API de producción.

---

## 6. INVENTARIO DE ARCHIVOS MODIFICADOS Y CREADOS

| Archivo | Acción | Descripción del Cambio |
| :--- | :--- | :--- |
| `Android/app/src/main/java/com/example/adoptme/model/NotificationItem.kt` | Modificado | Incorpora `id`, `correoPropietario`, `timestamp`, `fecha`, `leida`, `tipo`. |
| `Android/app/src/main/java/com/example/adoptme/model/MensajeChat.kt` | Modificado | Incorpora `leido` y `fechaLectura` en `MensajeChat`; `mensajesNoLeidos` en `ConversacionCentro`. |
| `Android/app/src/main/java/com/example/adoptme/repository/AppStorage.kt` | Modificado | Operaciones de conteo, marcado de lectura, limpieza E2E, constructor con `SharedPreferences`. |
| `Android/app/src/main/java/com/example/adoptme/data/AppStorage.kt` | Modificado | Réplica exacta para mantener sincronía y compatibilidad de paquetes. |
| `Android/app/src/main/java/com/example/adoptme/components/BarraNavegacionInferior.kt` | Modificado | Incorporación de `BadgedBox` y `Badge` para Avisos y Chat (0 oculto, 1-99, 99+). |
| `Android/app/src/main/java/com/example/adoptme/AdoptMe/BarraNavegacionInferior.kt` | Modificado | Sincronización de firma con parámetros por defecto para paquete legado. |
| `Android/app/src/main/java/com/example/adoptme/screens/PantallaNotificaciones.kt` | Modificado | Rediseño accesible: tarjetas nueva/leída, marcar leída, marcar todas, eliminar con confirmación. |
| `Android/app/src/main/java/com/example/adoptme/screens/PantallaPrincipalAdoptante.kt` | Modificado | Conexión reactiva de contadores de badges, limpieza inicial y marcado de chat como leído. |
| `Android/app/src/main/java/com/example/adoptme/screens/PantallaCentro.kt` | Modificado | Badge global en cabecera de bandeja y badges individuales por conversación con mensajes pendientes. |
| `Android/app/src/main/java/com/example/adoptme/MainActivity.kt` | Modificado | Limpieza E2E al inicio/login y marcado de lectura al abrir chat desde el panel del centro. |
| `Android/app/src/test/java/com/example/adoptme/NotificacionesYMensajesNoLeidosTest.kt` | **Creado** | Suite completa de 15 pruebas unitarias cubriendo los 22 puntos funcionales de UX-B1. |

---

## 7. RESULTADOS DE LA SUITE DE PRUEBAS AUTOMATIZADAS

### 7.1 Backend ASP.NET Core 8
- **Comando:** `dotnet build Backend/AdoptMe.sln` -> Compilación exitosa (0 advertencias, 0 errores).
- **Comando:** `dotnet test Backend/AdoptMe.sln`
- **Resultado:** **84 de 84 pruebas superadas** (0 fallidas, 0 omitidas, duración: 1s).

### 7.2 Android Debug Unit Tests
- **Comando:** `.\gradlew.bat testDebugUnitTest`
- **Resultado:** **205 de 205 pruebas superadas** (0 fallidas, 0 omitidas, duración: 21s).
  - Incluye las 190 pruebas de regresión base + 15 pruebas nuevas de UX-B1.

### 7.3 Android Release Build & Tests
- **Comando:** `.\gradlew.bat compileReleaseKotlin testReleaseUnitTest assembleRelease`
- **Resultado:** **205 de 205 pruebas superadas** y APK Release generado exitosamente (BUILD SUCCESSFUL in 2m 1s).

---

## 8. REQUISITO CONFIRMADO Y REGISTRADO PARA FASE UX-B2

Se ratifica formalmente el requisito obligatorio de autocompletado desde el mapa para la fase **UX-B2**:

> **Regla de Negocio para UX-B2:**  
> Cuando el usuario seleccione o cambie un punto en el mapa durante:
> 1. Registro de un centro;
> 2. Edición de un centro;
> 3. Publicación de una mascota;
> 4. Edición de una publicación;
> 
> La aplicación deberá ejecutar geocodificación inversa y completar automáticamente:
> - **Dirección física**;
> - **Distrito**;
> - **Latitud**;
> - **Longitud**.
> 
> **Comportamiento Requerido:**
> 1. El mapa devuelve Latitud y Longitud exactas.
> 2. La app consulta una dirección legible usando Geocoder de Android.
> 3. La Dirección física y el Distrito se completan automáticamente en el formulario.
> 4. La interfaz muestra primero la dirección y el distrito como campos primarios.
> 5. Las coordenadas aparecen como información técnica secundaria y discreta.
> 6. El usuario puede corregir manualmente la dirección si la respuesta es imprecisa.
> 7. Fallback seguro: Si no se obtiene dirección (offline o sin respuesta), conserva coordenadas, muestra *"Ubicación seleccionada en el mapa"*, permite ingreso manual y no bloquea el formulario.
> 8. No sobrescribir silenciosamente una corrección manual si el usuario no ha vuelto a mover el marcador.
> 
> *(Este requisito NO fue implementado en UX-B1 y queda agendado estrictamente para UX-B2)*.

---

## 9. EVALUACIÓN DE RIESGOS RESIDUALES

- **Riesgo:** Pérdida accidental de mensajes al reinstalar la aplicación.  
  *Mitigación:* Documentada formalmente la naturaleza local de SharedPreferences; se mantiene la arquitectura intacta hasta que se proyecte sincronización en la nube.
- **Riesgo:** Confusión de remitentes en chats múltiples.  
  *Mitigación:* Aislamiento garantizado mediante claves con formato `chat_{adoptante}_{idCentro}` y validación estricta de pertenencia de roles en el marcado de lectura.
- **Riesgo:** Regresiones en producción o base de datos.  
  *Mitigación:* Se cumplió estrictamente la prohibición de ejecutar SQL, modificar Neon o alterar Render. Producción conserva sus 18 mascotas históricas intactas.
