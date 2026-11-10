package com.example.adoptme

import com.example.adoptme.api.RetrofitClient
import com.example.adoptme.model.EdadMascota
import com.example.adoptme.model.MascotaDto
import com.example.adoptme.model.formatearEdadMascota
import com.example.adoptme.model.interpretarEdadMascota
import com.example.adoptme.model.validarEdadMascota
import com.example.adoptme.screens.esFormatoEdadValido
import com.example.adoptme.utils.formatearEdadCanonica
import com.google.gson.Gson
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import okio.Buffer
import org.junit.Assert.*
import org.junit.Test
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.util.concurrent.TimeoutException

/**
 * Pruebas unitarias para:
 * 1. Conexión predeterminada a Render y seguridad contra localhost en producción.
 * 2. Modelo de EdadMascota, formateo canónico compuesto y validación de rangos.
 * 3. Tolerancia e interpretación de formatos históricos y compuestos.
 * 4. Integridad de payloads JSON y multipart enviados al backend.
 */
class EdadMascotaYConexionTest {

    private val gson = Gson()

    @Test
    fun conexion_porDefectoApuntaARenderYDeshabilitaDevUrl() {
        assertEquals("https://adoptme2.onrender.com/", BuildConfig.BASE_URL)
        assertEquals("https://adoptme2.onrender.com/", RetrofitClient.baseUrl)
        assertFalse("PERMITIR_URL_DEV debe ser false por defecto para Android Studio Run", BuildConfig.PERMITIR_URL_DEV)
        assertEquals("", BuildConfig.URL_LOCAL_DEV)
    }

    @Test
    fun conexion_configurarBaseUrlRechazaCambioCuandoPermitirDevEsFalso() {
        // En compilación estándar (PERMITIR_URL_DEV = false), intentar conmutar a localhost debe ser un no-op
        RetrofitClient.configurarBaseUrl("http://127.0.0.1:5217/")
        assertEquals("https://adoptme2.onrender.com/", RetrofitClient.baseUrl)

        // Restaurar producción explícitamente debe mantener Render
        RetrofitClient.restaurarBaseUrlProduccion()
        assertEquals("https://adoptme2.onrender.com/", RetrofitClient.baseUrl)
    }

    @Test
    fun conexion_resolverMensajeErrorServidor_manejaColdStartYTimeouts() {
        val errTimeout = SocketTimeoutException("Read timed out")
        assertEquals(
            "El servidor está iniciando. Intenta nuevamente en unos segundos.",
            RetrofitClient.resolverMensajeErrorServidor(errTimeout)
        )

        val errConnect = ConnectException("Failed to connect to adoptme2.onrender.com")
        assertEquals(
            "El servidor está iniciando. Intenta nuevamente en unos segundos.",
            RetrofitClient.resolverMensajeErrorServidor(errConnect)
        )

        val errConcurrentTimeout = TimeoutException("Timed out waiting for response")
        assertEquals(
            "El servidor está iniciando. Intenta nuevamente en unos segundos.",
            RetrofitClient.resolverMensajeErrorServidor(errConcurrentTimeout)
        )

        val errGenerico = IOException("Network route unreachable")
        assertEquals(
            "No se pudo conectar con el servidor. Revisa tu conexión a Internet e inténtalo nuevamente.",
            RetrofitClient.resolverMensajeErrorServidor(errGenerico)
        )
    }

    @Test
    fun edad_formatearEdadMascota_casosRequeridos() {
        // Años solos
        assertEquals("1 año", formatearEdadMascota(EdadMascota(anios = 1, meses = 0)))
        assertEquals("2 años", formatearEdadMascota(EdadMascota(anios = 2, meses = 0)))
        assertEquals("5 años", formatearEdadMascota(EdadMascota(anios = 5, meses = 0)))

        // Meses solos
        assertEquals("1 mes", formatearEdadMascota(EdadMascota(anios = 0, meses = 1)))
        assertEquals("6 meses", formatearEdadMascota(EdadMascota(anios = 0, meses = 6)))
        assertEquals("11 meses", formatearEdadMascota(EdadMascota(anios = 0, meses = 11)))

        // Combinaciones compuestas
        assertEquals("1 año y 1 mes", formatearEdadMascota(EdadMascota(anios = 1, meses = 1)))
        assertEquals("1 año y 6 meses", formatearEdadMascota(EdadMascota(anios = 1, meses = 6)))
        assertEquals("2 años y 1 mes", formatearEdadMascota(EdadMascota(anios = 2, meses = 1)))
        assertEquals("2 años y 6 meses", formatearEdadMascota(EdadMascota(anios = 2, meses = 6)))
        assertEquals("10 años y 11 meses", formatearEdadMascota(EdadMascota(anios = 10, meses = 11)))

        // Cachorro recién nacido ("Menos de 1 mes")
        assertEquals("Menos de 1 mes", formatearEdadMascota(EdadMascota(anios = 0, meses = 0, menosDeUnMes = true)))
        assertEquals("Menos de 1 mes", formatearEdadMascota(EdadMascota(anios = 0, meses = 0, menosDeUnMes = false)))
    }

