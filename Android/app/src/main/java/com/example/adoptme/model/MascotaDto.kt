package com.example.adoptme.model


data class MascotaDto(
    val id: Int = 0,
    val name: String = "",
    val species: String = "",
    val breed: String = "",
    val age: String = "",
    val gender: String = "",
    val location: String = "",
    val status: String = "Disponible",
    val description: String = "",
    val imageUrl: String? = null,
    val creadoPorCorreo: String = "",
    val fechaCreacion: String? = null,
    val latitud: Double? = null,
    val longitud: Double? = null
)