package com.example.adoptme

import com.example.adoptme.api.CategoriaErrorConexion
import com.example.adoptme.api.RetrofitClient
import com.example.adoptme.utils.ReverseGeocodingHelper
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeoutException

class GeolocalizacionYPanelCentroTest {

    // =========================================================================
    // 1. FORMATEO DE DIRECCIÓN Y DISTRITO CON REVERSE GEOCODING HELPER
    // =========================================================================

    @Test
    fun formatearDireccion_conCalleYNumero_generaDireccionFisicaLimpia() {
        val resultado = ReverseGeocodingHelper.formatearDireccion(
            thoroughfare = "Av. Javier Prado Este",
            subThoroughfare = "1234",
            featureName = "1234",
            subLocality = "San Isidro",
            locality = "Lima",
            subAdminArea = "Lima",
            adminArea = "Municipalidad Metropolitana de Lima",
            countryName = "Perú",
            latitud = -12.0934,
            longitud = -77.0256
        )

        assertEquals("Av. Javier Prado Este 1234", resultado.direccionCompleta)
        assertEquals("San Isidro", resultado.distrito)
        assertEquals("Lima", resultado.ciudad)
        assertEquals("Perú", resultado.pais)
        assertTrue(resultado.fueEncontrada)
    }

    @Test
    fun formatearDireccion_sinCallePeroConFeatureName_usaFeatureName() {
        val resultado = ReverseGeocodingHelper.formatearDireccion(
            thoroughfare = null,
            subThoroughfare = null,
            featureName = "Parque Kennedy",
            subLocality = "Miraflores",
            locality = "Lima",
            subAdminArea = "Lima",
            adminArea = "Lima",
            countryName = "Perú",
            latitud = -12.1218,
            longitud = -77.0298
        )

        assertEquals("Parque Kennedy", resultado.direccionCompleta)
        assertEquals("Miraflores", resultado.distrito)
        assertTrue(resultado.fueEncontrada)
    }

    @Test
    fun formatearDireccion_conFeatureNameDeCoordenadas_noUsaCoordenadasComoCalle() {
        val resultado = ReverseGeocodingHelper.formatearDireccion(
            thoroughfare = null,
            subThoroughfare = null,
            featureName = "-12.1218, -77.0298",
            subLocality = "Barranco",
            locality = "Lima",
            subAdminArea = "Lima",
            adminArea = "Lima",
            countryName = "Perú",
            latitud = -12.1485,
            longitud = -77.0210
        )

        assertEquals("Barranco", resultado.direccionCompleta)
        assertEquals("Barranco", resultado.distrito)
    }

    @Test
    fun formatearDireccion_cadenaFallbackDistrito_priorizaSubLocality() {
        val resultado = ReverseGeocodingHelper.formatearDireccion(
            thoroughfare = "Jr. Lampa",
            subThoroughfare = "560",
            featureName = null,
            subLocality = "Cercado de Lima",
            locality = "Lima",
            subAdminArea = "Provincia de Lima",
            adminArea = "Lima",
            countryName = "Perú",
            latitud = -12.0480,
            longitud = -77.0310
        )

        assertEquals("Cercado de Lima", resultado.distrito)
    }

    @Test
    fun formatearDireccion_cadenaFallbackDistrito_siSubLocalityEsVacio_usaLocality() {
        val resultado = ReverseGeocodingHelper.formatearDireccion(
            thoroughfare = "Carretera Central",
            subThoroughfare = "Km 15",
            featureName = null,
            subLocality = "",
            locality = "Ate",
            subAdminArea = "Lima",
            adminArea = "Lima",
            countryName = "Perú",
            latitud = -12.0250,
            longitud = -76.9150
        )

        assertEquals("Ate", resultado.distrito)
    }

    @Test
    fun formatearDireccion_cadenaFallbackDistrito_siSubLocalityYLocalitySonVacios_usaSubAdminArea() {
        val resultado = ReverseGeocodingHelper.formatearDireccion(
            thoroughfare = "Calle Rural S/N",
            subThoroughfare = null,
            featureName = null,
            subLocality = null,
            locality = null,
            subAdminArea = "Canta",
            adminArea = "Lima",
            countryName = "Perú",
            latitud = -11.4667,
            longitud = -76.6230
        )

        assertEquals("Canta", resultado.distrito)
    }

    @Test
    fun limpiarDuplicados_eliminaTokensRepetidos() {
        val textoConDuplicados = "Lima, Lima, Cercado de Lima, Lima"
        val textoLimpio = ReverseGeocodingHelper.limpiarDuplicados(textoConDuplicados)
        assertEquals("Lima, Cercado de Lima", textoLimpio)
    }

    @Test
    fun resultadoFallback_preservaCoordenadasExactas_yMarcaNoEncontrada() {
        val lat = -12.0463
        val lon = -77.0305
        val fallback = ReverseGeocodingHelper.resultadoFallback(lat, lon, "Sin conexión de red")

        assertFalse(fallback.fueEncontrada)
        assertTrue(fallback.direccionCompleta.contains("-12.04630"))
        assertTrue(fallback.direccionCompleta.contains("-77.03050"))
        assertEquals("Sin conexión de red", fallback.mensajeFallback)
        assertEquals("", fallback.distrito)
    }

