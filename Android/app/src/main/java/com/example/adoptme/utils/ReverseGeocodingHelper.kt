package com.example.adoptme.utils

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.Locale
import kotlin.coroutines.resume

/**
 * Resultado estructurado de geocodificación inversa para AdoptMe.
 * Permite separar limpiamente la dirección física del distrito y mantener las coordenadas técnicas.
 */
data class ResultadoGeocodificacion(
    val direccionCompleta: String,
    val distrito: String,
    val ciudad: String = "",
    val departamento: String = "",
    val pais: String = "Perú",
    val mensajeFallback: String? = null,
    val fueEncontrada: Boolean = true
)

object ReverseGeocodingHelper {

    /**
     * Resuelve de forma asíncrona y segura la dirección y distrito a partir de coordenadas GPS.
     * Funciona tanto en APIs modernas (Android 33+ con GeocodeListener) como anteriores.
     */
    suspend fun obtenerDireccion(
        context: Context,
        latitud: Double,
        longitud: Double
    ): ResultadoGeocodificacion = withContext(Dispatchers.IO) {
        if (!Geocoder.isPresent()) {
            return@withContext resultadoFallback(
                latitud,
                longitud,
                "Servicio de geocodificación no disponible en este dispositivo."
            )
        }

        try {
            val geocoder = Geocoder(context, Locale.Builder().setLanguage("es").setRegion("PE").build())
            val addresses: List<Address>? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                suspendCancellableCoroutine { cont ->
                    try {
                        geocoder.getFromLocation(latitud, longitud, 1, object : Geocoder.GeocodeListener {
                            override fun onGeocode(results: MutableList<Address>) {
                                if (cont.isActive) cont.resume(results)
                            }
                            override fun onError(errorMessage: String?) {
                                if (cont.isActive) cont.resume(null)
                            }
                        })
                    } catch (_: Exception) {
                        if (cont.isActive) cont.resume(null)
                    }
                }
            } else {
                @Suppress("DEPRECATION")
                geocoder.getFromLocation(latitud, longitud, 1)
            }

            if (addresses.isNullOrEmpty()) {
                return@withContext resultadoFallback(
                    latitud,
                    longitud,
                    "No se encontró una dirección específica para este punto."
                )
            }

            val address = addresses[0]
            procesarAddress(address, latitud, longitud)
        } catch (_: IOException) {
            resultadoFallback(
                latitud,
                longitud,
                "Sin conexión a Internet para consultar la dirección física."
            )
        } catch (_: Exception) {
            resultadoFallback(
                latitud,
                longitud,
                "No se pudo resolver la dirección automáticamente."
            )
        }
    }

    /**
     * Procesa un objeto Address de Android extrayendo sus componentes.
     */
    fun procesarAddress(address: Address, latitud: Double, longitud: Double): ResultadoGeocodificacion {
        return formatearDireccion(
            thoroughfare = address.thoroughfare,
            subThoroughfare = address.subThoroughfare,
            featureName = address.featureName,
            subLocality = address.subLocality,
            locality = address.locality,
            subAdminArea = address.subAdminArea,
            adminArea = address.adminArea,
            countryName = address.countryName,
            latitud = latitud,
            longitud = longitud
        )
    }

    /**
     * Algoritmo puro de formateo de dirección y distrito para Perú.
     * Diseñado para pruebas unitarias sin dependencias del framework Android.
     */
    fun formatearDireccion(
        thoroughfare: String?,
        subThoroughfare: String?,
        featureName: String?,
        subLocality: String?,
        locality: String?,
        subAdminArea: String?,
        adminArea: String?,
        countryName: String?,
        latitud: Double,
        longitud: Double
    ): ResultadoGeocodificacion {
        // 1. Cadena de fallback para Distrito: subLocality -> locality -> subAdminArea -> adminArea
        val distrito = listOfNotNull(subLocality, locality, subAdminArea)
            .map { it.trim() }
            .firstOrNull { it.isNotBlank() } ?: ""

        val ciudad = listOfNotNull(locality, subAdminArea, adminArea)
            .map { it.trim() }
            .firstOrNull { it.isNotBlank() } ?: ""

        val departamento = listOfNotNull(adminArea, subAdminArea)
            .map { it.trim() }
            .firstOrNull { it.isNotBlank() } ?: ""

        val pais = countryName?.trim().takeUnless { it.isNullOrBlank() } ?: "Perú"

        // 2. Construcción de dirección física (vía / numeración / referencia)
        val partesCalle = mutableListOf<String>()
        val calle = thoroughfare?.trim().orEmpty()
        val numero = subThoroughfare?.trim().orEmpty()
        val nombreCaracteristico = featureName?.trim().orEmpty()

        if (calle.isNotBlank()) {
            partesCalle.add(calle)
            if (numero.isNotBlank() && !calle.contains(numero, ignoreCase = true)) {
                partesCalle.add(numero)
            }
        } else if (nombreCaracteristico.isNotBlank() && !esCoordenada(nombreCaracteristico)) {
            partesCalle.add(nombreCaracteristico)
        }

        val direccionBase = if (partesCalle.isNotEmpty()) {
            partesCalle.joinToString(" ")
        } else if (distrito.isNotBlank()) {
            distrito
        } else {
            String.format(Locale.US, "Ubicación (%.4f, %.4f)", latitud, longitud)
        }

        // 3. Limpieza de tokens duplicados (ej: "Lima, Lima")
        val direccionLimpia = limpiarDuplicados(direccionBase)

        return ResultadoGeocodificacion(
            direccionCompleta = direccionLimpia,
            distrito = distrito,
            ciudad = ciudad,
            departamento = departamento,
            pais = pais,
            mensajeFallback = null,
            fueEncontrada = true
        )
    }

    /**
     * Detecta si una cadena representa solo coordenadas numéricas.
     */
    fun esCoordenada(texto: String): Boolean {
        val trimmed = texto.trim()
        return trimmed.matches(Regex("^-?\\d+(\\.\\d+)?,?\\s*-?\\d+(\\.\\d+)?$"))
    }

    /**
     * Limpia duplicados secuenciales o recurrentes separados por comas.
     */
    fun limpiarDuplicados(texto: String): String {
        val tokens = texto.split(",").map { it.trim() }.filter { it.isNotBlank() }
        if (tokens.isEmpty()) return texto.trim()

        val vistos = mutableSetOf<String>()
        val unicos = mutableListOf<String>()
        for (token in tokens) {
            val normalizado = token.lowercase(Locale.ROOT)
            if (!vistos.contains(normalizado)) {
                vistos.add(normalizado)
                unicos.add(token)
            }
        }
        return if (unicos.isNotEmpty()) unicos.joinToString(", ") else texto.trim()
    }

    /**
     * Genera un resultado de fallback cuando no se puede geocodificar.
     */
    fun resultadoFallback(latitud: Double, longitud: Double, mensaje: String): ResultadoGeocodificacion {
        return ResultadoGeocodificacion(
            direccionCompleta = String.format(Locale.US, "Coordenadas: %.5f, %.5f", latitud, longitud),
            distrito = "",
            ciudad = "",
            departamento = "",
            pais = "Perú",
            mensajeFallback = mensaje,
            fueEncontrada = false
        )
    }
}
