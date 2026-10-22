package com.example.adoptme.data.remote

import com.example.adoptme.SharedPreferences.Preferencias
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor

object RetrofitClient {

    private const val BASE_URL = "https://adoptme-api-bzc8h7fdb3hcccac.westus3-01.azurewebsites.net/"

    // Permitimos inyectar las Preferencias para leer el token
    var preferencias: Preferencias? = null

    // Este interceptor atrapa la petición y le pega el Token JWT
    private val authInterceptor = Interceptor { chain ->
        val requestBuilder = chain.request().newBuilder()
        val token = preferencias?.obtenerTokenSesionActiva()

        if (!token.isNullOrBlank()) {
            requestBuilder.addHeader("Authorization", "Bearer $token")
        }
        chain.proceed(requestBuilder.build())
    }

    private val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BASIC
    }

    private val client = OkHttpClient.Builder()
        .addInterceptor(authInterceptor) // <- Inyectamos el JWT aquí
        .addInterceptor(logging)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val mascotasApi: MascotasApi = retrofit.create(MascotasApi::class.java)
    val usuariosApi: UsuariosApi = retrofit.create(UsuariosApi::class.java)
}