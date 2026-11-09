package com.example.adoptme.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT

data class RegistroUsuarioRequest(
    val nombre: String,
    val correo: String,
    val contrasena: String,
    val telefono: String
)

data class RegistroUsuarioResponse(
    val mensaje: String,
    val id: Int,
    val nombre: String,
    val correo: String,
    val rol: String
)

data class LoginUsuarioRequest(
    val correo: String,
    val contrasena: String
)

data class LoginUsuarioResponse(
    val mensaje: String,
    val id: Int,
    val nombre: String,
    val correo: String,
    val telefono: String = "",
    val rol: String,
    val token: String,
    val expiracion: String
)

data class PerfilUsuarioResponse(
    val id: Int,
    val nombre: String,
    val correo: String,
    val telefono: String = "",
    val rol: String
)

data class ActualizarPerfilRequest(
    val nombre: String,
    val telefono: String
)

interface UsuariosApi {

    @POST("api/Usuarios/registro")
    suspend fun registrarUsuario(
        @Body datos: RegistroUsuarioRequest
    ): Response<RegistroUsuarioResponse>

    @POST("api/Usuarios/login")
    suspend fun iniciarSesion(
        @Body datos: LoginUsuarioRequest
    ): Response<LoginUsuarioResponse>

    @GET("api/Usuarios/perfil")
    suspend fun obtenerPerfil(): Response<PerfilUsuarioResponse>

    @PUT("api/Usuarios/perfil")
    suspend fun actualizarPerfil(
        @Body datos: ActualizarPerfilRequest
    ): Response<PerfilUsuarioResponse>
}