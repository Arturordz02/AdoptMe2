package com.example.adoptme.model

/**
 * Modelo de datos para representar un Centro o Albergue de Adopción en la interfaz.
 * Cada centro cuenta con un ID numérico único e inmutable para aislar
 * sus conversaciones y gestiones.
 */
data class CentroAdopcion(
    val id: Int,
    val nombre: String,
    val ubicacion: String,
    val descripcion: String,
    val horarioAtencion: String,
    val telefono: String = "",
    val direccion: String = "",
    val contactoEjemplo: String = telefono,
    val direccionEjemplo: String = direccion,
    val latitud: Double? = null,
    val longitud: Double? = null,
    val logoUrl: String? = null,
    val esDemostrativo: Boolean = false
)
