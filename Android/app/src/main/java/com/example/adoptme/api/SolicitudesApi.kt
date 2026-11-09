package com.example.adoptme.api

import com.example.adoptme.model.ActualizarEstadoSolicitudRequestDto
import com.example.adoptme.model.CrearSolicitudRequestDto
import com.example.adoptme.model.SolicitudResponseDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
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

    /**
     * Consultar todas las solicitudes registradas (GET /api/Solicitudes/admin).
     * Exclusivo para usuarios con rol Admin.
     */
    @GET("api/Solicitudes/admin")
    suspend fun obtenerSolicitudesAdmin(): Response<List<SolicitudResponseDto>>

    /**
     * Actualizar el estado de una solicitud de adopción (PUT /api/Solicitudes/{id}/estado).
     * Exclusivo para usuarios con rol Admin.
     */
    @PUT("api/Solicitudes/{id}/estado")
    suspend fun actualizarEstadoSolicitud(
        @Path("id") id: Int,
        @Body request: ActualizarEstadoSolicitudRequestDto
    ): Response<SolicitudResponseDto>
}