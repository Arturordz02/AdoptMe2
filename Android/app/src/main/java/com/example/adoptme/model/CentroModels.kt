package com.example.adoptme.model

import com.google.gson.annotations.SerializedName

/**
 * Datos públicos de un Centro de Adopción aprobado y activo (GET /api/centros).
 * Cumple con el contrato CentroPublicoDto de ASP.NET Core.
 */
data class CentroPublicoDto(
    @SerializedName("id") val id: Int,
    @SerializedName("nombre") val nombre: String,
    @SerializedName("descripcion") val descripcion: String = "",
    @SerializedName("direccion") val direccion: String = "",
    @SerializedName("distrito") val distrito: String = "",
    @SerializedName("telefono") val telefono: String = "",
    @SerializedName("correoContacto") val correoContacto: String = "",
    @SerializedName("horario") val horario: String = "",
    @SerializedName("latitud") val latitud: Double? = null,
    @SerializedName("longitud") val longitud: Double? = null,
    @SerializedName("logoUrl") val logoUrl: String? = null,
    @SerializedName("esDemostrativo") val esDemostrativo: Boolean = false
)

/**
 * Perfil y estado institucional del Centro autenticado (GET /api/centros/mi-centro).
 * Cumple con CentroPropietarioDto en el backend.
 */
data class CentroPropietarioDto(
    @SerializedName("id") val id: Int,
    @SerializedName("usuarioId") val usuarioId: Int? = null,
    @SerializedName("nombre") val nombre: String,
    @SerializedName("descripcion") val descripcion: String = "",
    @SerializedName("direccion") val direccion: String = "",
    @SerializedName("distrito") val distrito: String = "",
    @SerializedName("telefono") val telefono: String = "",
    @SerializedName("correoContacto") val correoContacto: String = "",
    @SerializedName("horario") val horario: String = "",
    @SerializedName("latitud") val latitud: Double? = null,
    @SerializedName("longitud") val longitud: Double? = null,
    @SerializedName("logoUrl") val logoUrl: String? = null,
    @SerializedName("estado") val estado: String = "Pendiente",
    @SerializedName("motivoRechazo") val motivoRechazo: String? = null,
    @SerializedName("fechaSolicitud") val fechaSolicitud: String? = null,
    @SerializedName("fechaRevision") val fechaRevision: String? = null,
    @SerializedName("esDemostrativo") val esDemostrativo: Boolean = false,
    @SerializedName("activo") val activo: Boolean = false,
    @SerializedName("fechaActualizacion") val fechaActualizacion: String? = null,
    @SerializedName("motivoSuspension") val motivoSuspension: String? = null,
    @SerializedName("fechaSuspension") val fechaSuspension: String? = null,
    @SerializedName("fechaReactivacion") val fechaReactivacion: String? = null
)

/**
 * Información completa y administrativa de un Centro para administradores (GET /api/admin/centros).
 * Cumple con CentroAdminDto en el backend.
 */
data class CentroAdminDto(
    @SerializedName("id") val id: Int,
    @SerializedName("usuarioId") val usuarioId: Int? = null,
    @SerializedName("nombre") val nombre: String,
    @SerializedName("descripcion") val descripcion: String = "",
    @SerializedName("direccion") val direccion: String = "",
    @SerializedName("distrito") val distrito: String = "",
    @SerializedName("telefono") val telefono: String = "",
    @SerializedName("correoContacto") val correoContacto: String = "",
    @SerializedName("horario") val horario: String = "",
    @SerializedName("latitud") val latitud: Double? = null,
    @SerializedName("longitud") val longitud: Double? = null,
    @SerializedName("logoUrl") val logoUrl: String? = null,
    @SerializedName("estado") val estado: String = "Pendiente",
    @SerializedName("motivoRechazo") val motivoRechazo: String? = null,
    @SerializedName("fechaSolicitud") val fechaSolicitud: String? = null,
    @SerializedName("fechaRevision") val fechaRevision: String? = null,
    @SerializedName("esDemostrativo") val esDemostrativo: Boolean = false,
    @SerializedName("activo") val activo: Boolean = false,
    @SerializedName("fechaActualizacion") val fechaActualizacion: String? = null,
    @SerializedName("nombreResponsable") val nombreResponsable: String? = null,
    @SerializedName("correoResponsable") val correoResponsable: String? = null,
    @SerializedName("telefonoResponsable") val telefonoResponsable: String? = null,
    @SerializedName("revisadoPorUsuarioId") val revisadoPorUsuarioId: Int? = null,
    @SerializedName("revisadoPorNombre") val revisadoPorNombre: String? = null,
    @SerializedName("fechaCreacion") val fechaCreacion: String? = null,
    @SerializedName("totalMascotasPublicadas") val totalMascotasPublicadas: Int = 0,
    @SerializedName("motivoSuspension") val motivoSuspension: String? = null,
    @SerializedName("fechaSuspension") val fechaSuspension: String? = null,
    @SerializedName("fechaReactivacion") val fechaReactivacion: String? = null,
    @SerializedName("suspendidoPorAdminId") val suspendidoPorAdminId: Int? = null,
    @SerializedName("suspendidoPorAdminNombre") val suspendidoPorAdminNombre: String? = null
)

