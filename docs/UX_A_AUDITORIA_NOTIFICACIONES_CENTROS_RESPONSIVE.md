# INFORME DE AUDITORÍA Y DISEÑO DE EXPERIENCIA (FASE UX-A)
## MEJORAS DE EXPERIENCIA, NOTIFICACIONES, MENSAJERÍA, CENTROS Y RESPONSIVE

**Proyecto:** AdoptMe — Plataforma de Adopción Responsable  
**Repositorio local:** `D:\Escritorio D\T2_GRUPO3_ DESARROLLO_APP\AdoptMe2_Entrega4_Git`  
**Rama de trabajo:** `feature/ux-notificaciones-centros-responsive`  
**Base de rama:** Commit `da712812c8cf3d4975bcab671cbfa807cab77057` (`origin/main`)  
**Estado:** Documento de diseño y auditoría técnica — Fase exclusivamente analítica (Sin mutación de base de datos ni producción)  
**Fecha:** 9 de noviembre de 2026  

---

## 1. ESTADO INICIAL DEL ENTORNO Y LÍNEA BASE

Antes de iniciar el proceso analítico, se verificó exhaustivamente el repositorio, la sincronización con el remoto y la suite de pruebas del proyecto:

1. **Sincronización Git:**
   - Commit base: `da712812c8cf3d4975bcab671cbfa807cab77057` (PR #4 integrado: *"fix(android): usar Render por defecto y admitir edades compuestas"*).
   - Rama activa aislada: `feature/ux-notificaciones-centros-responsive`.
   - Árbol de trabajo: Limpio (`working tree clean`).

2. **Validación de Pruebas de Regresión:**
   - **Backend ASP.NET Core 8:**
     - Comando: `dotnet test Backend/AdoptMe.sln`
     - Resultado: **84 de 84 pruebas pasadas exitosamente** (0 fallidas, 0 omitidas, duración: 1.1s).
   - **Android Jetpack Compose:**
     - Comando: `gradlew.bat testDebugUnitTest`
     - Resultado: **190 de 190 pruebas unitarias pasadas exitosamente** (0 fallidas, 0 omitidas, duración: 1m 13s).

3. **Restricciones y Salvaguardas Activas:**
   - Entorno de producción en Render (`https://adoptme2.onrender.com/`) no modificado.
   - Base de datos Neon PostgreSQL intacta con sus **18 mascotas históricas** y tablas de centros dinámicos protegidas.
   - Sin inserción ni mutación de registros temporales en producción.
   - El paquete legado `com.example.adoptme.AdoptMe` se mantiene compilable sin eliminaciones en esta etapa.

---

## 2. AUDITORÍA DEL MÓDULO 1: NOTIFICACIONES

### 2.1 Diagnóstico del Estado Actual
- **Modelo de datos actual (`NotificationItem.kt`):**
  ```kotlin
  data class NotificationItem(
      val title: String,
      val message: String
  )
  ```
  *Deficiencias detectadas:* Carece de identificador único (`id`), marca de tiempo (`timestamp`), estado de lectura (`isRead`), categoría (`tipo: Solicitud, Centro, Sistema`), y referencia de acción opcional (`referenciaId`).
- **Persistencia en SharedPreferences (`AppStorage.kt`):**
  Las notificaciones se guardan mediante la clave `notificaciones_{correoUsuario}`. Aunque la clave ya incorpora partición por correo, la serialización JSON actual solo almacena `{"title": "...", "message": "..."}`.
- **Contaminación por pruebas de integración anteriores:**
  Se detectó la presencia ocasional de notificaciones residuales generadas durante pruebas de integración previas (con prefijos `PRUEBA_SOLICITUD_*`, `PRUEBA_CENTRO_*`).
- **Visualización y Navegación:**
  - `BarraNavegacionInferior.kt` muestra el icono `ic_notifications` de forma estática, **sin badge reactivo de contador de avisos pendientes**.
  - `PantallaNotificaciones.kt` es una lista pasiva: no permite marcar notificaciones individuales como leídas, no tiene botón de "Marcar todas como leídas", ni opción para eliminar una notificación leída.

### 2.2 Diseño de la Solución Técnica
#### 2.2.1 Modelo Enriquecido
```kotlin
enum class TipoNotificacion {
    SOLICITUD_RECIBIDA,
    SOLICITUD_ACTUALIZADA,
    CENTRO_APROBADO,
    CENTRO_SUSPENDIDO,
    CENTRO_RECHAZADO,
    MENSAJE_CHAT,
    SISTEMA
}

data class NotificationItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val title: String,
    val message: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false,
    val tipo: TipoNotificacion = TipoNotificacion.SISTEMA,
    val referenciaId: String? = null
)
```

#### 2.2.2 Filtrado Quirúrgico de Residuos de Pruebas
Para eliminar de forma segura los avisos residuales sin barrer notificaciones legítimas del usuario:
```kotlin
fun esNotificacionValida(item: NotificationItem): Boolean {
    val patronesPrueba = listOf("PRUEBA_SOLICITUD_", "PRUEBA_CENTRO_", "TEST_E2E_")
    return patronesPrueba.none { prefijo ->
        item.title.startsWith(prefijo, ignoreCase = true) ||
        item.message.startsWith(prefijo, ignoreCase = true)
    }
}
```

#### 2.2.3 Contador y Badge en Barra de Navegación
Implementar en `BarraNavegacionInferior.kt` mediante `BadgedBox` de Material3:
- Si `cantidadNoLeidas == 0`: mostrar icono limpio.
- Si `cantidadNoLeidas in 1..99`: mostrar badge numérico con el valor exacto.
- Si `cantidadNoLeidas > 99`: mostrar badge con texto `"99+"`.
- El contador es reactivo y depende de la lista observada en el estado global del usuario autenticado.

#### 2.2.4 Funcionalidades en `PantallaNotificaciones`
1. **Acción rápida por tarjeta:** Al tocar la tarjeta, se marca como leída (`isRead = true`) y se actualiza la persistencia.
2. **Botón superior "Marcar todas como leídas":** Habilitado únicamente si `notificaciones.any { !it.isRead }`.
3. **Deslizar para eliminar o botón de eliminación individual:** Permite remover avisos prescindibles.
4. **Estado vacío amigable:** Indicador visual ilustrado cuando no hay notificaciones o todas están leídas.

---

## 3. AUDITORÍA DEL MÓDULO 2: MENSAJERÍA Y CHAT LOCAL

### 3.1 Diagnóstico del Estado Actual
- **Naturaleza de la mensajería:** La plataforma AdoptMe opera el chat como una **demostración local en Android** mediante `AppStorage.kt` (SharedPreferences particionado por usuario y centro: `chat_{usuario}_{centroId}`). No existen endpoints en el backend de ASP.NET Core para mensajes en tiempo real ni webhooks.
- **Modelo de datos actual (`MensajeChat.kt`):**
  ```kotlin
  data class MensajeChat(
      val id: Long = System.currentTimeMillis(),
      val idCentro: Int,
      val remitente: String,
      val receptor: String,
      val texto: String,
      val nombreCentro: String,
      val fecha: String,
      val esDelCentro: Boolean
  )
  ```
  *Deficiencias detectadas:*
  - No contiene la propiedad `leido: Boolean`.
  - La conversación en el panel del centro (`ConversacionCentro`) no cuenta con un campo `mensajesNoLeidos: Int`.
  - El icono de "Chat" en la barra de navegación inferior no refleja si hay mensajes nuevos sin leer.
  - Al abrir un chat, no existe marcado de lectura de los mensajes recibidos.

### 3.2 Diseño de la Solución Técnica
#### 3.2.1 Enriquecimiento del Modelo Local
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
    val leido: Boolean = false // Nuevo campo
)

