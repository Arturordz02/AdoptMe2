package com.example.adoptme.data.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface SolicitudesApi {

    /**
     * Enviar una nueva solicitud de adopción (POST /api/Solicitudes).
     * Requiere encabezado Authorization con Bearer JWT.
     */
    @POST("api/Solicitudes")
    suspend fun crearSolicitud(
        @Body solicitud: CrearSolicitudRequestDto
    ): Response<SolicitudResponseDto>

    /**
     * Consultar las solicitudes realizadas por el adoptante autenticado (GET /api/Solicitudes/mis-solicitudes).
     */
    @GET("api/Solicitudes/mis-solicitudes")
    suspend fun obtenerMisSolicitudes(): Response<List<SolicitudResponseDto>>

    /**
     * Consultar el detalle de una solicitud específica por su Id (GET /api/Solicitudes/{id}).
     */
    @GET("api/Solicitudes/{id}")
    suspend fun obtenerSolicitudPorId(
        @Path("id") id: Int
    ): Response<SolicitudResponseDto>
}

