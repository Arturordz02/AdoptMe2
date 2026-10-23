package com.example.adoptme.data.remote

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Body
import retrofit2.http.Path
import retrofit2.http.PUT
import retrofit2.http.DELETE

interface MascotasApi {

    // Obtener todas las mascotas desde la API
    @GET("api/Mascotas")
    suspend fun obtenerMascotas(): List<MascotaDto>

    // Obtener una mascota por su ID
    @GET("api/Mascotas/{id}")
    suspend fun obtenerMascotaPorId(
        @Path("id") id: Int
    ): Response<MascotaDto>

    // Publicar una mascota
    @POST("api/Mascotas")
    suspend fun publicarMascota(
        @Body mascota: MascotaDto
    ): Response<MascotaDto>

    // Actualizar una mascota
    @PUT("api/Mascotas/{id}")
    suspend fun actualizarMascota(
        @Path("id") id: Int,
        @Body mascota: MascotaDto
    ): Response<Unit>

    // Eliminar una mascota
    @DELETE("api/Mascotas/{id}")
    suspend fun eliminarMascota(
        @Path("id") id: Int
    ): Response<Unit>
}