    @Test
    fun edad_validarEdadMascota_rechazaValoresInvalidos() {
        // Cero años y cero meses sin marcar recién nacido -> inválido
        val errCero = validarEdadMascota(EdadMascota(anios = 0, meses = 0, menosDeUnMes = false))
        assertNotNull(errCero)
        assertTrue(errCero!!.contains("Menos de 1 mes"))

        // Recién nacido -> válido
        assertNull(validarEdadMascota(EdadMascota(anios = 0, meses = 0, menosDeUnMes = true)))

        // Años > 40 -> inválido
        val errAniosExceso = validarEdadMascota(EdadMascota(anios = 41, meses = 0))
        assertNotNull(errAniosExceso)
        assertTrue(errAniosExceso!!.contains("40"))

        // Años negativos -> inválido
        val errAniosNegativos = validarEdadMascota(EdadMascota(anios = -1, meses = 0))
        assertNotNull(errAniosNegativos)
        assertTrue(errAniosNegativos!!.contains("negativa"))

        // Meses >= 12 -> inválido
        val errMesesExceso = validarEdadMascota(EdadMascota(anios = 1, meses = 12))
        assertNotNull(errMesesExceso)
        assertTrue(errMesesExceso!!.contains("11"))

        // Meses negativos -> inválido
        val errMesesNegativos = validarEdadMascota(EdadMascota(anios = 1, meses = -1))
        assertNotNull(errMesesNegativos)
        assertTrue(errMesesNegativos!!.contains("negativos"))

        // Casos borde válidos
        assertNull(validarEdadMascota(EdadMascota(anios = 40, meses = 11)))
        assertNull(validarEdadMascota(EdadMascota(anios = 0, meses = 1)))
        assertNull(validarEdadMascota(EdadMascota(anios = 1, meses = 0)))
    }

    @Test
    fun edad_interpretarEdadMascota_formatosCompuestosEHistoricos() {
        // Compuestos canónicos
        assertEquals(EdadMascota(2, 6), interpretarEdadMascota("2 años y 6 meses"))
        assertEquals(EdadMascota(1, 1), interpretarEdadMascota("1 año y 1 mes"))

        // Compuestos con variantes ortográficas
        assertEquals(EdadMascota(2, 6), interpretarEdadMascota("2 anos y 6 meses"))
        assertEquals(EdadMascota(3, 4), interpretarEdadMascota("3 anios y 4 meses"))

        // Históricos simples de años
        assertEquals(EdadMascota(2, 0), interpretarEdadMascota("2 años"))
        assertEquals(EdadMascota(1, 0), interpretarEdadMascota("1 año"))
        assertEquals(EdadMascota(1, 0), interpretarEdadMascota("1 ano"))
        assertEquals(EdadMascota(2, 0), interpretarEdadMascota("2 anios"))

        // Históricos simples de meses
        assertEquals(EdadMascota(0, 6), interpretarEdadMascota("6 meses"))
        assertEquals(EdadMascota(0, 1), interpretarEdadMascota("1 mes"))

        // Opción recién nacido
        assertEquals(EdadMascota(0, 0, menosDeUnMes = true), interpretarEdadMascota("Menos de 1 mes"))
        assertEquals(EdadMascota(0, 0, menosDeUnMes = true), interpretarEdadMascota("menos de un mes"))
        assertEquals(EdadMascota(0, 0, menosDeUnMes = true), interpretarEdadMascota("recien nacido"))

        // Desconocidos / inválidos devuelven null
        assertNull(interpretarEdadMascota("2 años y medio"))
        assertNull(interpretarEdadMascota("perro"))
        assertNull(interpretarEdadMascota(""))
        assertNull(interpretarEdadMascota("   "))
        assertNull(interpretarEdadMascota(null))
    }

    @Test
    fun edad_formatearEdadCanonica_normalizacionCorrecta() {
        assertEquals("2 años y 6 meses", formatearEdadCanonica("2 anos y 6 meses"))
        assertEquals("1 año y 1 mes", formatearEdadCanonica("1 anio y 1 mes"))
        assertEquals("2 años", formatearEdadCanonica("2 anos"))
        assertEquals("1 año", formatearEdadCanonica("1 ano"))
        assertEquals("6 meses", formatearEdadCanonica("6 meses"))
        assertEquals("Menos de 1 mes", formatearEdadCanonica("menos de 1 mes"))

        // Cadenas no reconocidas se preservan sin excepción
        assertEquals("desconocido", formatearEdadCanonica("desconocido"))
    }

    @Test
    fun edad_esFormatoEdadValido_coherencia() {
        assertTrue(esFormatoEdadValido("2 años y 6 meses"))
        assertTrue(esFormatoEdadValido("1 año y 1 mes"))
        assertTrue(esFormatoEdadValido("Menos de 1 mes"))
        assertTrue(esFormatoEdadValido("2 años"))
        assertTrue(esFormatoEdadValido("6 meses"))
        assertTrue(esFormatoEdadValido("1 ano"))
        assertTrue(esFormatoEdadValido("2 anos"))

        assertFalse(esFormatoEdadValido("0 años"))
        assertFalse(esFormatoEdadValido("45 años"))
        assertFalse(esFormatoEdadValido("1 año y 15 meses"))
        assertFalse(esFormatoEdadValido("2 años y medio"))
        assertFalse(esFormatoEdadValido(""))
        assertFalse(esFormatoEdadValido("gato"))
    }

    @Test
    fun payload_jsonYMultipart_construyeCampoEdadCompuesto() {
        val edadCompuesta = formatearEdadCanonica("2 años y 6 meses")
        assertEquals("2 años y 6 meses", edadCompuesta)

        // Verificación de serialización JSON en MascotaDto
        val dto = MascotaDto(
            id = 0,
            name = "Luna",
            species = "Perro",
            breed = "Labrador",
            age = edadCompuesta,
            gender = "Hembra",
            location = "Miraflores, Lima",
            description = "Perrita amigable"
        )
        val json = gson.toJson(dto)
        assertTrue(json.contains("\"age\":\"2 años y 6 meses\""))

        // Verificación de RequestBody para multipart
        val reqAge = edadCompuesta.toRequestBody("text/plain".toMediaTypeOrNull())
        val buffer = Buffer()
        reqAge.writeTo(buffer)
        val contenidoMultipart = buffer.readUtf8()
        assertEquals("2 años y 6 meses", contenidoMultipart)
    }
}
