package com.example.adoptme.api

import com.example.adoptme.preferences.Preferencias
import java.util.concurrent.TimeUnit
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {

    const val URL_PRODUCCION = "https://adoptme2.onrender.com/"
    val URL_LOCAL_DEV: String = com.example.adoptme.BuildConfig.URL_LOCAL_DEV

    // Determinista: toma BuildConfig.BASE_URL como valor inicial por defecto
    @Volatile
    private var baseUrlActual: String = com.example.adoptme.BuildConfig.BASE_URL

    val baseUrl: String
        get() = baseUrlActual

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
        .writeTimeout(60, TimeUnit.SECONDS)
        .addInterceptor(authInterceptor) // <- Inyectamos el JWT aquí
        .addInterceptor(logging)
        .build()

    /**
     * Resuelve un mensaje descriptivo y amigable ante problemas de red o cold start en Render.
     */
    fun resolverMensajeErrorServidor(e: Throwable): String {
        val esTimeoutOArranque = e is java.net.SocketTimeoutException ||
            e is java.util.concurrent.TimeoutException ||
            e is java.net.ConnectException ||
            e.message?.contains("timeout", ignoreCase = true) == true ||
            e.message?.contains("failed to connect", ignoreCase = true) == true ||
            e.message?.contains("connection refused", ignoreCase = true) == true
        return if (esTimeoutOArranque) {
            "El servidor está iniciando. Intenta nuevamente en unos segundos."
        } else {
            "No se pudo conectar con el servidor. Revisa tu conexión a Internet e inténtalo nuevamente."
        }
    }

    @Volatile
    private var retrofitInstance: Retrofit = construirRetrofit(baseUrlActual)

    private fun construirRetrofit(url: String): Retrofit {
        return Retrofit.Builder()
            .baseUrl(url)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Synchronized
    fun configurarBaseUrl(nuevaUrl: String) {
        // En release o compilaciones de producción donde PERMITIR_URL_DEV es false,
        // está estrictamente prohibido alterar la URL de producción
        if (!com.example.adoptme.BuildConfig.PERMITIR_URL_DEV) {
            return
        }
        val urlFormateada = if (nuevaUrl.endsWith("/")) nuevaUrl else "$nuevaUrl/"
        if (baseUrlActual != urlFormateada) {
            baseUrlActual = urlFormateada
            retrofitInstance = construirRetrofit(urlFormateada)
            _mascotasApi = retrofitInstance.create(MascotasApi::class.java)
            _usuariosApi = retrofitInstance.create(UsuariosApi::class.java)
            _solicitudesApi = retrofitInstance.create(SolicitudesApi::class.java)
            _centrosApi = retrofitInstance.create(CentrosApi::class.java)
        }
    }

    fun restaurarBaseUrlProduccion() {
        if (!com.example.adoptme.BuildConfig.PERMITIR_URL_DEV) {
            baseUrlActual = URL_PRODUCCION
            return
        }
        configurarBaseUrl(URL_PRODUCCION)
    }

    @Volatile
    private var _mascotasApi: MascotasApi = retrofitInstance.create(MascotasApi::class.java)
    val mascotasApi: MascotasApi
        get() = _mascotasApi

    @Volatile
    private var _usuariosApi: UsuariosApi = retrofitInstance.create(UsuariosApi::class.java)
    val usuariosApi: UsuariosApi
        get() = _usuariosApi

    @Volatile
    private var _solicitudesApi: SolicitudesApi = retrofitInstance.create(SolicitudesApi::class.java)
    val solicitudesApi: SolicitudesApi
        get() = _solicitudesApi

    @Volatile
    private var _centrosApi: CentrosApi = retrofitInstance.create(CentrosApi::class.java)
    val centrosApi: CentrosApi
        get() = _centrosApi
}