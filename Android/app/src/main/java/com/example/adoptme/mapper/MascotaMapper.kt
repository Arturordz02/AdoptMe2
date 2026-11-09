package com.example.adoptme.mapper

import com.example.adoptme.R
import com.example.adoptme.model.MascotaDto
import com.example.adoptme.model.Pet
import com.example.adoptme.utils.formatearEdadCanonica

fun MascotaDto.toPet(): Pet {
    val fallbackImage = when (species.trim().lowercase()) {
        "gato" -> R.drawable.luna
        "conejo" -> R.drawable.mia
        else -> R.drawable.max
    }

    return Pet(
        id = id,
        name = name,
        species = species,
        breed = breed,
        age = formatearEdadCanonica(age),
        gender = gender,
        location = location,
        status = status,
        description = description,
        image = fallbackImage,
        photoBitmap = null,
        creadoPorCorreo = creadoPorCorreo,
        esRemota = id > 0,
        latitud = latitud,
        longitud = longitud
    )
}

fun Pet.toDto(): MascotaDto {
    return MascotaDto(
        id = id,
        name = name,
        species = species,
        breed = breed,
        age = age,
        gender = gender,
        location = location,
        status = status,
        description = description,
        imageUrl = null,
        creadoPorCorreo = creadoPorCorreo,
        latitud = latitud,
        longitud = longitud
    )
}