package com.example.adoptme.model

import com.google.gson.annotations.SerializedName

/**
 * DTO para enviar una nueva solicitud de adopción al backend ASP.NET Core (POST /api/Solicitudes).
 * Corresponde a CrearSolicitudDto.cs en el servidor.
 */
data class CrearSolicitudRequestDto(
    @SerializedName("mascotaId")
    val mascotaId: Int,

    @SerializedName("applicantName")
    val applicantName: String,

    @SerializedName("phone")
    val phone: String,

    @SerializedName("address")
    val address: String,

    @SerializedName("housingType")
    val housingType: String,

    @SerializedName("timeAvailability")
    val timeAvailability: String? = "",

    @SerializedName("previousExperience")
    val previousExperience: String? = "",

    @SerializedName("reason")
    val reason: String
)

/**
 * DTO de respuesta que devuelve el backend al crear o consultar solicitudes.
 * Corresponde a SolicitudResponseDto.cs en el servidor.
 */
data class SolicitudResponseDto(
    @SerializedName("id")
    val id: Int,

    @SerializedName("mascotaId")
    val mascotaId: Int,

    @SerializedName("adoptanteId")
    val adoptanteId: Int,

    @SerializedName("petName")
    val petName: String = "",

    @SerializedName("petSpecies")
    val petSpecies: String = "",

    @SerializedName("applicantName")
    val applicantName: String = "",

    @SerializedName("phone")
    val phone: String = "",

    @SerializedName("email")
    val email: String = "",

    @SerializedName("address")
    val address: String = "",

    @SerializedName("housingType")
    val housingType: String = "",

    @SerializedName("timeAvailability")
    val timeAvailability: String? = null,

    @SerializedName("previousExperience")
    val previousExperience: String? = null,

    @SerializedName("reason")
    val reason: String = "",

    @SerializedName("status")
    val status: String = "Pendiente",

    @SerializedName("observacion")
    val observacion: String? = null,

    @SerializedName("fechaSolicitud")
    val fechaSolicitud: String? = null,

    @SerializedName("fechaActualizacion")
    val fechaActualizacion: String? = null
)

/**
 * DTO para actualizar el estado de una solicitud de adopción (PUT /api/Solicitudes/{id}/estado).
 * Corresponde a ActualizarEstadoSolicitudDto.cs en el servidor.
 */
data class ActualizarEstadoSolicitudRequestDto(
    @SerializedName("status")
    val status: String,

    @SerializedName("observacion")
    val observacion: String? = null
)
