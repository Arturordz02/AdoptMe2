package com.example.adoptme.mapper

import com.example.adoptme.model.AdoptionRequest
import com.example.adoptme.model.CrearSolicitudRequestDto
import com.example.adoptme.model.SolicitudResponseDto

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
 * Los campos opcionales se normalizan de forma segura a cadena vacía sin texto "null".
 */
fun SolicitudResponseDto.toDomain(): AdoptionRequest {
    return AdoptionRequest(
        petId = this.mascotaId,
        petName = this.petName?.takeIf { it != "null" } ?: "",
        petSpecies = this.petSpecies?.takeIf { it != "null" } ?: "",
        applicantName = this.applicantName?.takeIf { it != "null" } ?: "",
        phone = this.phone?.takeIf { it != "null" } ?: "",
        email = this.email?.takeIf { it != "null" } ?: "",
        address = this.address?.takeIf { it != "null" } ?: "",
        housingType = this.housingType?.takeIf { it != "null" } ?: "",
        timeAvailability = this.timeAvailability?.takeIf { it != "null" } ?: "",
        previousExperience = this.previousExperience?.takeIf { it != "null" } ?: "",
        reason = this.reason?.takeIf { it != "null" } ?: "",
        status = if (this.status.isNullOrBlank() || this.status == "null") "Pendiente" else this.status,
        id = this.id
    )
}