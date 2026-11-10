# INFORME TÉCNICO DE CIERRE — FASE C8B: REINICIO CONTROLADO DE DATOS DEMOSTRATIVOS EN NEON

**Proyecto:** AdoptMe — Plataforma de Adopción Responsable  
**Repositorio:** `D:\Escritorio D\T2_GRUPO3_ DESARROLLO_APP\AdoptMe2_Entrega4_Git`  
**Rama activa:** `feature/ux-notificaciones-centros-responsive`  
**Base de Datos:** Neon PostgreSQL 16 (Producción / Render API)  
**Fecha:** 10 de noviembre de 2026  
**Estado:** EJECUCIÓN TRANSACCIONAL EXITOSA — 100% VALIDADO Y COHERENTE

---

## 1. Resumen Ejecutivo de la Fase C8B

La Fase C8B ejecutó exitosamente el reinicio seguro y controlado del catálogo de datos demostrativos en Neon PostgreSQL, satisfaciendo todas las restricciones de integridad y directrices autorizadas por el usuario.

Se preservaron intactas las cuentas de administradores, el usuario vinculado al centro demostrativo oficial, el adoptante demostrativo de referencia y la cuenta del desarrollador. Se purgó el histórico de mascotas sin georreferenciación y solicitudes residuales, reemplazándolas por un dataset limpio, balanceado (perros, gatos y conejos) con coordenadas exactas de Lima para su renderizado óptimo en Google Maps, y fotografías de alta resolución vía HTTPS.

---

## 2. Precondiciones Verificadas (Paso 1)

Antes de cualquier mutación, se ejecutaron consultas `SELECT` de validación estricta sobre Neon PostgreSQL:

| Métrica / Entidad | Conteo / Valor Encontrado | Esperado | Estado |
| :--- | :---: | :---: | :---: |
| **Total Usuarios** | `28` | `28` | Aprobado |
| **Total Centros** | `5` | `5` | Aprobado |
| **Total Mascotas** | `19` | `19` | Aprobado |
| **Total Solicitudes** | `14` | `14` | Aprobado |
| **Cuenta ID 32** | `teddy@gmail.com` | `teddy@gmail.com` | Aprobado |
| **Usuario ID 3** | `arthur01961@outlook.com` | `arthur01961@outlook.com` | Aprobado |
| **Usuario ID 5** | `admin@adoptme.com` | `admin@adoptme.com` | Aprobado |
| **Usuario ID 6** | `centro@adoptme.com` | `centro@adoptme.com` | Aprobado |
| **Usuario ID 7** | `usuario@adoptme.com` | `usuario@adoptme.com` | Aprobado |
| **Centros 1 al 5** | Existentes y activos | IDs 1, 2, 3, 4, 5 | Aprobado |
| **Centro 1 vinculado** | `UsuarioId = 6` | `UsuarioId = 6` | Aprobado |

---

## 3. Respaldo Recuperable Fuera del Repositorio (Paso 2)

Se generó un respaldo completo en formato JSON de todas las tablas operativas antes de iniciar el procedimiento. El respaldo se encuentra almacenado fuera del repositorio de Git, sin exponer secretos ni ser añadido al control de versiones:

