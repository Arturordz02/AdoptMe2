package com.example.adoptme.data.remote

import com.example.adoptme.model.Pet

fun MascotaDto.toPet(): Pet {
    return Pet(
        id = id,
        name = name,
        species = species,
        breed = breed,
        age = age,
        gender = gender,
        location = location,
        status = status,
        description = description,
        image = 0,
        photoBitmap = null,
        creadoPorCorreo = creadoPorCorreo
    )
}
