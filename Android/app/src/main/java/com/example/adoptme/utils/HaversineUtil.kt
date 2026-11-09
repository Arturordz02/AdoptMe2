package com.example.adoptme.utils

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Utilidad matemática para cálculo de distancia geográfica mediante la fórmula de Haversine.
 * Entrega #4: Filtros de cercanía y geolocalización.
 */
object HaversineUtil {

    private const val RADIO_TIERRA_KM = 6371.0

    /**
     * Valida si un par de coordenadas (latitud, longitud) está en el rango geográfico válido.
     */
    fun sonCoordenadasValidas(latitud: Double?, longitud: Double?): Boolean {
        if (latitud == null || longitud == null) return false
        if (latitud.isNaN() || longitud.isNaN()) return false
        if (latitud < -90.0 || latitud > 90.0) return false
        if (longitud < -180.0 || longitud > 180.0) return false
        return true
    }

    /**
     * Calcula la distancia en kilómetros entre dos puntos sobre la superficie terrestre
     * utilizando la fórmula de Haversine.
     * Retorna null si alguna de las coordenadas es inválida o nula.
     */
    fun calcularDistanciaKm(
        lat1: Double?,
        lon1: Double?,
        lat2: Double?,
        lon2: Double?
    ): Double? {
        if (!sonCoordenadasValidas(lat1, lon1) || !sonCoordenadasValidas(lat2, lon2)) {
            return null
        }

        val phi1 = Math.toRadians(lat1!!)
        val phi2 = Math.toRadians(lat2!!)
        val deltaPhi = Math.toRadians(lat2 - lat1)
        val deltaLambda = Math.toRadians(lon2!! - lon1!!)

        val a = sin(deltaPhi / 2.0).pow(2) +
                cos(phi1) * cos(phi2) * sin(deltaLambda / 2.0).pow(2)

        val c = 2.0 * atan2(sqrt(a), sqrt(1.0 - a))

        return RADIO_TIERRA_KM * c
    }

    /**
     * Formatea la distancia calculada para presentación amigable al usuario en la interfaz.
     */
    fun formatearDistancia(distanciaKm: Double?): String {
        if (distanciaKm == null || distanciaKm.isNaN() || distanciaKm < 0.0) {
            return "Distancia no disponible"
        }
        return if (distanciaKm < 1.0) {
            val metros = (distanciaKm * 1000).toInt()
            "a ${metros} m"
        } else {
            "a %.1f km".format(java.util.Locale.US, distanciaKm)
        }
    }
}