- **Ruta del Respaldo:** `D:\Escritorio D\T2_GRUPO3_ DESARROLLO_APP\AdoptMe_Backup_C8B_2026-11-10\`
- **Archivos y Tamaños:**
  - `Usuarios_backup.json` (9,284 bytes — 2026-11-10 16:00:22)
  - `Centros_backup.json` (5,387 bytes — 2026-11-10 16:00:22)
  - `Mascotas_backup.json` (13,085 bytes — 2026-11-10 16:00:22)
  - `Solicitudes_backup.json` (10,526 bytes — 2026-11-10 16:00:23)

---

## 4. Revisión de Cloudinary (Paso 4)

Se inspeccionó la presencia de identificadores públicos de Cloudinary (`ImagePublicId`) en las 19 mascotas previas:
- **Mascotas con `ImagePublicId` activo:** 1 (Mascota #41: Perro).
- **Mascotas sin `ImagePublicId`:** 18 mascotas.
- **Acción sobre Cloudinary:** **CERO eliminaciones**. La imagen previa permanece en el storage de Cloudinary para no afectar dependencias externas ni romper referencias. Se documenta como potencial recurso huérfano para una limpieza futura programada.

---

## 5. Correcciones Aplicadas al Script SQL (Paso 3)

El script [Neon_C8_Reset_Datos_Demo_PROPUESTA.sql](file:///D:/Escritorio%20D/T2_GRUPO3_%20DESARROLLO_APP/AdoptMe2_Entrega4_Git/Backend/AdoptMe.Api/Data/Neon_C8_Reset_Datos_Demo_PROPUESTA.sql) fue ajustado con las siguientes directrices de seguridad:
1. Se excluyó el ID 32 (`teddy@gmail.com`) de la tabla de protección temporal, autorizando su depuración segura al no contar con centro vinculado.
2. Se restringió la lista blanca de protección exclusivamente a los usuarios con IDs y correos verificados: 3, 5, 6 y 7.
3. Se estableció `ImagePublicId = NULL` para todas las fotografías externas provistas vía HTTPS de Unsplash, eliminando prefijos artificiales.
4. Se reutilizó el hash de contraseña almacenado del usuario demo (`usuario@adoptme.com`) mediante consulta interna sin imprimirlo, sin exponerlo en texto plano y abortando mediante excepción si resultaba nulo o vacío.
5. Se añadieron aserciones previas con `RAISE EXCEPTION` que impiden borrar cualquier usuario vinculado a centros.
6. Se incorporaron comprobaciones posteriores de conteos esperados y distribución de estados.

---

## 6. Ensayo Obligatorio con ROLLBACK (Paso 5)

Se ejecutó la transacción completa en Neon PostgreSQL manteniendo la cláusula `ROLLBACK;`.

- **Métricas simuladas dentro de la transacción:**
  - Usuarios: `7` (4 protegidos + 3 nuevos adoptantes demo).
  - Centros: `5`.
  - Mascotas: `6` (Rocky, Mía, Copito, Luna, Simba, Tambor).
  - Solicitudes: `1` (Luna postulado por `usuario@adoptme.com`).
- **Verificación inmediata post-ROLLBACK en la base de datos real:**
  - Usuarios: `28`
  - Centros: `5`
  - Mascotas: `19`
  - Solicitudes: `14`
- **Conclusión del ensayo:** Cero mutaciones en la base de datos durante el ensayo; confirmación de compatibilidad e integridad referencial al 100%.

---

## 7. Ejecución Real con COMMIT (Paso 6) y Verificación Posterior (Paso 7)

Tras el éxito del ensayo, se generó el script canónico [Neon_C8_Reset_Datos_Demo_EJECUTADO.sql](file:///D:/Escritorio%20D/T2_GRUPO3_%20DESARROLLO_APP/AdoptMe2_Entrega4_Git/Backend/AdoptMe.Api/Data/Neon_C8_Reset_Datos_Demo_EJECUTADO.sql) sustituyendo `ROLLBACK;` por `COMMIT;` y ejecutándolo en una sola transacción atómica.

### 7.1 Conteos Finales en Neon PostgreSQL

| Entidad | Conteo Final | Conteo Previo | Diferencia | Estado |
| :--- | :---: | :---: | :---: | :--- |
| **`Usuarios`** | **7** | 28 | -21 | 4 protegidos + 3 nuevos adoptantes coherentes |
| **`Centros`** | **5** | 5 | 0 | 100% de centros demo conservados y activos |
| **`Mascotas`** | **6** | 19 | -13 | 6 mascotas georreferenciadas con GPS |
| **`Solicitudes`** | **1** | 14 | -13 | 1 solicitud representativa en evaluación |

### 7.2 Usuarios Protegidos e Intactos
- **ID 3:** `arthur01961@outlook.com` (Adoptante / Administrador técnico)
- **ID 5:** `admin@adoptme.com` (Administrador Demo)
- **ID 6:** `centro@adoptme.com` (Centro Demo, Responsable del Centro #1)
- **ID 7:** `usuario@adoptme.com` (Adoptante Demo oficial)
- **ID 32 (`teddy@gmail.com`):** Eliminado con éxito (cuenta incompleta sin centro).

### 7.3 Nuevos Adoptantes Demostrativos Insertados
- **Carlos Mendoza:** `carlos.mendoza@adoptme.pe` — Tel: 987112233 (Surco)
- **Ana Valdivia:** `ana.valdivia@adoptme.pe` — Tel: 987223344 (Miraflores)
- **Lucía Paredes:** `lucia.paredes@adoptme.pe` — Tel: 987334455 (SJL)

### 7.4 Mascotas Georreferenciadas Insertadas

| ID | Nombre | Especie | Raza | Estado | Centro | Ubicación | Latitud | Longitud | Foto HTTPS |
| :---: | :--- | :--- | :--- | :--- | :---: | :--- | :---: | :---: | :---: |
| **48** | **Rocky** | Perro | Labrador Retriever | `Disponible` | Centro 1 | SJL, Lima | `-12.0125` | `-77.0018` | Unsplash HD |
| **49** | **Mía** | Gato | Siamés | `Disponible` | Centro 3 | Miraflores, Lima | `-12.1198` | `-77.0345` | Unsplash HD |
| **50** | **Copito** | Conejo | Belier Enano | `Disponible` | Centro 2 | Surco, Lima | `-12.1350` | `-76.9820` | Unsplash HD |
| **51** | **Luna** | Perro | Beagle | `En proceso` | Centro 4 | Ate Vitarte, Lima | `-12.0480` | `-76.9250` | Unsplash HD |
| **52** | **Simba** | Gato | Atigrado Europeo | `Disponible` | Centro 5 | Comas, Lima | `-11.9350` | `-77.0510` | Unsplash HD |
| **53** | **Tambor** | Conejo | Holandés Enano | `Disponible` | Centro 1 | SJL, Lima | `-12.0060` | `-77.0080` | Unsplash HD |

### 7.5 Solicitud Demostrativa Activa
- **ID Solicitud:** 16
- **Mascota:** Luna (ID 51)
- **Postulante:** Adoptante Demo (`usuario@adoptme.com`, ID 7)
- **Estado:** `En revisión`
- **Observación:** *Solicitud demostrativa en proceso de evaluación.*

### 7.6 Integridad Referencial Post-Commit
- Mascotas huérfanas con `CentroId` inexistente: **0**.
- Solicitudes huérfanas con `MascotaId` inexistente: **0**.
- Solicitudes huérfanas con `AdoptanteId` inexistente: **0**.
- Usuarios protegidos eliminados: **0**.
- Alteraciones estructurales o de esquema en base de datos: **0**.

---

## 8. Verificación de API Pública de Solo Lectura (Paso 8)

Se realizó una consulta HTTP GET de solo lectura contra la API de producción activa:
- **Endpoint:** `GET https://adoptme2.onrender.com/api/Mascotas`
- **Código de Respuesta:** `200 OK`
- **Total Mascotas Devueltas:** `6` (Rocky, Copito, Luna, Simba, Mía, Tambor).
- **Georreferenciación:** Las 6 mascotas exponen correctamente `latitud` y `longitud` para Google Maps.
- **Validación de Imágenes:** Las 6 URLs HTTPS de Unsplash fueron verificadas mediante petición HEAD, respondiendo con `200 OK`.

---

## 9. Procedimiento de Recuperación y Restauración

En caso fortuito de requerir restaurar el estado previo de los datos a partir del respaldo:
1. Las entidades pueden importarse utilizando los archivos JSON preservados en:  
   `D:\Escritorio D\T2_GRUPO3_ DESARROLLO_APP\AdoptMe_Backup_C8B_2026-11-10\`
2. El orden de restauración requerido respetando claves foráneas es:
   1. `Usuarios_backup.json`
   2. `Centros_backup.json`
   3. `Mascotas_backup.json`
   4. `Solicitudes_backup.json`

---

## 10. Garantías de Seguridad y Control Git

- **Cloudinary:** No se modificaron ni borraron recursos.
- **Render:** No se ejecutó despliegue de código nuevo.
- **Git Push / Merge / PR:** No se ejecutó push ni merge a main.
- **Secretos:** No se expusieron contraseñas, hashes ni cadenas de conexión en el repositorio ni en la documentación.
