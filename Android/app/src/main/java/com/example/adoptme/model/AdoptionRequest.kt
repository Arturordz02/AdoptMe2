package com.example.adoptme.model


data class AdoptionRequest(
    val petId: Int = 0,
    val petName: String,
    val petSpecies: String = "",
    val applicantName: String = "",
    val phone: String = "",
    val email: String = "",
    val address: String = "",
    val housingType: String = "",
    val timeAvailability: String = "",
    val previousExperience: String = "",
    val reason: String = "",
    val status: String = "Pendiente",
    val id: Int = 0,
    val observacion: String? = null
)