package com.example.adoptme.api

import com.example.adoptme.model.MascotaDto
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path

interface MascotasApi {

    // Obtener todas las mascotas desde la API
    @GET("api/Mascotas")
    suspend fun obtenerMascotas(): List<MascotaDto>

    // Obtener una mascota por su ID
    @GET("api/Mascotas/{id}")
    suspend fun obtenerMascotaPorId(
        @Path("id") id: Int
    ): Response<MascotaDto>

    // Publicar una mascota mediante JSON tradicional
    @POST("api/Mascotas")
    suspend fun publicarMascota(
        @Body mascota: MascotaDto
    ): Response<MascotaDto>

    // Publicar una mascota mediante multipart/form-data con imagen opcional (Fase A)
    @Multipart
    @POST("api/Mascotas/con-imagen")
    suspend fun publicarMascotaConImagen(
        @Part("Name") name: RequestBody,
        @Part("Species") species: RequestBody,
        @Part("Breed") breed: RequestBody?,
        @Part("Age") age: RequestBody?,
        @Part("Gender") gender: RequestBody?,
        @Part("Location") location: RequestBody?,
        @Part("Description") description: RequestBody?,
        @Part("Status") status: RequestBody?,
        @Part("Latitud") latitud: RequestBody?,
        @Part("Longitud") longitud: RequestBody?,
        @Part imagen: MultipartBody.Part?
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
