package com.example.adoptme.data.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

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
    val rol: String,
    val token: String,
    val expiracion: String
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
}

