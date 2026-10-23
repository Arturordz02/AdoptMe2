package com.example.adoptme.data.remote

import com.example.adoptme.model.AdoptionRequest

/**
 * Convierte un modelo de dominio AdoptionRequest a su equivalente CrearSolicitudRequestDto.
 */
fun AdoptionRequest.toCrearSolicitudDto(idMascotaConfirmado: Int = this.petId): CrearSolicitudRequestDto {
    return CrearSolicitudRequestDto(
        mascotaId = if (idMascotaConfirmado > 0) idMascotaConfirmado else this.petId,
        applicantName = this.applicantName.trim(),
        phone = this.phone.trim(),
        address = this.address.trim(),
        housingType = this.housingType.trim(),
        timeAvailability = this.timeAvailability.trim(),
        previousExperience = this.previousExperience.trim(),
        reason = this.reason.trim()
    )
}

/**
 * Convierte la respuesta del backend SolicitudResponseDto a nuestro modelo de dominio AdoptionRequest.
 */
fun SolicitudResponseDto.toDomain(): AdoptionRequest {
    return AdoptionRequest(
        petId = this.mascotaId,
        petName = this.petName,
        petSpecies = this.petSpecies,
        applicantName = this.applicantName,
        phone = this.phone,
        email = this.email,
        address = this.address,
        housingType = this.housingType,
        timeAvailability = this.timeAvailability ?: "",
        previousExperience = this.previousExperience ?: "",
        reason = this.reason,
        status = this.status,
        id = this.id
    )
}

