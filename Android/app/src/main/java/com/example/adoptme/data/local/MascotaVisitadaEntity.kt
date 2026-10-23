package com.example.adoptme.data.local

import androidx.room.Entity
import androidx.room.Index
import com.example.adoptme.model.Pet

@Entity(
    tableName = "mascotas_visitadas",
    primaryKeys = ["correoUsuario", "mascotaId"],
    indices = [Index(value = ["correoUsuario", "mascotaId"], unique = true)]
)
data class MascotaVisitadaEntity(
    val correoUsuario: String,
    val mascotaId: Int,
    val usuarioId: Int = 0,
    val nombre: String,
    val especie: String = "",
    val raza: String = "",
    val edad: String = "",
    val genero: String = "",
    val ubicacion: String = "",
    val estado: String = "Disponible",
    val descripcion: String = "",
    val imagenResId: Int = 0,
    val creadoPorCorreo: String = "",
    val fechaUltimaVisita: Long = System.currentTimeMillis()
) {
    fun toPet(): Pet {
        return Pet(
            id = mascotaId,
            name = nombre,
            species = especie,
            breed = raza,
            age = edad,
            gender = genero,
            location = ubicacion,
            status = estado,
            description = descripcion,
            image = imagenResId,
            creadoPorCorreo = creadoPorCorreo,
            esRemota = true
        )
    }
}