/**
 * Estado resumido de un centro (EstadoCentroDto).
 */
data class EstadoCentroDto(
    @SerializedName("centroId") val centroId: Int? = null,
    @SerializedName("estado") val estado: String? = null,
    @SerializedName("activo") val activo: Boolean = false,
    @SerializedName("esDemostrativo") val esDemostrativo: Boolean = false,
    @SerializedName("motivoRechazo") val motivoRechazo: String? = null,
    @SerializedName("motivoSuspension") val motivoSuspension: String? = null,
    @SerializedName("fechaSuspension") val fechaSuspension: String? = null,
    @SerializedName("fechaReactivacion") val fechaReactivacion: String? = null
)

/**
 * Solicitud de registro institucional de Centro (POST /api/usuarios/registro-centro).
 */
data class RegistroCentroRequest(
    @SerializedName("nombreResponsable") val nombreResponsable: String,
    @SerializedName("correo") val correo: String,
    @SerializedName("contrasena") val contrasena: String,
    @SerializedName("telefonoResponsable") val telefonoResponsable: String? = null,
    @SerializedName("nombreCentro") val nombreCentro: String,
    @SerializedName("descripcion") val descripcion: String,
    @SerializedName("direccion") val direccion: String,
    @SerializedName("distrito") val distrito: String,
    @SerializedName("telefonoCentro") val telefonoCentro: String,
    @SerializedName("correoContacto") val correoContacto: String,
    @SerializedName("horario") val horario: String,
    @SerializedName("latitud") val latitud: Double? = null,
    @SerializedName("longitud") val longitud: Double? = null,
    @SerializedName("aceptacionTerminos") val aceptacionTerminos: Boolean = true
)

/**
 * Respuesta del backend al registrar un centro (ResultadoRegistroCentroDto).
 */
data class RegistroCentroResponse(
    @SerializedName("mensaje") val mensaje: String = "",
    @SerializedName("usuarioId") val usuarioId: Int = 0,
    @SerializedName("centroId") val centroId: Int = 0,
    @SerializedName("nombreCentro") val nombreCentro: String = "",
    @SerializedName("estado") val estado: String = "",
    @SerializedName("rol") val rol: String = "Centro"
)

/**
 * Solicitud de edición del perfil institucional por su titular (PUT /api/centros/mi-centro).
 */
data class ActualizarMiCentroRequest(
    @SerializedName("nombre") val nombre: String,
    @SerializedName("descripcion") val descripcion: String,
    @SerializedName("direccion") val direccion: String,
    @SerializedName("distrito") val distrito: String,
    @SerializedName("telefono") val telefono: String,
    @SerializedName("correoContacto") val correoContacto: String,
    @SerializedName("horario") val horario: String,
    @SerializedName("latitud") val latitud: Double? = null,
    @SerializedName("longitud") val longitud: Double? = null
)

/**
 * Rechazo administrativo con justificación obligatoria (PUT /api/admin/centros/{id}/rechazar).
 */
data class RechazarCentroRequest(
    @SerializedName("motivo") val motivo: String
)

/**
 * Suspensión administrativa con justificación obligatoria (PUT /api/admin/centros/{id}/suspender).
 */
data class SuspenderCentroRequest(
    @SerializedName("motivo") val motivo: String
)

/**
 * Asignación / Vinculación administrativa de usuario responsable (PUT /api/admin/centros/{id}/vincular-responsable).
 */
data class AsignarPropietarioCentroRequest(
    @SerializedName("usuarioId") val usuarioId: Int
)

/**
 * Respuesta genérica de operaciones administrativas o mutaciones de centro.
 */
data class OperacionCentroResponse(
    @SerializedName("mensaje") val mensaje: String = "",
    @SerializedName("id") val id: Int? = null,
    @SerializedName("estado") val estado: String? = null,
    @SerializedName("activo") val activo: Boolean? = null,
    @SerializedName("motivoRechazo") val motivoRechazo: String? = null,
    @SerializedName("motivoSuspension") val motivoSuspension: String? = null,
    @SerializedName("fechaSuspension") val fechaSuspension: String? = null,
    @SerializedName("fechaReactivacion") val fechaReactivacion: String? = null,
    @SerializedName("fechaRevision") val fechaRevision: String? = null,
    @SerializedName("centroId") val centroId: Int? = null,
    @SerializedName("usuarioId") val usuarioId: Int? = null,
    @SerializedName("logoUrl") val logoUrl: String? = null
)

/**
 * Mapeo de conveniencia desde CentroPublicoDto hacia el modelo CentroAdopcion tradicional de UI.
 */
fun CentroPublicoDto.toCentroAdopcion(): CentroAdopcion {
    return CentroAdopcion(
        id = id,
        nombre = nombre,
        ubicacion = if (distrito.isNotBlank()) "$distrito, Lima" else direccion,
        descripcion = descripcion,
        horarioAtencion = horario,
        telefono = telefono,
        direccion = direccion,
        contactoEjemplo = correoContacto.ifBlank { telefono },
        direccionEjemplo = direccion,
        latitud = latitud,
        longitud = longitud,
        logoUrl = logoUrl,
        esDemostrativo = esDemostrativo
    )
}