data class ConversacionCentro(
    val idCentro: Int,
    val correoAdoptante: String,
    val correoCentro: String,
    val nombreCentro: String,
    val ultimoTexto: String,
    val fecha: String,
    val mensajesNoLeidos: Int = 0 // Nuevo campo calculado
)
```

#### 3.2.2 Lógica de Conteo y Marcado de Lectura
- **Cálculo de no leídos para Adoptante:** Mensajes recibidos donde `esDelCentro == true` y `leido == false`.
- **Cálculo de no leídos para Centro:** Mensajes recibidos donde `!esDelCentro` y `leido == false`.
- **Transición de Estado (Marcar como leídos):**
  Al entrar a `PantallaChat`:
  ```kotlin
  fun marcarMensajesComoLeidos(correoAdoptante: String, idCentro: Int, rolUsuario: String) {
      val mensajes = cargarMensajesChat(correoAdoptante, idCentro)
      val actualizados = mensajes.map { m ->
          val esRecibido = if (rolUsuario == "Centro") !m.esDelCentro else m.esDelCentro
          if (esRecibido && !m.leido) m.copy(leido = true) else m
      }
      guardarMensajesChat(correoAdoptante, idCentro, actualizados)
  }
  ```
- **Badge en Barra Inferior y en Panel del Centro:**
  Se añade `BadgedBox` en la pestaña de Chat y un indicador tipo píldora destacada en cada conversación del centro que contenga `mensajesNoLeidos > 0`.
- **Transparencia Arquitectónica Documentada:** Se mantendrá visible en la interfaz el distintivo pedagógico *"Mensajería demostrativa local (sin costo de infraestructura de servidor)"*.

---

## 4. AUDITORÍA DEL MÓDULO 3: PANEL OPERATIVO DEL CENTRO

### 4.1 Diagnóstico de Pantalla Actual (`PantallaCentro.kt`)
1. **Disposición Visual Congestionada:**
   - La cabecera coloca 3 botones horizontales (`Editar perfil`, `Cambiar logo`, `Publicar mascota`) en una sola fila `Row`. En pantallas con anchos de 320dp o 360dp, los textos se truncan o desbordan.
2. **Uso de Colores Hardcodeados y Falta de Soporte para Tema Oscuro:**
   - Se utilizan colores directos como `AdoptTextDark (0xFF1A1A2E)` y `AdoptCardWhite (0xFFFFFFFF)` directamente en lugar de `MaterialTheme.colorScheme.onSurface` o `MaterialTheme.colorScheme.surface`. Esto genera problemas de contraste severos cuando el sistema activa el tema oscuro.
3. **Escasez de Métricas Operativas:**
   - Actualmente solo se muestra un conteo simple de `conversacionesFiltradas.size`. El responsable del centro no tiene visibilidad inmediata de sus mascotas activas, en proceso o mensajes pendientes.

### 4.2 Rediseño Propuesto (Responsive & Arquitectura de Componentes)
```
+-------------------------------------------------------------+
| [Logotipo Institucional]  Refugio Amigos Peludos (ID #3)    |
|                           Distrito: Miraflores, Lima        |
|                           [Aprobado · Activo]  [Oficial]    |
+-------------------------------------------------------------+
| Tarjetas Métricas en Cuadrícula (2x2 en móvil, 4x1 en tablet)|
|  [ 🐶 12 ] Publicaciones       |  [ 🟢 9 ] Disponibles      |
|  [ 💬 5  ] Conversaciones      |  [ 🔔 2 ] Mensajes nuevos  |
+-------------------------------------------------------------+
| Acciones Operativas Principales                             |
|  [ ➕ Publicar Mascota ]  (Botón destacado primario)         |
|  [ ✏️ Editar Perfil ]      [ 🖼️ Cambiar Logotipo ]          |
+-------------------------------------------------------------+
| Información de Contacto y Horarios (Plegable / Accesible)   |
|  📞 987654321 · ✉️ contacto@refugio.org · 🕒 Lun-Sáb 9-18h  |
+-------------------------------------------------------------+
| Bandeja de Consultas de Adoptantes                          |
|  - Juan Pérez (Perro Toby) · "Hola, ¿sigue disponible?" (2) |
|  - María Gómez (Gata Michi)· "Muchas gracias por el dato"   |
+-------------------------------------------------------------+
```

#### 4.2.1 Especificaciones de Adaptabilidad
- En anchos `< 360dp`: Los botones de acción se apilan verticalmente o en cuadrícula de 2 columnas.
- En anchos `>= 600dp` (Tablets / Foldables): La cabecera institucional y las estadísticas se muestran en dos columnas laterales continuas.
- Reemplazo completo de colores estáticos por tokens semánticos:
  - `MaterialTheme.colorScheme.surface`
  - `MaterialTheme.colorScheme.onSurface`
  - `MaterialTheme.colorScheme.primary`
  - `MaterialTheme.colorScheme.surfaceVariant`

---

## 5. AUDITORÍA DEL MÓDULO 4: DIRECCIÓN LEGIBLE Y COORDENADAS TÉCNICAS

### 5.1 Diagnóstico del Estado Actual
- En `PantallaPublicarMascota.kt` y `DialogoSelectorMapa.kt`, una vez marcado el punto en el mapa, la interfaz muestra prominentemente una cadena de texto técnico:
  `📍 Lat: -12.0463, Lon: -77.0305`.
- Esto confunde al usuario común y no aporta valor inmediato en la lectura cotidiana de la publicación.

### 5.2 Diseño de Separación Semántica
1. **Jerarquía Visual Reorganizada:**
   - **Campo Principal:** Dirección legible / Referencia pública descriptiva (ej. *"Av. José Larco 850, Miraflores"* o *"Parque Central de Miraflores"*).
   - **Campo Secundario:** Distrito / Localidad (ej. *"Miraflores, Lima"*).
   - **Sección Técnica Discreta:** Bloque plegable (*"Detalles técnicos de posicionamiento GPS"*), visible solo al expandir, mostrando `Latitud: -12.046374`, `Longitud: -77.030583` con precisión de 6 decimales.
2. **Estrategia Robusta de Geocodificación Inversa:**
   - Uso de `Geocoder.getFromLocation(lat, lon, 1)` en un contexto seguro de corrutinas (`Dispatchers.IO`).
   - Si el geocodificador no devuelve calle específica, usar `subLocality` o `locality`.
   - **Fallback Seguro en Modo Offline o Error de Red:** Si falla la resolución de dirección legible, el formulario no se bloquea ni genera error; en su lugar muestra: *"Ubicación confirmada en el mapa"* y conserva intactos los valores de `Latitud` y `Longitud` para que el cálculo Haversine de distancias siga funcionando con máxima precisión.

### 5.3 Requisito Confirmado y Obligatorio para Fase UX-B2
Cuando el usuario seleccione o cambie un punto en el mapa durante:
- Registro de un centro;
- Edición de un centro;
- Publicación de una mascota;
- Edición de una publicación;

La aplicación deberá ejecutar geocodificación inversa y completar automáticamente:
- Dirección física;
- Distrito;
- Latitud;
- Longitud.

**Comportamiento Requerido:**
1. El mapa devuelve Latitud y Longitud exactas.
2. La app consulta una dirección legible usando Geocoder de Android o una solución equivalente ya disponible.
3. La Dirección física se completa automáticamente.
4. El Distrito se completa automáticamente.
5. La interfaz muestra primero la dirección y el distrito.
6. Las coordenadas aparecen como información técnica secundaria.
7. El usuario puede corregir manualmente la dirección si la respuesta es imprecisa.
8. Si no se obtiene una dirección:
   - Conservar las coordenadas;
   - Mostrar *"Ubicación seleccionada en el mapa"*;
   - Permitir completar manualmente dirección y distrito;
   - No bloquear ni cerrar el formulario.
9. No sobrescribir silenciosamente una corrección manual si el usuario no ha vuelto a mover el marcador.
10. Este requisito se implementará en la Fase UX-B2.

---

## 6. AUDITORÍA DEL MÓDULO 5: RECHAZO INICIAL VS. SUSPENSIÓN OPERATIVA

### 6.1 Problema Conceptual y Técnico Detectado
En la implementación actual (`CentrosController.cs` y `AdminCentrosController.cs`):
- Los estados de la columna `Estado` son: `'Pendiente'`, `'Aprobado'`, `'Rechazado'`.
- La columna `Activo` (booleano) permite desactivar un centro.
- **Falla de Diseño:** Cuando un centro ya aprobado y con mascotas en adopción incurre en una irregularidad operativa, cambiar su `Estado` a `'Rechazado'` genera inconsistencia ontológica: confunde el rechazo de la solicitud inicial de registro con una medida disciplinaria sobre una entidad que ya fue acreditada.
- Además, el backend carecía de campos de auditoría para registrar el motivo de la suspensión, la fecha de la sanción y la identidad del administrador que la aplicó.

### 6.2 Matriz y Máquina de Estados Definitiva

```
[ POST /api/centros/solicitar ]
               │
               ▼
      ┌─────────────────┐
      │    PENDIENTE    │
      └────────┬────────┘
               │
       ┌───────┴────────────────────────┐
       │ (Admin aprueba)                │ (Admin rechaza con motivo)
       ▼                                ▼
┌──────────────┐              ┌──────────────────┐
│   APROBADO   │              │    RECHAZADO     │
│ Activo: TRUE │              │ (Solicitud inicial│
└──────┬───────┘              │  desestimada)    │
       │                      └────────┬─────────┘
       │ (Admin suspende)              │
       ▼                               │ (Usuario corrige y reenvía)
┌───────────────────────┐              │
│       APROBADO        │◄─────────────┘
│    (SUSPENDIDO)       │
│    Activo: FALSE      │
│ MotivoSuspension != Ø │
└──────────┬────────────┘
           │ (Admin reactiva)
           ▼
┌───────────────────────┐
│       APROBADO        │
│     (REACTIVADO)      │
│     Activo: TRUE      │
│ FechaReactivacion: t  │
└───────────────────────┘
```

### 6.3 Endpoints Administrativos Propuestos
1. `PUT /api/admin/centros/{id}/suspender`:
   - Payload: `{"motivo": "Incumplimiento de términos de bienestar animal verificado en inspección."}`.
   - Acciones: Asigna `Activo = false`, `MotivoSuspension = dto.Motivo`, `FechaSuspension = DateTime.UtcNow`, `SuspendidoPorAdminId = adminId`.
   - Efecto público: El centro deja de ser listado en catálogo y mapa.
   - Efecto institucional: Al autenticarse el centro, su panel muestra un banner de advertencia con el motivo de suspensión y bloquea la publicación de mascotas nuevas (`HTTP 403`).
2. `PUT /api/admin/centros/{id}/reactivar`:
   - Acciones: Asigna `Activo = true`, `FechaReactivacion = DateTime.UtcNow`, limpia `MotivoSuspension` (o lo archiva en auditoría).
   - Efecto: El centro vuelve a operar y a ser visible en el catálogo público.

### 6.4 Propuesta SQL Creada
Se redactó el script transaccional e idempotente en:
`Backend/AdoptMe.Api/Data/Neon_UX_Centros_Suspension_PROPUESTA.sql`
*(Nota: Este script es únicamente una propuesta documental y no ha sido ejecutado en Neon PostgreSQL durante esta fase)*.

---

## 7. AUDITORÍA DEL MÓDULO 6: ACCIÓN "ASIGNAR" EN PANEL DE ADMINISTRACIÓN

### 7.1 Diagnóstico de Pantalla Actual (`PantallaAdministrador.kt`)
- Cada tarjeta de centro en el panel de administración muestra un botón genérico titulado `"Asignar"` (línea 1118).
- Al presionarlo, se abre un diálogo que solicita un número entero arbitrario `UsuarioId`.
- **Deficiencias de Usabilidad:**
  - No explica qué significa asignar ni qué condiciones debe cumplir el usuario.
  - El administrador debe memorizar o consultar previamente el ID numérico de la cuenta del centro.
  - No valida en la interfaz si el centro ya tiene un titular asignado o si se trata de un centro demostrativo huérfano.

### 7.2 Rediseño Propuesto: "Vincular Responsable"
1. **Renombrado y Claridad de la Acción:**
   - Si `centro.usuarioId == null`: Botón `"Vincular responsable"` con icono 🔗.
   - Si `centro.usuarioId != null`: Botón `"Transferir titular"` con advertencia de reasignación.
2. **Modal Inteligente de Selección:**
   - Muestra la lista de usuarios registrados con el rol `"Centro"` que aún no son propietarios de ningún otro centro (respetando el índice único parcial de Neon).
   - Muestra nombre, correo electrónico y fecha de registro del usuario candidato.
   - Si se ingresa por búsqueda, valida en tiempo real y muestra la tarjeta del usuario antes de confirmar la vinculación.
3. **Validaciones en Backend y Frontend:**
   - Verificar rol `"Centro"`.
   - Impedir asignación a usuarios con rol `"Adoptante"` o `"Admin"`.
   - Confirmar mediante diálogo de confirmación explícito antes de invocar `PUT /api/admin/centros/{id}/asignar-propietario`.

---

## 8. AUDITORÍA DEL MÓDULO 7: RESPONSIVE Y ACCESIBILIDAD

### 8.1 Matriz de Dispositivos y Viewports Evaluados
| Factor de Forma | Resolución Típica | Densidad | Desafíos Detectados | Solución Diseñada |
| :--- | :--- | :--- | :--- | :--- |
| **Gama Baja / Compacto** | 320 x 568 dp | 1.5 - 2.0x | Desbordamiento de filas de 3 botones; recorte de campos de texto con teclado abierto. | `verticalScroll(rememberScrollState())`, `imePadding()`, apilamiento en columna en `< 360dp`. |
| **Smartphone Estándar** | 390 x 844 dp | 2.5 - 3.0x | Espaciado visual desequilibrado en tarjetas. | Padding simétrico de 16-20dp, esquinas redondeadas de 14-16dp. |
| **Smartphone Grande** | 412 x 915 dp | 3.5 - 4.0x | Espacio en blanco excesivo en listas simples. | Tarjetas con jerarquía tipográfica enriquecida. |
| **Tablets / Plegables** | 600 x 960 dp+ | 1.5 - 2.0x | Estiramiento horizontal excesivo de formularios. | Contenedores con `maxWidth(600.dp)` centrado o disposición de 2 columnas. |

### 8.2 Accesibilidad y Cumplimiento WCAG 2.1 AA
1. **Escalado Dinámico de Fuentes:**
   - La aplicación debe admitir escalado de fuentes del sistema entre 1.0x y 1.5x sin que se recorten los textos en botones o cabeceras. Se definirá `maxLines` con `overflow = TextOverflow.Ellipsis` únicamente en subtítulos y nunca en textos instructivos o de advertencia.
2. **Consumo Estricto de `MaterialTheme.colorScheme`:**
   - Prohibir el uso directo de colores fijos como `AdoptTextDark` en componentes con fondo dinámico.
   - Todo texto de primer plano debe emplear `MaterialTheme.colorScheme.onSurface`, `onPrimary` o `onSurfaceVariant` para garantizar ratios de contraste mínimos de 4.5:1 tanto en temas claros como oscuros.
3. **Áreas de Toque Accesibles:**
   - Botones e iconos interactivos con área mínima táctil de 48 x 48 dp (cumpliendo directrices de Material Design 3).

---

## 9. AUDITORÍA DEL MÓDULO 8: ESTADOS DE CONEXIÓN CON RENDER Y RESILIENCIA

### 9.1 Diagnóstico de la Conectividad con Render
- Tras la unificación de URL en el hotfix anterior, Android consume por defecto:
  `https://adoptme2.onrender.com/`
- Render (en su plan gratuito) suspende la instancia tras 15 minutos de inactividad (*spin-down*). El primer request entrante activa el arranque en frío (*cold start*), el cual puede demorar entre 30 y 55 segundos.
- En `RetrofitClient.kt` ya se aumentaron los timeouts a 60 segundos y se creó la función `resolverMensajeErrorServidor(e)`.

### 9.2 Taxonomía de Errores y Experiencia de Usuario
Para evitar frustración en el usuario, se diseñó la siguiente diferenciación de estados en la capa de red:

| Tipo de Error | Señales Técnicas | Mensaje Amigable al Usuario | Acción de Recuperación |
| :--- | :--- | :--- | :--- |
| **Cold Start (Arranque en frío)** | `SocketTimeoutException`, `TimeoutException`, HTTP 502, 503, 504 | *"El servicio en la nube se está iniciando. Esto puede tomar unos segundos la primera vez. Intentaremos conectarnos automáticamente."* | Barra de progreso con reintento automático exponencial acotado (2s, 5s). |
| **Sin Conexión Local** | `UnknownHostException`, red desconectada | *"No hay conexión a Internet. Revisa tus datos móviles o Wi-Fi e intenta nuevamente."* | Botón manual "Reintentar". |
| **No Autorizado / Sesión Expirada** | HTTP 401 Unauthorized | *"Tu sesión ha expirado por seguridad. Por favor, inicia sesión nuevamente."* | Limpieza de token y redirección a `PantallaInicioSesion`. |
| **Acceso Restringido / Suspendido** | HTTP 403 Forbidden | *"Acceso denegado: [Mensaje explicativo del backend]"* | Presentar causa exacta (ej. centro en revisión o suspendido). |
| **Error Interno** | HTTP 500 Internal Server Error | *"El servicio experimentó un inconveniente técnico momentáneo. Nuestro equipo está informado."* | Ofrecer reintento con informe amigable. |

---

## 10. MATRIZ DE ARCHIVOS A MODIFICAR EN LA FUTURA IMPLEMENTACIÓN

Esta matriz detalla los componentes que se modificarán o crearán en las fases subsecuentes de desarrollo, manteniendo intacto el código en la presente fase de diseño:

| Componente / Capa | Archivo | Tipo de Cambio | Justificación Técnica |
| :--- | :--- | :--- | :--- |
| **Backend - Modelo** | `Backend/AdoptMe.Api/Models/Centro.cs` | Modificación | Agregar `MotivoSuspension`, `FechaSuspension`, `SuspendidoPorAdminId`, `FechaReactivacion`. |
| **Backend - DTOs** | `Backend/AdoptMe.Api/Models/SuspenderCentroDto.cs` | Creación | DTO tipado para recibir motivo obligatorio de suspensión. |
| **Backend - DTOs** | `Backend/AdoptMe.Api/Models/CentroAdminDto.cs` | Modificación | Exponer campos de suspensión y reactivación al panel de administración. |
| **Backend - DTOs** | `Backend/AdoptMe.Api/Models/CentroPropietarioDto.cs` | Modificación | Informar motivo y fecha de suspensión al propietario autenticado. |
| **Backend - Controlador** | `Backend/AdoptMe.Api/Controllers/AdminCentrosController.cs` | Modificación | Implementar `PUT /suspender` y `PUT /reactivar` con validación de roles y auditoría. |
| **Backend - SQL** | `Backend/AdoptMe.Api/Data/Neon_UX_Centros_Suspension.sql` | Creación | Script definitivo para ejecución controlada en Neon PostgreSQL (Fases futuras). |
| **Backend - Pruebas** | `Backend/AdoptMe.Api.Tests/CentrosDinamicosTests.cs` | Modificación | Pruebas de suspensión, reactivación, y bloqueo 403 en publicación de suspendidos. |
| **Android - Modelo** | `Android/app/src/main/java/com/example/adoptme/model/NotificationItem.kt` | Modificación | Ampliar con `id`, `timestamp`, `isRead`, `tipo`, `referenciaId`. |
| **Android - Modelo** | `Android/app/src/main/java/com/example/adoptme/model/MensajeChat.kt` | Modificación | Agregar `leido: Boolean` en `MensajeChat` y `mensajesNoLeidos: Int` en `ConversacionCentro`. |
| **Android - Storage** | `Android/app/src/main/java/com/example/adoptme/data/AppStorage.kt` | Modificación | Métodos para marcar leídos, calcular no leídos y filtrar avisos residuales de pruebas. |
| **Android - UI** | `Android/app/src/main/java/com/example/adoptme/components/BarraNavegacionInferior.kt` | Modificación | Agregar `BadgedBox` reactivo en pestañas de Avisos y Chat. |
| **Android - UI** | `Android/app/src/main/java/com/example/adoptme/screens/PantallaNotificaciones.kt` | Modificación | Tarjetas con acción de lectura individual, marcar todo como leído y eliminar. |
| **Android - UI** | `Android/app/src/main/java/com/example/adoptme/screens/PantallaCentro.kt` | Modificación | Rediseño responsive con métricas 2x2, tema dinámico y alerta de suspensión. |
| **Android - UI** | `Android/app/src/main/java/com/example/adoptme/screens/PantallaChat.kt` | Modificación | Marcado de mensajes como leídos al entrar al chat y badges por conversación. |
| **Android - UI** | `Android/app/src/main/java/com/example/adoptme/screens/PantallaAdministrador.kt` | Modificación | Modal y botón "Vincular responsable" con validación de rol 'Centro'. |
| **Android - UI** | `Android/app/src/main/java/com/example/adoptme/screens/PantallaPublicarMascota.kt` | Modificación | Dirección legible principal con sección técnica desplegable para Lat/Lon. |
| **Android - Pruebas** | `Android/app/src/test/java/com/example/adoptme/NotificacionesYChatTest.kt` | Creación | Pruebas unitarias de conteo de no leídos, badges 1..99/99+ y aislamiento por usuario. |

---

## 11. PLAN DE PRUEBAS PROPUESTO (TEST MATRIX)

Para validar rigurosamente la implementación cuando sea autorizada, se proyecta la siguiente suite de pruebas:

1. **Pruebas Unitarias de Notificaciones y Badges:**
   - `testNotificaciones_ContadorInicialCero()`
   - `testNotificaciones_AgregarNoLeidas_IncrementaBadge()`
   - `testNotificaciones_ContadorSupera99_MuestraTexto99Plus()`
   - `testNotificaciones_MarcarLeidaIndividual_DisminuyeContador()`
   - `testNotificaciones_MarcarTodasLeidas_ReseteaBadgeACero()`
   - `testNotificaciones_FiltradoQuirurgico_EliminaSoloPrefijosPrueba()`
2. **Pruebas de Mensajería y Chat Local:**
   - `testChat_MensajeRecibido_SeGuardaConLeidoFalse()`
   - `testChat_EntrarAConversacion_MarcaSoloMensajesRecibidosComoLeidos()`
   - `testChat_MensajesPropios_NoAfectanContadorDeNoLeidos()`
   - `testChat_AislamientoEntreUsuarios_NoFugaConversaciones()`
3. **Pruebas de Backend de Suspensión y Reactivación:**
   - `testAdmin_SuspenderCentroAprobado_PasaAActivoFalseConMotivo()`
   - `testAdmin_SuspenderSinMotivo_RetornaBadRequest400()`
   - `testAdmin_CentroSuspendido_NoApareceEnCatalogoPublico()`
   - `testCentro_PublicarMascotaSiendoSuspendido_RetornaForbidden403()`
   - `testAdmin_ReactivarCentroSuspendido_RestauraActivoTrue()`
4. **Pruebas de Geocodificación y Dirección:**
   - `testDireccion_GeocoderDisponible_ExtraeDistritoYDireccionLegible()`
   - `testDireccion_GeocoderFallaOFallaRed_AplicaFallbackSinRomperCoordenadas()`

---

## 12. EVALUACIÓN DE RIESGOS Y ESTRATEGIA DE MITIGACIÓN

| Riesgo Identificado | Impacto | Estrategia de Mitigación |
| :--- | :--- | :--- |
| **Corrupción de datos en Neon PostgreSQL** | Alto | No se ejecuta SQL en esta fase. El script de propuesta utiliza bloques `DO $$ ... $$` y DDL idempotente (`IF NOT EXISTS`). |
| **Pérdida de las 18 mascotas históricas** | Crítico | Las mascotas no tienen dependencia directa de la suspensión de centros. `CentroId` se mantiene nullable con `ON DELETE RESTRICT`. |
| **Regresiones en navegación Android** | Medio | Las nuevas propiedades en `NotificationItem` y `MensajeChat` tienen valores por defecto (`isRead = false`, `leido = false`) para garantizar compatibilidad binaria con datos preexistentes en SharedPreferences. |
| **Incompatibilidad con Render Cold Start** | Medio | Se preserva la arquitectura predeterminada de Render establecida en el hotfix reciente, enriqueciendo únicamente el feedback visual y la resiliencia en la capa cliente. |

---

## 13. CONCLUSIÓN Y ESTADO DE APROBACIÓN

- **Objetivos de la Fase UX-A cumplidos satisfactoriamente:** Se cuenta con el diagnóstico completo, los modelos de datos enriquecidos, las máquinas de estado, la especificación de componentes y la propuesta de migración SQL sin haber alterado el código funcional ni la persistencia de producción.
- **Entregables generados:**
  1. `docs/UX_A_AUDITORIA_NOTIFICACIONES_CENTROS_RESPONSIVE.md` (Este informe integral).
  2. `Backend/AdoptMe.Api/Data/Neon_UX_Centros_Suspension_PROPUESTA.sql` (Propuesta de script SQL idempotente y transaccional).
- **Siguiente paso recomendado:** Someter este informe de auditoría a revisión por parte del usuario para autorizar la planificación de las fases de implementación sucesivas.