    // =========================================================================
    // 2. TAXONOMÍA DE ERRORES DE RED Y CONEXIÓN
    // =========================================================================

    @Test
    fun clasificarError_unknownHostException_clasificaComoSinInternet() {
        val ex = UnknownHostException("Unable to resolve host \"adoptme2.onrender.com\": No address associated with hostname")
        val cat = RetrofitClient.clasificarError(ex)
        val mensaje = RetrofitClient.resolverMensajeErrorServidor(ex)

        assertEquals(CategoriaErrorConexion.SIN_INTERNET, cat)
        assertTrue(mensaje.contains("No hay conexión a Internet"))
    }

    @Test
    fun clasificarError_socketTimeoutException_clasificaComoTimeoutOArranque() {
        val ex = SocketTimeoutException("connect timed out")
        val cat = RetrofitClient.clasificarError(ex)
        val mensaje = RetrofitClient.resolverMensajeErrorServidor(ex)

        assertEquals(CategoriaErrorConexion.TIMEOUT_O_COLD_START, cat)
        assertTrue(mensaje.contains("El servidor está iniciando"))
    }

    @Test
    fun clasificarError_timeoutException_clasificaComoTimeoutOArranque() {
        val ex = TimeoutException("Operation timed out")
        val cat = RetrofitClient.clasificarError(ex)
        val mensaje = RetrofitClient.resolverMensajeErrorServidor(ex)

        assertEquals(CategoriaErrorConexion.TIMEOUT_O_COLD_START, cat)
        assertTrue(mensaje.contains("El servidor está iniciando"))
    }

    @Test
    fun clasificarError_connectException_clasificaComoTimeoutOArranque() {
        val ex = ConnectException("Failed to connect to adoptme2.onrender.com")
        val cat = RetrofitClient.clasificarError(ex)
        val mensaje = RetrofitClient.resolverMensajeErrorServidor(ex)

        assertEquals(CategoriaErrorConexion.TIMEOUT_O_COLD_START, cat)
        assertTrue(mensaje.contains("El servidor está iniciando"))
    }

    @Test
    fun clasificarError_http401_clasificaComoAutorizacion() {
        val response = Response.error<Any>(401, "{\"error\":\"Credenciales invalidas\"}".toResponseBody("application/json".toMediaTypeOrNull()))
        val ex = HttpException(response)
        val cat = RetrofitClient.clasificarError(ex)
        val mensaje = RetrofitClient.resolverMensajeErrorServidor(ex)

        assertEquals(CategoriaErrorConexion.AUTORIZACION, cat)
        assertTrue(mensaje.contains("correo o la contraseña no son correctos") || mensaje.contains("sesión ha expirado"))
    }

    @Test
    fun clasificarError_http403_clasificaComoProhibido() {
        val response = Response.error<Any>(403, "{\"error\":\"Acceso denegado: centro inactivo\"}".toResponseBody("application/json".toMediaTypeOrNull()))
        val ex = HttpException(response)
        val cat = RetrofitClient.clasificarError(ex)
        val mensaje = RetrofitClient.resolverMensajeErrorServidor(ex)

        assertEquals(CategoriaErrorConexion.PROHIBIDO, cat)
        assertTrue(mensaje.contains("Acceso denegado") || mensaje.contains("No tienes autorización"))
    }

    @Test
    fun clasificarError_http500_clasificaComoErrorServidor() {
        val response = Response.error<Any>(500, "{\"error\":\"Error interno de servidor\"}".toResponseBody("application/json".toMediaTypeOrNull()))
        val ex = HttpException(response)
        val cat = RetrofitClient.clasificarError(ex)
        val mensaje = RetrofitClient.resolverMensajeErrorServidor(ex)

        assertEquals(CategoriaErrorConexion.ERROR_SERVIDOR, cat)
        assertTrue(mensaje.contains("problema temporal en el servidor"))
    }

    @Test
    fun clasificarError_http502ColdStart_clasificaComoTimeoutOArranque() {
        val response = Response.error<Any>(502, "Bad Gateway".toResponseBody("text/plain".toMediaTypeOrNull()))
        val ex = HttpException(response)
        val cat = RetrofitClient.clasificarError(ex)
        val mensaje = RetrofitClient.resolverMensajeErrorServidor(ex)

        assertEquals(CategoriaErrorConexion.TIMEOUT_O_COLD_START, cat)
        assertTrue(mensaje.contains("El servidor está iniciando"))
    }

    @Test
    fun clasificarError_otroErrorDesconocido_clasificaComoDesconocido() {
        val ex = IllegalStateException("Estado no esperado en flujo local")
        val cat = RetrofitClient.clasificarError(ex)
        val mensaje = RetrofitClient.resolverMensajeErrorServidor(ex)

        assertEquals(CategoriaErrorConexion.DESCONOCIDO, cat)
        assertTrue(mensaje.contains("No se pudo conectar con el servidor"))
    }
}
