package com.example.adoptme.api

import com.example.adoptme.preferences.Preferencias
import java.util.concurrent.TimeUnit
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {

    private const val BASE_URL = "https://adoptme2.onrender.com/"

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
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
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
    val solicitudesApi: SolicitudesApi = retrofit.create(SolicitudesApi::class.java)
}