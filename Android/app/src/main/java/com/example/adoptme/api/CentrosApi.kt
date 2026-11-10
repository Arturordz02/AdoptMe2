package com.example.adoptme.api

import com.example.adoptme.model.ActualizarMiCentroRequest
import com.example.adoptme.model.AsignarPropietarioCentroRequest
import com.example.adoptme.model.CentroAdminDto
import com.example.adoptme.model.CentroPropietarioDto
import com.example.adoptme.model.CentroPublicoDto
import com.example.adoptme.model.OperacionCentroResponse
import com.example.adoptme.model.RechazarCentroRequest
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

interface CentrosApi {

    // =========================================================
    // ENDPOINTS PÚBLICOS
    // =========================================================

    @GET("api/centros")
    suspend fun obtenerCentros(
        @Query("distrito") distrito: String? = null,
        @Query("q") q: String? = null
    ): List<CentroPublicoDto>

    @GET("api/centros/{id}")
    suspend fun obtenerPorId(
        @Path("id") id: Int
    ): Response<CentroPublicoDto>

    // =========================================================
    // ENDPOINTS DEL CENTRO AUTENTICADO (PROPIETARIO)
    // =========================================================

    @GET("api/centros/mi-centro")
    suspend fun obtenerMiCentro(): Response<CentroPropietarioDto>

    @PUT("api/centros/mi-centro")
    suspend fun actualizarMiCentro(
        @Body request: ActualizarMiCentroRequest
    ): Response<CentroPropietarioDto>

    @POST("api/centros/mi-centro/reenviar")
    suspend fun reenviarSolicitud(): Response<OperacionCentroResponse>

    @Multipart
    @PUT("api/centros/mi-centro/logo")
    suspend fun actualizarLogo(
        @Part imagen: MultipartBody.Part
    ): Response<OperacionCentroResponse>

    // =========================================================
    // ENDPOINTS DE ADMINISTRACIÓN DE CENTROS
    // =========================================================

    @GET("api/admin/centros")
    suspend fun listarCentrosAdmin(
        @Query("estado") estado: String? = null,
        @Query("activo") activo: Boolean? = null,
        @Query("esDemostrativo") esDemostrativo: Boolean? = null,
        @Query("q") q: String? = null
    ): Response<List<CentroAdminDto>>

    @GET("api/admin/centros/{id}")
    suspend fun obtenerDetalleAdmin(
        @Path("id") id: Int
    ): Response<CentroAdminDto>

    @PUT("api/admin/centros/{id}/aprobar")
    suspend fun aprobarCentro(
        @Path("id") id: Int
    ): Response<OperacionCentroResponse>

    @PUT("api/admin/centros/{id}/rechazar")
    suspend fun rechazarCentro(
        @Path("id") id: Int,
        @Body request: RechazarCentroRequest
    ): Response<OperacionCentroResponse>

    @PUT("api/admin/centros/{id}/activar")
    suspend fun activarCentro(
        @Path("id") id: Int
    ): Response<OperacionCentroResponse>

    @PUT("api/admin/centros/{id}/desactivar")
    suspend fun desactivarCentro(
        @Path("id") id: Int
    ): Response<OperacionCentroResponse>

    @PUT("api/admin/centros/{id}/asignar-propietario")
    suspend fun asignarPropietario(
        @Path("id") id: Int,
        @Body request: AsignarPropietarioCentroRequest
    ): Response<OperacionCentroResponse>
}
