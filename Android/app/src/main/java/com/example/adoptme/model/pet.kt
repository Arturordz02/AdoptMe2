package com.example.adoptme.model

import android.graphics.Bitmap

data class Pet(
    val id: Int,
    val name: String,
    val species: String,
    val breed: String,
    val age: String,
    val gender: String,
    val location: String = "Lima",
    val status: String = "Disponible",
    val description: String,
    val image: Int = 0,
    val photoBitmap: Bitmap? = null,
    val imageUrl: String? = null,
    val imagePublicId: String? = null,
    val creadoPorCorreo: String = "",
    val esRemota: Boolean = false,
    val latitud: Double? = null,
    val longitud: Double? = null
) {
    /**
     * Clave única y estable para LazyColumn/LazyRow en Compose.
     * Previene colisiones por IDs idénticos entre mascotas remotas de Neon y mascotas locales/demostrativas.
     */
    val claveCompose: String
        get() = if (esRemota) "remota-$id" else "demo-$id"
}
